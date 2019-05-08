# Reactive Eventsourcing e2e 

In the context of the "SBB IT Education Camp 2019" (SBB IT Ausbildungscamp 2019) this example was developed in order
to give the participants a first understanding of the meaning and the relevance of eventsourcing in our context (Swiss Federal Railways)

Some basic slides about the theory e.g. reactive programming, eventsourcing and CQRS, actor system etc.: [handson-eventsourcing-cqrs.pptx](./handson-eventsourcing-cqrs.pptx) 


## Example
It's an end-to-end example of eventsourcing, that means it streams events sourced in the backend to the frontend in real-time.
A more detailed description of the content of either back- or frontend can be found in the particular `readme.md` file:
*  [Backend Readme.md](eventsourcing-backend/README.md)
*  [Frontend Readme.md](eventsourcing-frontend/README.md)

#### Backend
The backend part is based on Akka's actor system, Akka Streams and Akka Http.

#### Frontend
In order to demonstrate the eventsourcing pattern also in the frontend, we use Redux in combination with Angular.

## Run instruction hints
You should be able to run the example (both parts) "as is", i.e. without installing and setting up additional libs or middleware.
However you should consider some hints in order to get this example running without problems:

#### Backend
* Use Java 11 as runtime for this example
* Either the Lombok plugin should be installed or lombok's annotation processor should be configured in your IDE 
  to avoid compile time errors. Although I'm not a fan of this kind of code generation it makes the code more clear here 
  and strips it down to the essential. 
* So that we can simulate an inbound streaming source (without additional middleware) we use the file `resources/verkehrsmittel.csv`
  acting as an inbound command source which continuously streams commands to the backend; one line here means a single command.
  Copy the file outside of your workspace (IDE) to avoid the circumstance, that your IDE will continuously scan or index it.
  The new file path must be configured in the `resources/application.conf` under the key `eventsourcing.command-file.path`(file system path, not class path!)
* Make sure your copy of the file is encoded in utf-8 and you use the right line feeds depending on your os (windows: CR LF, *nix: LF)

#### Frontend
* The frontend is based on Angular and Redux (angular-redux), the minimal requirement to build and run it is `Node 8.x or 10.x` and `npm` as pkg manager.
* After you've cloned the repo into your own workspace, change to the frontend module directory and run the command `npm install` in a terminal or IDE. This
  will install all the node_modules needed to run the UI (should create a `node_modules`directory)
* Afterwards you can start the UI with the `npm start` command
* If you wanna play around with the event store and the events in the frontend you could install the Redux Dev-Tools Extensions (for Chrome, FF, Edge).
  Have fun!
  
## Questions / Pull Requests
**Please don't hesitate to ask** if something is not clear to you or if you have a specific question about it.
Or place a pull request if you've found a bug or have some improvements you wanna share with us.

