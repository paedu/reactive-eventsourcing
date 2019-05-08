import {CommonModule} from '@angular/common';
import {NgModule, Optional, SkipSelf} from '@angular/core';
import {RouterModule} from '@angular/router';

import {throwIfAlreadyLoaded} from './module-import-guard';
import {DevToolsExtension, NgRedux, NgReduxModule} from '@angular-redux/store';
import {createEpicMiddleware, Epic} from 'redux-observable';
import {applyMiddleware, createStore, Middleware, Store, StoreEnhancer} from 'redux';
import {composeWithDevTools} from 'redux-devtools-extension';
import {AppState, INITIAL_APP_STATE} from '../redux/app-state';
import {combineDecoratedEpics} from 'redux-observable-decorator';
import {rootReducer} from '../redux/reducers';
import {RootEpic} from '../redux/epics/root.epic';

/**
 * Core helper module to bootstrap the redux dev tools (for chrome, ff, edge) and
 * to prevent angular from loading modules twice.
 */
@NgModule({
  imports: [
    CommonModule,
    /* HttpClientModule, */
    NgReduxModule,
    RouterModule
  ],
  declarations: [],
  exports: [],
  providers: []
})
export class CoreModule {

  constructor(@Optional() @SkipSelf() parentModule: CoreModule,
              private ngRedux: NgRedux<AppState>,
              private reduxDevTools: DevToolsExtension,
              private rootEpic: RootEpic) {

    throwIfAlreadyLoaded(parentModule, 'core module');

    const epicMiddleware = createEpicMiddleware();
    const store: Store<AppState> = createStore(rootReducer, INITIAL_APP_STATE, this.createEnhancer(epicMiddleware));
    const epics: Epic = combineDecoratedEpics(...this.rootEpic.createEpics());
    this.ngRedux.provideStore(store);

    epicMiddleware.run(epics);
  }

  /**
   * Creates a store enhancer from the middleware (epics), depending on the availability of the dev tools.
   */
  private createEnhancer(middleware: Middleware<{}, AppState, any>): StoreEnhancer<{ dispatch: {} }> {
    const storeEnhancer: StoreEnhancer<{ dispatch: {} }> = applyMiddleware(middleware);
    if (this.reduxDevTools.isEnabled()) {
      return composeWithDevTools(storeEnhancer);
    }
    return storeEnhancer;
  }
}
