
import {Epic as IEpic, ofType} from 'redux-observable';
import {Injectable} from '@angular/core';
import {Epic} from 'redux-observable-decorator';
import {AppState} from '../app-state';
import {Observable} from 'rxjs';
import {UserAction, UserActions} from '../actions/user.actions';
import {WebsocketService} from '../../service/websocket.service';
import {ignoreElements, tap} from 'rxjs/operators';
import {VerkehrsmittelAction, VerkehrsmittelActions} from '../actions/verkehrsmittel.actions';

/**
 * Redux middleware:
 * User epic or "side-effect" used to send a command to the backend when the appropriate action is dispatched
 *
 * @see https://redux.js.org/advanced/middleware
 */
@Injectable({
  providedIn: 'root'
})
export class UserEpics {
  constructor(private websocketService: WebsocketService) {
  }

  /**
   * Epic or "side-effect" to trigger the initial load of the user name from backend
   */
  @Epic()
  usernameEpic: IEpic<UserAction, UserAction, AppState, void> =
    (action$: Observable<UserAction>): Observable<UserAction> =>
      action$.pipe(
        ofType(UserActions.LOAD_USERNAME),
        tap(() => this.websocketService.send(UserActions.loadUsername())),
        ignoreElements()
      )

  /**
   *  Epic or "side-effect" to trigger the initial load of the "verkehrsmittel" from backend
   */
  @Epic()
  loadVerkehrsmittelEpic: IEpic<UserAction, UserAction, AppState, void> =
    (action$: Observable<UserAction>): Observable<UserAction> =>
      action$.pipe(
        ofType(UserActions.LOAD_VERKEHRSMITTEL),
        tap(() => this.websocketService.send(UserActions.loadVerkehrsmittel())),
        ignoreElements()
      )

  /**
   *  Epic or "side-effect" to signal the delay of a specific "verkehrsmittel" to the backend
   */
  @Epic()
  delayVerkehrsmittelEpic: IEpic<UserAction, UserAction, AppState, void> =
    (action$: Observable<VerkehrsmittelAction>): Observable<VerkehrsmittelAction> =>
      action$.pipe(
        ofType(VerkehrsmittelActions.DELAY_VERKEHRSMITTEL),
        tap((action) => this.websocketService.send(VerkehrsmittelActions.delay(action.meta, action.payload as number))),
        ignoreElements()
      )
}
