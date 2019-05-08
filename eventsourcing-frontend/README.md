## Reactive Eventsourcing e2e - Frontend Part

### Content
Here you'll find the frontend part of the example based on Angular and Redux (i.e. "eventsourcing for ui").

* `websocketservice` - angular2 compliant websocket service which uses the rx.js websocket subject and observable.
  it wil be started by eventdispatcher when the main component gets loaded
* `eventdispatcher` - event dispatcher service where all the traffic from backend to frontend flows through. According to their types
   it maps the incoming JSON events into the appropriate flux actions and dispatches them to the redux store (via reducer).
* `verkehrsmittel` - angular component which receives the event stream from backend transforming it to a real-time display 
   of changes (incl. live updates)
* `redux store` - the one and only place where the app state should remain! changes to this state can only be made via events
   aka. "actions". so, one-way data binding and "on-push" change detection are all we need in this scenario.
* `redux reducers` - all "actions" (events) are dispatched to the reducer: it takes the current state and
   the action passed in in order to reduce them to a single result - the new app state (i.e. new instance of it)
* `redux middleware aka. epics or "side-effects"` - from time to time, it' not enough to simply dispatch an action to a reducer which
   mutates the app state and done. But in some cases we need to do a bit more, maybe trigger another action 
   or we wanna send a command to the backend in order to trigger other or more data etc.
   
### Run instructions
* The frontend is based on Angular and Redux (angular-redux), the minimal requirement to build and run it is `Node 8.x or 10.x` and `npm` as pkg manager.
* After you've cloned the repo into your own workspace, change to the frontend module directory and run the command `npm install` in a terminal or your IDE. 
  This will install all the node modules needed to run the UI (should create a `node_modules`directory or so called library root)
* Afterwards you can start the frontend by executing the command `npm start` 
  (this will redirect to the angular cli command `ng serve` which starts the app and finally opens a browser window showing up the UI)
* If you wanna play around with the eventstore and the events in the frontend you could install the [Redux Dev-Tools Extensions](https://github.com/zalmoxisus/redux-devtools-extension)
  They are available for the most common browsers like Chrome, FF etc..
  
You can also open two or more browser sessions in order to observe the real-time stream of changes aka. events from the backend to the UI.
Or you can trigger a new command by yourself (e.g. delay an existing means of transport e.g. train -> use input field and button)
and see what happens in the backend and the other sessions..
  
  Have fun!
