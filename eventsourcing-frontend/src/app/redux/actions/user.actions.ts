
import {FluxStandardAction} from 'flux-standard-action';
import {AnyAction} from 'redux';
import {User} from '../../domain/user';

/**
 * Type definitions and helper methods for user actions.
 *
 * @see https://redux.js.org/basics/actions
 */
type Payload = User | null;
type MetaData = unknown;

export type UserAction = FluxStandardAction<Payload, MetaData> & AnyAction;
export type UserErrorAction = FluxStandardAction<Error, MetaData>;

export class UserActions {

  // Backend actions (Events)
  static USERNAME_LOADED = 'username_loaded';

  // User actions (Commands sent to backend)
  static LOAD_USERNAME = 'load_username';
  static LOAD_VERKEHRSMITTEL = 'load_verkehrsmittel';


  // Actions originated by User, aka. "Commands"
  static loadUsername(): UserAction {
    return {
      type: UserActions.LOAD_USERNAME
    };
  }

  static loadVerkehrsmittel(): UserAction {
    return {
      type: UserActions.LOAD_VERKEHRSMITTEL
    };
  }

  static usernameLoaded(username: string): UserAction {
    return {
      type: UserActions.USERNAME_LOADED,
      payload: {name: username} as User
    };
  }

}
