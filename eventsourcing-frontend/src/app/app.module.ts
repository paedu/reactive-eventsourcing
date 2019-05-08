import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {CoreModule} from './core/core.module';
import { VerkehrsmittelComponent } from './verkehrsmittel/verkehrsmittel.component';

/**
 * Main angular module loaded when starting the app.
 */
@NgModule({
  declarations: [
    AppComponent,
    VerkehrsmittelComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    CoreModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
