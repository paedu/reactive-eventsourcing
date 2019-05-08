
import {Injectable, OnDestroy} from '@angular/core';
import {AppState} from '../redux/app-state';
import {NgRedux} from '@angular-redux/store';
import {WebsocketService} from './websocket.service';
import {Subject, Subscription} from 'rxjs';
import {FluxStandardAction} from 'flux-standard-action';
import {UserActions} from '../redux/actions/user.actions';
import {VerkehrsmittelActions} from '../redux/actions/verkehrsmittel.actions';

/**
 * Event dispatcher service:
 * It subscribes to the websocket's event stream and dispatches all incoming messages (from backend)
 * according to their type (via redux).
 * After dispatching the action it will be first routed via a matching "side-effect" (middleware) aka. epic (if available)
 * and afterwards onward to the reducer which determines the new app state based on the current state and this action.
 * (therefore the name "reducer": it reduces the 2 values "current state" and "action" to the new app state)
 *
 * see https://redux.js.org/introduction/getting-started
 * see https://redux.js.org/basics/reducers
 */
@Injectable({
  providedIn: 'root'
})
export class EventDispatcherService implements OnDestroy {

  constructor(private ngRedux: NgRedux<AppState>,
              private websocketService: WebsocketService) {
  }

  private eventstream: Subject<any>;
  private subscription: Subscription;

  private static handleOnComplete(): void {
    console.warn(`websocket closed`);
  }

  private static handleError(error: any): void {
    console.error('websocket error received:', error);
  }

  ngOnDestroy(): void {
    console.log('event dispatcher service stopped');
    // cancel the subscription and close websocket
    this.subscription.unsubscribe();
    this.eventstream.complete();
  }

  public initDispatcherWhenWebsocketAvailable(): void {
    console.log('event dispatcher service started');

    this.eventstream = this.websocketService.setOnOpenCallbackAndGetDataStream(() => this.onReconnect());
    this.subscribeToEvents();
  }

  private onReconnect(): void {
    // trigger user name from backend
    this.ngRedux.dispatch(UserActions.loadUsername());
  }

  private subscribeToEvents(): void {
    this.subscription = this.eventstream.subscribe(
      event => this.handleEvent(JSON.parse(event.data)),
      error => EventDispatcherService.handleError(error),
      () => EventDispatcherService.handleOnComplete()
    );
  }

  private handleEvent(event: FluxStandardAction<any, any>): void {
    if (!event) {
      return;
    }
    switch (event.type) {
      case UserActions.USERNAME_LOADED:
        this.ngRedux.dispatch(UserActions.usernameLoaded(event.payload));
        break;
      case VerkehrsmittelActions.VERKEHRSMITTEL_CREATED:
        this.ngRedux.dispatch(VerkehrsmittelActions.created(event.meta, event.payload));
        break;
      case VerkehrsmittelActions.VERKEHRSMITTEL_MOVED:
        this.ngRedux.dispatch(VerkehrsmittelActions.moved(event.meta, event.payload));
        break;
      case VerkehrsmittelActions.VERKEHRSMITTEL_DELAYED:
        this.ngRedux.dispatch(VerkehrsmittelActions.delayed(event.meta, event.payload));
        break;
      case VerkehrsmittelActions.VERKEHRSMITTEL_ARRIVED:
        this.ngRedux.dispatch(VerkehrsmittelActions.arrived(event.meta));
        break;
      // Fallback
      default:
        console.error('unhandled event received:', event);
        break;
    }
  }
}

