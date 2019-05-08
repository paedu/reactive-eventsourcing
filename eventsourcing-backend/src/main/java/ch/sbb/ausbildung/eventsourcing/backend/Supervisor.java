
package ch.sbb.ausbildung.eventsourcing.backend;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import ch.sbb.ausbildung.eventsourcing.backend.client.HttpRoute;
import ch.sbb.ausbildung.eventsourcing.backend.in.InboundActor;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor;

/**
 * Supervisor actor and root of the (user's) actor hierarchy.
 * It is responsible for starting and supervising its child actors:<ul>
 *     <li>First, it starts the "Verkehrsmittel" actor (entity, persistent actor (eventsourced)</li>
 *     <li>Second, the http server and all its routes are bound to port 8080</li>
 *     <li>finally, the inbound actor is started. It holds a ref (ActorRef) to the Verkehrsmittel actor in order
 *     to forward the incoming commands to it</li>
 * </ul>
 *
 * @see <a href="https://akka.io/docs/">Akka Documentation</a>
 * @see <a href="https://doc.akka.io/docs/akka/current/general/supervision.html">Akka Actors and Supervision</a>
 */
public class Supervisor extends AbstractLoggingActor {

    // props used to create this actor
    static Props props() {
        return Props.create(Supervisor.class, Supervisor::new);
    }

    @Override
    public void preStart() throws Exception {
        // create the "verkehrsmittel" actor (DDD: aggregate root, bounded ctx for "verkehrsmittel") handling the commands and events
        ActorRef vmActor = context().actorOf(VerkehrsmittelActor.props(), "vmActor");

        // start HTTP server binding (port 8080) incl. routes and listen for incoming requests
        ActorSystem system = context().system();
        new HttpRoute().bindHttp(ActorMaterializer.create(context()), system, vmActor);
        // finally start the inbound streaming actor
        context().actorOf(InboundActor.props(vmActor));

        super.preStart();
    }

    // no message handling here (empty bahaviour)
    @Override
    public Receive createReceive() {
        return emptyBehavior();
    }
}
