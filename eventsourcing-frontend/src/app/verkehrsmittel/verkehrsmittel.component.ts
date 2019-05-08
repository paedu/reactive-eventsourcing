
import {Component, OnInit} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {AppState} from '../redux/app-state';
import {Observable} from 'rxjs';
import {Verkehrsmittel} from '../domain/verkehrsmittel';
import {UserActions} from '../redux/actions/user.actions';
import {VerkehrsmittelActions} from '../redux/actions/verkehrsmittel.actions';

/**
 * Verkehrsmittel component used to display the "verkehrsmittel" entities or objects from backend.
 * It reflects the state of the "verkehrsmittel" entities in real-time.
 * If the user wants to delay one of the "verkehrsmittel", the appropriate redux action will be dispatched.
 */
@Component({
  selector: 'app-verkehrsmittel',
  templateUrl: './verkehrsmittel.component.html',
  styleUrls: ['./verkehrsmittel.component.css']
})
export class VerkehrsmittelComponent implements OnInit {

  @select(['user', 'name']) user$: Observable<string>;
  @select(['verkehrsmittel']) verkehrsmittel$: Observable<Verkehrsmittel[]>;

  constructor(private ngRedux: NgRedux<AppState>) {
  }

  ngOnInit() {
    console.info('verkehrsmittel component started');
    this.ngRedux.dispatch(UserActions.loadVerkehrsmittel());
  }

  public delayVerkehrsmittel(vmNummer: number, delay: number): void {
    console.info('delay clicked for: ' + vmNummer + ' delay=' + delay);
    if (delay) {
      this.ngRedux.dispatch(VerkehrsmittelActions.delay(vmNummer, delay));
    }
  }

}
