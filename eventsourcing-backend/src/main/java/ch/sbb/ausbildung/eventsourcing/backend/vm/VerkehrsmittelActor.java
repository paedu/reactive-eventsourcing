
package ch.sbb.ausbildung.eventsourcing.backend.vm;

import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor.Event.VerkehrsmittelCreated;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor.Event.VerkehrsmittelDelayed;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor.Event.VerkehrsmittelMoved;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import scala.Immutable;

import java.io.Serializable;

/**
 * Persistent actor handling all incoming commands (i.e. VerkehrsmittelCommand from inbound actor or user):
 * it validates the incoming command and if it's applicable it will create an appropriate event (fact) out of it and
 * stores it in the event store (journal). If the event has succesfully been stored it will also be applied to the current
 * <code>state</code> (mutate the state due to the fact i.e. event that happened)
 * When this actor restarts, it first recovers all of the stored events from journal in order to restore the last active state
 * (see {@link #receiveRecover()}. Messages arriving while the actor is restarting will be stashed away until
 *
 * @see <a href="https://doc.akka.io/docs/akka/current/persistence.html#persistence">Persistent Actors</a>
 * @see <a href="https://doc.akka.io/docs/akka/current/persistence.html#event-sourcing">Eventsourcing</a>
 */
public class VerkehrsmittelActor extends AbstractPersistentActor {

    private final LoggingAdapter log = context().system().log();

    private final State state;

    private VerkehrsmittelActor() {
        this.state = State.empty();
    }

    // recovering all events from event store while (re)starting actor
    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(VerkehrsmittelCreated.class, evt -> {
                    log.info("recover event: {}", evt);
                    this.eventHandler(evt);
                })
                .match(VerkehrsmittelMoved.class, evt -> {
                    log.info("recover event: {}", evt);
                    this.eventHandler(evt);
                })
                .match(VerkehrsmittelDelayed.class, evt -> {
                    log.info("recover event: {}", evt);
                    this.eventHandler(evt);
                })
                .build();
    }

    // message handling (process incoming commands)
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.CreateVerkehrsmittel.class, this::commandHandler)
                .match(Command.MoveVerkehrsmittel.class, this::commandHandler)
                .match(Command.DelayVerkehrsmittel.class, this::commandHandler)
                // debug or test command (retrieves the current state)
                .match(Command.GetState.class, this::commandHandler)
                .build();
    }

    private void commandHandler(Command.MoveVerkehrsmittel move) {
        // if command's position is not yet passed -> set it as new position
        if (state.verkehrsmittelByVmNummer(move.getVmNummer())
                // shorten list of "fahrtpunkte" so that only those stops remain which haven't been passed yet ("übrigbleibende Fahrtpunkte")
                .map(vm -> vm.getFahrtpunkte().takeRightUntil(punkt -> punkt.equals(vm.getAktuellePosition().getOrElse(""))))
                .map(restlicheFahrtpunkte -> restlicheFahrtpunkte.contains(move.getAktuellePosition())).getOrElse(false)) {

            // create event (fact) and save it into event store
            persist(VerkehrsmittelMoved.builder()
                    .vmNummer(move.getVmNummer())
                    .aktuellePosition(move.getAktuellePosition())
                    .build(), this::eventHandler);
        }
    }

    private void commandHandler(Command.CreateVerkehrsmittel command) {
        if (!state.containsVerkehrsmittel(command.vmNummer)) {

            // create event (fact) and save it into event store
            persist(VerkehrsmittelCreated.builder()
                    .vmNummer(command.vmNummer)
                    .verkehrsmittel(Verkehrsmittel.builder()
                            .vmNummer(command.vmNummer)
                            .vmArt(command.vmArt)
                            .bezeichnung(command.bezeichnung)
                            .fahrtpunkte(command.getFahrtpunkte())
                            .build())
                    .build(), this::eventHandler);
        }
    }

    private void commandHandler(Command.DelayVerkehrsmittel command) {
        if (state.containsVerkehrsmittel(command.getVmNummer())) {

            // create event (fact) and save it into event store
            persist(VerkehrsmittelDelayed.builder()
                    .vmNummer(command.getVmNummer())
                    .delay(command.getDelay() == 0 ? null : command.getDelay())
                    .build(), this::eventHandler);
        }
    }

    // debug or test command to retrieve the current state
    private void commandHandler(Command.GetState getState) {
        sender().tell(state.verkehrsmittel, self());
    }

    private void eventHandler(VerkehrsmittelCreated created) {
        state.verkehrsmittelCreated(created);
    }

    private void eventHandler(VerkehrsmittelMoved moved) {
        state.verkehrsmittelMoved(moved);
    }

    private void eventHandler(VerkehrsmittelDelayed delayed) {
        state.verkehrsmitteDelayed(delayed);
    }

    // persistence id used to identify the events in the event store (should not be changed!)
    @Override
    public String persistenceId() {
        return "vm";
    }

    public static Props props() {
        return Props.create(VerkehrsmittelActor.class, VerkehrsmittelActor::new);
    }


    // Commands
    public interface Command extends Immutable {
        @Value
        class CreateVerkehrsmittel implements Command {
            final int vmNummer;
            final String vmArt;
            final String bezeichnung;
            final List<String> fahrtpunkte;
        }

        @Value
        class MoveVerkehrsmittel implements Command {
            final int vmNummer;
            final String aktuellePosition;
        }

        @Value
        class DelayVerkehrsmittel implements Command {
            final int vmNummer;
            final int delay;
        }

        @Value(staticConstructor = "instance")
        class GetState implements Command {
        }

        @Value(staticConstructor = "instance")
        class NoOp implements Command {
        }
    }

    // Events (facts, cannot be deleted once applied)
    public interface Event extends Immutable, Serializable {
        @Value
        @Builder
        class VerkehrsmittelCreated implements Event {
            private final int vmNummer;
            @NonNull
            private final Verkehrsmittel verkehrsmittel;
        }

        @Value
        @Builder
        class VerkehrsmittelMoved implements Event {
            private final int vmNummer;
            @NonNull
            private final String aktuellePosition;
        }

        @Value
        @Builder
        class VerkehrsmittelDelayed implements Event {
            private final int vmNummer;
            private final Integer delay;
        }
    }

    // current state of VerkehrsmittelActor
    static class State {

        Map<Integer, Verkehrsmittel> verkehrsmittel = HashMap.empty();

        boolean containsVerkehrsmittel(int vmNummer) {
            return verkehrsmittel.containsKey(vmNummer);
        }

        Option<Verkehrsmittel> verkehrsmittelByVmNummer(int vmNummer) {
            return verkehrsmittel.get(vmNummer);
        }

        void verkehrsmittelCreated(VerkehrsmittelCreated created) {
            verkehrsmittel = verkehrsmittel.put(created.getVmNummer(), created.verkehrsmittel);
        }

        void verkehrsmittelMoved(VerkehrsmittelMoved moved) {
            verkehrsmittelByVmNummer(moved.getVmNummer()).map(vm -> vm.setAktuellePosition(moved.getAktuellePosition()));
        }

        void verkehrsmitteDelayed(VerkehrsmittelDelayed delayed) {
            verkehrsmittelByVmNummer(delayed.getVmNummer()).map(vm -> vm.setDelay(delayed.getDelay()));
        }

        static State empty() {
            return new State();
        }
    }
}
