
import {Injectable} from '@angular/core';
import {UserEpics} from './user.epics';

/**
 * Redux Middleware:
 * Root epic or "side-effect" which loads and aggregates all child epics (not needed in our case..)
 *
 * @see https://redux.js.org/advanced/middleware
 */
@Injectable({
  providedIn: 'root'
})
export class RootEpic {
  constructor(private userEpics: UserEpics) {
  }

  createEpics(): any[] {
    return [
      this.userEpics
    ];
  }
}
