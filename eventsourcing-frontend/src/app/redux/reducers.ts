
import {AppState} from './app-state';
import {FluxStandardAction} from 'flux-standard-action';
import {Reducer} from 'redux';
import {tassign} from 'tassign';
import {VerkehrsmittelActions} from './actions/verkehrsmittel.actions';
import {UserActions} from './actions/user.actions';
import {Verkehrsmittel} from '../domain/verkehrsmittel';

/**
 * Redux reducer(s) - pure functional code:
 * it "reduces" the 2 values, "current state" and "action", to the new app state by applying pure functional logic.
 * That means, depending on the action passed in the matching case statement will be triggered if available.
 * Therein, all the functional things happen in order to get the new app state back as result.
 *
 * IMPORTANT: so that the change detection will recognize that something changed, it is important
 * that the reducer always returns a new instance of the new app state or the part of it which has changed.
 * Otherwise it's not sure that angular can detect these changes.
 * Therefore the spread and "tassign" operator are widely used herein.
 *
 * @param state the current app state
 * @param action the action passed in (via redux.dispatch(..) )
 *
 * @see https://redux.js.org/basics/reducers
 */
export const rootReducer: Reducer<AppState, FluxStandardAction<any, any>> =
  (state: AppState, action: FluxStandardAction<any, any>): AppState => {
    // simple error handler (console ;-)
    if (action.error) {
      handleError(action.payload);
      return state;
    }
    switch (action.type) {
      // User-Actions
      case UserActions.USERNAME_LOADED:
        return tassign(state, {user: action.payload});

      // Verkehrsmittel-Actions
      case VerkehrsmittelActions.VERKEHRSMITTEL_CREATED:
        return state.verkehrsmittel.findIndex(verkehrsmittel => verkehrsmittel.vmNummer === action.meta as number) !== -1 ?
          tassign(state, {
            verkehrsmittel: state.verkehrsmittel.map(verkehrsmittel => {
              if (verkehrsmittel.vmNummer === action.meta as number) {
                return action.payload as Verkehrsmittel;
              }
              return verkehrsmittel;
            })
          }) :
          tassign(state, {verkehrsmittel: [...state.verkehrsmittel, action.payload]});
      case VerkehrsmittelActions.VERKEHRSMITTEL_MOVED:
        return tassign(state, {
          verkehrsmittel: state.verkehrsmittel.map(verkehrsmittel => {
            if (verkehrsmittel.vmNummer === action.meta as number) {
              return tassign(verkehrsmittel, {aktuellePosition: action.payload as string});
            }
            return verkehrsmittel;
          })
        });
      case VerkehrsmittelActions.VERKEHRSMITTEL_DELAYED:
        return tassign(state, {
          verkehrsmittel: state.verkehrsmittel.map(verkehrsmittel => {
            if (verkehrsmittel.vmNummer === action.meta as number) {
              return tassign(verkehrsmittel, {delay: action.payload as number});
            }
            return verkehrsmittel;
          })
        });
    }
    return state;
  };

function handleError(error: any): void {
  console.error(error);
}
