
import {FluxStandardAction} from 'flux-standard-action';
import {AnyAction} from 'redux';
import {Verkehrsmittel} from '../../domain/verkehrsmittel';

/**
 * Type definitions and helper methods for verkehrsmittel actions.
 *
 * @see https://redux.js.org/basics/actions
 */
type Payload = Verkehrsmittel | string | number | null;
type MetaData = number | null;

export type VerkehrsmittelAction = FluxStandardAction<Payload, MetaData> & AnyAction;
export type VerkehrsmittelErrorAction = FluxStandardAction<Error, MetaData>;

export class VerkehrsmittelActions {

  // Backend actions (Events)
  static VERKEHRSMITTEL_CREATED = 'verkehrsmittel_created';
  static VERKEHRSMITTEL_MOVED = 'verkehrsmittel_moved';
  static VERKEHRSMITTEL_ARRIVED = 'verkehrsmittel_arrived';
  static VERKEHRSMITTEL_DELAYED = 'verkehrsmittel_delayed';

  // User actions (Commands)
  static DELAY_VERKEHRSMITTEL = 'delay_verkehrsmittel';


  // Actions originated by User, aka. "Commands"
  static delay(vmNummer: number, delay: number): VerkehrsmittelAction {
    return {
      type: VerkehrsmittelActions.DELAY_VERKEHRSMITTEL,
      payload: delay,
      meta: vmNummer
    };
  }

  // Actions originated by Backend, aka. "Events"
  static created(vmNummer: number, verkehrsmittel: Verkehrsmittel): VerkehrsmittelAction {
    return {
      type: VerkehrsmittelActions.VERKEHRSMITTEL_CREATED,
      payload: verkehrsmittel,
      meta: vmNummer
    };
  }

  static moved(vmNummer: number, aktuellePosition: string): VerkehrsmittelAction {
    return {
      type: VerkehrsmittelActions.VERKEHRSMITTEL_MOVED,
      payload: aktuellePosition,
      meta: vmNummer
    };
  }

  static delayed(vmNummer: number, delay: number): VerkehrsmittelAction {
    return {
      type: VerkehrsmittelActions.VERKEHRSMITTEL_DELAYED,
      payload: delay,
      meta: vmNummer
    };
  }

  static arrived(vmNummer: number): VerkehrsmittelAction {
    return {
      type: VerkehrsmittelActions.VERKEHRSMITTEL_ARRIVED,
      payload: undefined,
      meta: vmNummer
    };
  }

}
