import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/observable/dom/WebSocketSubject';

// Used by DefinePlugin
declare const ENV: string;

function calcWsUrl(): string {
  if (ENV === 'development') {
    const wsUrl = 'ws://localhost:8080/ws';
    const msg = 'DEVELOPMENT MODE ENGAGED - websocket URL:';
    // '='.repeat(msg.length + wsUrl.length + 1) +
    console.warn(`======================================\n${msg}`, wsUrl);
    return wsUrl;
  }

  if (window.location.protocol !== 'https:') {
    console.warn('Using insecure protocol', window.location.protocol);
  }
  return `${(window.location.protocol === 'https:') ? 'wss://' : 'ws://'}${window.location.hostname}:${window.location.port}/ws`;
}

const webSocketSubjectConfig: WebSocketSubjectConfig = {
  url: calcWsUrl(),
  openObserver: {
    next: (value: Event) => {
      console.log('WebSocket Opened', value);
      let payload = JSON.stringify({message: 'Text from Browser'});
      socket.socket.send(payload);
    }
  },
  closeObserver: {
    next: (closeEvent: CloseEvent) => {
      console.log('WebSocket Close', closeEvent);
    }
  },
  closingObserver: {
    next: (voidValue: void) => {
      console.log('WebSocket Closing', voidValue);
    }
  }
};

const socket: WebSocketSubject<any> = WebSocketSubject.create(webSocketSubjectConfig);

socket.subscribe(
  // next
  function (e) {
    console.debug('message', e);
  },
  // error
  function (error: any) {
    // errors and "unclean" closes land here
    console.error('error:', error);
  },
  // complete
  function () {
    // the socket has been closed
    console.info('socket closed');
  }
);
