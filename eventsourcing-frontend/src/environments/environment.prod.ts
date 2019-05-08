import {Environment} from './environment.model';

export const environment: Environment = {
  production: true,
  serviceName: 'eventsourcing-frontend',
  backendUrl: 'ws://localhost:8080/websocket'
};
