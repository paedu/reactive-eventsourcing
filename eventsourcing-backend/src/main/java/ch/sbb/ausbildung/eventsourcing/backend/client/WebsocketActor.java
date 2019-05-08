
package ch.sbb.ausbildung.eventsourcing.backend.client;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.journal.leveldb.javadsl.LeveldbReadJournal;
import akka.stream.Materializer;
import akka.stream.QueueOfferResult;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.SourceQueueWithComplete;
import ch.sbb.ausbildung.eventsourcing.backend.client.FSA.UserCommands;
import ch.sbb.ausbildung.eventsourcing.backend.client.FSA.UserFSA;
import ch.sbb.ausbildung.eventsourcing.backend.client.FSA.VerkehrsmittelFSA;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor.Command;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor.Event;
import lombok.Value;

import java.util.concurrent.CompletionStage;

/**
 * Websocket-Actor handling the bidirectional websocket communication between backend and frontend.
 * Each browser session (client) will trigger the creation of a new instance of this actor.
 *
 * To be able to stream the backend events to the frontend, this actor holds a ref to the event journal (leveldb)
 * aka. read journal where persistent queries can be triggered against; the result will be a streaming source of events
 * (of type EventEnvelope). These events are then offered to the actor's input queue as they arrive from stream.
 * The websocket actor takes them off the queue and pushes them to the client side (converted to the appropriate action type).
 *
 * User commands from the client on the other side are handled in the {@link #receive(TextMessage)} block:
 * depending on their type the corresponding action will be triggered (e.g. forwarding to the verkehrsmittel actor or the like)
 *
 * @param <T> type of FSA to handle in this websocket actor
 *
 * @see <a href="https://doc.akka.io/docs/akka/current/stream/stream-integrations.html#source-queue">SourceQueueWithComplete
 * (streaming source which emits elements from a queue</a>
 */
public class WebsocketActor<T extends FSA> extends AbstractLoggingActor {

    private static final String USERNAME = "USERNAME";
    private static final String USERNAME_UNDEF = "<undefined>";

    private final Class<T> fsaClass;
    private final Materializer materializer;
    private final ActorRef vmActor;
    private LeveldbReadJournal readJournal;
    private SourceQueueWithComplete<Message> toClientQueue;

    static Props props(ActorRef vmActor, Materializer materializer) {
        return Props.create(WebsocketActor.class, () -> new WebsocketActor(vmActor, materializer));
    }

    private WebsocketActor(ActorRef vmActor, Materializer materializer) {
        this.vmActor = vmActor;
        this.fsaClass = (Class<T>) VerkehrsmittelFSA.class;
        this.materializer = materializer;
    }

    @Override
    public void preStart() {
        // get the read journal used for persistence queries on the event store
        readJournal = PersistenceQuery.get(context().system())
                .getReadJournalFor(LeveldbReadJournal.class, LeveldbReadJournal.Identifier());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                // handling of messages coming from the client (frontend)
                .match(TextMessage.class, this::receive)
                // register the "queue" (SourceQueueWithComplete) used for events from backend
                .match(RegisterOutgoingQueue.class, this::init)
                .match(ConnectionError.class, streamTerminated -> context().stop(self()))
                .build();
    }

    private void init(final RegisterOutgoingQueue command) {
        this.toClientQueue = command.getQueue();
    }

    // reply with a serialized FSA sending to client (frontend)
    private CompletionStage<Boolean> reply(final FSA action) {
        try {
            log().info("Reply to client: {}", action.getType());
            return toClientQueue
                    .offer(TextMessage.create(FSA.writeValueAsString(action)))
                    .thenApply(queueOfferResult -> queueOfferResult == QueueOfferResult.enqueued());
        } catch (final Exception e) {
            throw new WebsocketException("cannot send to client", e);
        }
    }

    // action handler for messages coming from client (frontend)
    private void actionHandler(final VerkehrsmittelFSA action) {
        log().debug("dispatching: {}", action);

        switch (action.getType()) {
            case UserCommands.LOAD_USERNAME:
                pushUsername();
                break;
            case UserCommands.LOAD_VERKEHRSMITTEL:
                pushEvents();
                break;
            case UserCommands.DELAY_VERKEHRSMITTEL:
                delayVerkehrsmittel(action);
                break;
            default:
                reply(FSA.error("server_error", "can not find dispatcher for action: " + action, "type unknown"));
                break;
        }
    }

    // handler for client messages (frontend)
    private void receive(final TextMessage textMessage) {
        try {
            log().debug("receive: {}", textMessage.getStrictText());
            final T request = FSA.readValueFor(textMessage.getStrictText(), fsaClass);
            try {
                actionHandler((VerkehrsmittelFSA) request);
            } catch (final Exception e) {
                log().error(e, "error while running request: " + request);
                final String details = "execution failed. exception=" +
                        e.getClass().getSimpleName();
                reply(FSA.error("server_error", details, "execution error"));
            }
        } catch (final Exception e) {
            log().error(e, "cannot parse request");
            final String details = "cannot parse request: " +
                    textMessage.getStrictText() + ", exception=" +
                    e.getClass().getSimpleName();
            reply(FSA.error("server_error", details, "parsing error"));
        }
    }

    // we start a persistence query i.e. a source streaming all "vm"-events from backend to frontend
    private void pushEvents() {
        readJournal.eventsByPersistenceId("vm", 0L, Long.MAX_VALUE)
                .map(EventEnvelope::event)
                .map(event -> {
                    if (event instanceof Event.VerkehrsmittelCreated) {
                        Event.VerkehrsmittelCreated created = (Event.VerkehrsmittelCreated) event;
                        return VerkehrsmittelFSA.action(FSA.Events.VM_CREATED, created.getVerkehrsmittel(), created.getVmNummer());
                    } else if (event instanceof Event.VerkehrsmittelMoved) {
                        Event.VerkehrsmittelMoved moved = (Event.VerkehrsmittelMoved) event;
                        return VerkehrsmittelFSA.action(FSA.Events.VM_MOVED, moved.getAktuellePosition(), moved.getVmNummer());
                    } else if (event instanceof Event.VerkehrsmittelDelayed) {
                        Event.VerkehrsmittelDelayed delayed = (Event.VerkehrsmittelDelayed) event;
                        return VerkehrsmittelFSA.action(FSA.Events.VM_DELAYED, delayed.getDelay(), delayed.getVmNummer());
                    } else {
                        return VerkehrsmittelFSA.error("server_error", "unhandled event type: " + event);
                    }
                })
                .map(this::reply)
                .runWith(Sink.ignore(), materializer);
    }

    // pushed the user name (user logged in) to the frontend
    private void pushUsername() {
        this.reply(UserFSA.action(FSA.UserEvents.USERNAME_LOADED, System.getenv().getOrDefault(USERNAME, USERNAME_UNDEF)));
    }

    // command from the client (frontend) which will be forwarded to the "verkehrsmittel"-actor
    private void delayVerkehrsmittel(VerkehrsmittelFSA action) {
        this.vmActor.tell(new Command.DelayVerkehrsmittel((int) action.getMeta(), Integer.parseInt((String) action.getPayload())), self());
    }


    // command sent from client websocket when connection got lost
    @Value
    static class ConnectionError {
    }

    // initialize the outbound queue (websocket-actor -> client)
    @Value
    static class RegisterOutgoingQueue {
        SourceQueueWithComplete<Message> queue;
    }

    static class WebsocketException extends RuntimeException {
        WebsocketException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
