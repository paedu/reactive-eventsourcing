## Reactive Eventsourcing e2e - Backend Part

### Content
Here you'll find the backend part of the example based on Akka's actor system, Akka Streams and Akka Http.

* `Supervisor` - root of actor hierarchy, bootstraps all other actors
* `InboundActor` - simulating a streaming command source (i.e. message queue or the like), here from a file
* `VerkehrsmittelActor` - persistent actor and aggregate root for "Verkehrsmittel" entities, it receives commands
  from inbound actor or from user, either refuses them if invalid or accepts them if ok. Accepted commands lead
  to events being generated and persisted into event store by this actor, moreover the events mutate the internal 
  state of this actor. That state can be restored by recovering all of the events from the store (while actor is restarting).
* `WebsocketActor` - handles the communication between a client and the backend, acting as a bridge which streams the
  events from backend to client and forwards user actions to the appropriate backend actor which can handle them.
  
### Run instruction hints:
* If you'll run this example please make sure that you use Java 11 as runtime
* Please make sure you have installed the lombok plugin or its annotation processor(s) in order to eliminate compile time errors!
* Copy the ["verkehrsmittel.csv"](src/main/resources/inbound/verkehrsmittel.csv) command file (UTF-8) out of your workspace
  and adjust the file path in `resources/application.conf` under the key `eventsourcing.command-file.path`(file system path, not class path!)
  The file should have line feeds that correspond to your os (windows CR LF, linux LF).
  
If you wanna change the file holding the inbound commands while the app is running, make sure that you've moved it away
from your workspace. Otherwise the IDE will continuously scan it for changes, so that mutations may not be possible.

This eventsoucing example uses a [FileTailSource (Akka Alpakka)](https://doc.akka.io/docs/alpakka/current/file.html#tailing-a-file-into-a-stream) 
to simulate an inbound command source (e.g. message queue or the like). If you manually append a new line aka. command to the `verkehrsmittel.csv` file
(incl. line feed at the end) while the app is running, you should see the command being processed by the VerkehrsmittelActor and instantly being pushed to the frontend.
This way you can simulate new commands flowing into the system from outside..

