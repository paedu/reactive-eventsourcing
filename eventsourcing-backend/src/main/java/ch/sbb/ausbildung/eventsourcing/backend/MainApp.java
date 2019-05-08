
package ch.sbb.ausbildung.eventsourcing.backend;

import akka.actor.ActorSystem;

/**
 * Main entry point of the app.
 * Starts the backend, bootstraps the actor system (name "eventsourcing") and creates the supervisor actor
 * acting as a root of the actor hierarchy. Simultaneously the actor system's config, e.g. backend config
 * is loaded from the file <code>resources/application.conf</code> (typesafe config)
 */
public class MainApp {

    public static void main(final String[] args) {
        // bootstrapping actor system
        ActorSystem system = ActorSystem.create("eventsourcing");

        // start supervisor i.e .root of actor system
        system.actorOf(Supervisor.props(), "supervisor");
    }
}
