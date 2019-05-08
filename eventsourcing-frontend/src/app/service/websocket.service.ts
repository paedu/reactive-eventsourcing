
import {Injectable, OnDestroy} from '@angular/core';
import {Subject} from 'rxjs';
import {environment} from '../../environments/environment';
import {Router} from '@angular/router';
import {$WebSocket, WebSocketSendMode} from 'angular2-websocket/angular2-websocket';

/**
 * Service which establishes a websocket connection between the frontend and backend (bidirectional, for each browser session).
 * It's possible to configure an automatic reconnect when connection will be terminated unexpectedly.
 * The user can subscribe to incoming messages, errors or close events (Observable e.g. Subject)
 * and gets notified each time one of those events arrive.
 * It' also possible to send back messages to the backend, via the "send"-method (2-way communication)
 */
@Injectable({
  providedIn: 'root'
})
export class WebsocketService implements OnDestroy {

  private websocket$: $WebSocket;

  constructor(private router: Router) {
  }

  ngOnDestroy(): void {
    this.websocket$.close(true);
  }

  public setOnOpenCallbackAndGetDataStream(onOpenCallback: any): Subject<any> {
    // register (re)connect callback handler
    this.websocket$.onOpen(onOpenCallback);
    return this.websocket$.getDataStream();
  }

  public send(data: any): void {
    // send data in "direct" mode (no promise or observable as result)
    this.websocket$.send(JSON.stringify(data), WebSocketSendMode.Direct);
  }

  /**
   * Initializes the websocket and establishes the connection.
   * (wil be called when main app component starts, "ngOnInit")
   */
  public initWebsocket(): void {
    const url: string = environment.backendUrl;

    this.websocket$ = new $WebSocket(url, [] ,  {
      initialTimeout: 5000,
      maxTimeout: 60000,
      reconnectIfNotNormalClose: true
    });

    this.websocket$.onError((ev: Event) => this.cbErrorFromWebsocket(ev));
  }

  private cbErrorFromWebsocket(ev: Event): void {
    console.error(ev);
    this.router.navigate(['/error'], {queryParams: {msgkey: 'websocket_error_common'}});
  }
}
