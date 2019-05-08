
package ch.sbb.ausbildung.eventsourcing.backend.client;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.server.Route;
import akka.pattern.BackoffOpts;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.CoupledTerminationFlow;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.extractActorSystem;
import static akka.http.javadsl.server.Directives.extractMaterializer;
import static akka.http.javadsl.server.Directives.get;
import static akka.http.javadsl.server.Directives.handleWebSocketMessages;
import static akka.http.javadsl.server.Directives.path;
import static akka.http.javadsl.server.Directives.pathSingleSlash;
import static akka.http.javadsl.server.Directives.route;

/**
 * Http routes used to bind incoming http or websocket requests to an appropriate handler.
 * Main part here is the <code>websocketRoute</code> which accepts incoming websocket requests (ws://..)
 * from client and creates a corresponding websocket-actor (handler) for it, i.e. each client has its own
 * websocket actor handling the bidirectional communication between backend and frontend.
 *
 * @see <a href="https://doc.akka.io/docs/akka-http/current/routing-dsl/index.html#routing-dsl">Akka Http (Routing DSL)</a>
 * @see <a href="https://doc.akka.io/docs/akka-http/current/server-side/websocket-support.html#server-websocket-support">Akka Websocket Support</a>
 */
public class HttpRoute {

    private static final String WEBSOCKET_PATH_SEGENT = "websocket";
    private static final Logger logger = LoggerFactory.getLogger(HttpRoute.class);


    private Route createWebsocketRoute(ActorRef vmActor) {
        return route(
                indexPage("",
                        WEBSOCKET_PATH_SEGENT
                ),
                path(WEBSOCKET_PATH_SEGENT, () ->
                        get(() ->
                                extractActorSystem(actorSystem ->
                                        extractMaterializer(materializer ->
                                                handleWebSocketMessages(websocketFlow(actorSystem, vmActor, materializer)))
                                )
                        )
                )
        );
    }

    /**
     * Creates a flow which streams messages from the frontend directly to the {@link WebsocketActor} and forwards
     * replies from the backend to the input queue of the websocket actor (<tt>Source.queue(..)</tt> )
     * <p>
     * <pre>
     *                  +-----------------------+
     *                  | Websocket flow        |
     *                  |                       |
     *                  |  +-----------------+  |
     *  WS from client ~~> | ActorRef [Sink] |  |  ~~> Message to WebsocketActor
     *                  |  +-----------------+  |
     *                  |                       |
     *                  |  +-----------------+  |
     *  WS to client   <~~ | Queue [Source]  |  |  <~~ Add to Queue (uses a SourceQueueWithComplete, from WebsocketActor)
     *                  |  +-----------------+  |
     *                  +-----------------------+
     *
     * </pre>
     *
     * @param actorSystem  reference to the Akka Actor System
     * @param materializer the materializer to use.
     */
    private static Flow<Message, Message, Tuple2<NotUsed, NotUsed>> websocketFlow(ActorSystem actorSystem, ActorRef vmActor, Materializer materializer) {

        // Erstelle Backoff Supervisor, damit neuer Actor einen Parent hat auf dem eine Supervision-Strategie definiert werden kann.
        // Der Aktor soll nie neu starten - weil das Websocket dann nicht mehr verbunden wäre.
        final Props supervisorProps = BackoffOpts.onFailure(
                WebsocketActor.props(vmActor, materializer),
                "websocketActor",
                Duration.create(3, TimeUnit.SECONDS),
                Duration.create(20, TimeUnit.SECONDS),
                0.2).withMaxNrOfRetries(100)
                .props();

        ActorRef actor = actorSystem.actorOf(supervisorProps);

        // Create ActorRef as Sink. Send ConnectionError when Stream breaks.
        Sink<Message, NotUsed> incomingSink = Sink.actorRef(actor, new WebsocketActor.ConnectionError());

        // send the materialized Queue to the actor when ready.
        Source<Message, NotUsed> outgoingSource = Source.<Message>queue(2000,
                OverflowStrategy.fail())
                .mapMaterializedValue(queue -> {
                    actor.tell(new WebsocketActor.RegisterOutgoingQueue(queue), ActorRef.noSender());
                    return NotUsed.getInstance();
                });

        logger.info("new websocket-actor created");
        return CoupledTerminationFlow.fromSinkAndSource(incomingSink, outgoingSource);
    }

    // bind HTTP port to localhost and listens for incoming data to stream (via flow)
    public CompletionStage<Void> bindHttp(ActorMaterializer materializer, ActorSystem system, final ActorRef vmActor) {
        final Http http = Http.get(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = this.createWebsocketRoute(vmActor).flow(system, materializer);
        int port = system.settings().config().getInt("eventsourcing.http.port");

        return http.bindAndHandle(routeFlow, ConnectHttp.toHost("0.0.0.0", port), materializer)
                .thenAccept(res -> logger.info("Server started at http://localhost:{}/", port))
                .whenComplete((res, thr) -> {
                    if (thr != null) {
                        logger.error("Can not bind http port -> TERMINATE AKKA!", thr);
                        CoordinatedShutdown.get(system).runAll(CoordinatedShutdown.unknownReason());
                    }
                });
    }


    // helper method to generate a simple index page for the given sub-pages
    private static Route indexPage(String intro, String... pages) {
        // Seite ist nur mit Slash am Ende erreichbar, damit die Html-Links den richtigen Pfad haben.
        // Es muss darauf mit Slash am Ende verlinkt werden.
        return pathSingleSlash(() -> get(() -> complete(
                HttpResponse.create().withEntity(ContentTypes.TEXT_HTML_UTF8,
                        ByteString.fromString(
                                "<h2>" + intro
                                        + "<ul>"
                                        + Arrays.stream(pages)
                                        .map(p -> "<li><a href='" + p + "'>" + p + "</a></li>")
                                        .collect(Collectors.joining())
                                        + "</ul>"
                        ))
                )
        ));
    }
}
