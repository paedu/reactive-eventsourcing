
import {Verkehrsmittel} from '../domain/verkehrsmittel';
import {User} from '../domain/user';

/**
 * This AppState interface defines the central redux store and therewith the one and only state of the whole app!
 *
 * <b>Changes on this app state can only be made via redux' actions (i.e. events / commands).</b>
 * The app state is immutable, no direct access or mutations on this structure allowed!
 *
 * So, actions which wanna "mutate the state" must be dispatched by redux and pass the reducer
 * in order to return a new instance of the app state.
 *
 * @see https://redux.js.org/basics/store
 */
export interface AppState {
  // current user
  user: User;
  verkehrsmittel: Verkehrsmittel[];
}


export const INITIAL_APP_STATE: AppState = {
  user: {name: '<undefined>'},
  verkehrsmittel: []
};
