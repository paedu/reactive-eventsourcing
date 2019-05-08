import {Component, OnDestroy, OnInit} from '@angular/core';
import {WebsocketService} from './service/websocket.service';
import {EventDispatcherService} from './service/event-dispatcher.service';

/**
 * Main component bootstrapped by the app module.
 * It starts the websocket and event dispatcher services in order to establish the 2-way communication
 * between backend and frontend.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Eventsourcing Frontend';

  constructor(private websocketService: WebsocketService,
              private eventDispatcherService: EventDispatcherService) {
  }

  ngOnInit(): void {
    // start websocket service
    this.websocketService.initWebsocket();

    // initialize event dispatchers
    this.eventDispatcherService.initDispatcherWhenWebsocketAvailable();
  }

  ngOnDestroy(): void {
    console.info('Stopping app');
  }

}
