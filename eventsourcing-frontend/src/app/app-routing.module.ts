import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {VerkehrsmittelComponent} from './verkehrsmittel/verkehrsmittel.component';

/**
 * Routing definitions.
 */
const routes: Routes = [
  {path: '**', component: VerkehrsmittelComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
