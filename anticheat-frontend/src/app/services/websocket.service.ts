import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Observable, Subject, filter, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChessMessage, MessageType } from '../models/protocol';

@Injectable({ providedIn: 'root' })
export class WebsocketService implements OnDestroy {
  private socket: WebSocket | null = null;
  private messagesSubject = new Subject<ChessMessage>();
  private connectionSubject = new Subject<boolean>();
  private token: string | null = null;
  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private intentionalClose = false;

  private static readonly MAX_RECONNECT_ATTEMPTS = 10;
  private static readonly BASE_DELAY_MS = 1000;
  private static readonly MAX_DELAY_MS = 30000;

  readonly messages$ = this.messagesSubject.asObservable();
  readonly connected$ = this.connectionSubject.asObservable();

  constructor(private ngZone: NgZone) {}

  connect(token: string): void {
    this.token = token;
    this.intentionalClose = false;
    this.reconnectAttempts = 0;
    this.clearReconnectTimer();
    this.createConnection();
  }

  private createConnection(): void {
    if (!this.token) return;
    this.closeSocket();

    const url = `${environment.wsUrl}?token=${encodeURIComponent(this.token)}`;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.ngZone.run(() => {
        this.reconnectAttempts = 0;
        this.connectionSubject.next(true);
      });
    };

    this.socket.onmessage = (event) => {
      try {
        const msg: ChessMessage = JSON.parse(event.data);
        this.ngZone.run(() => this.messagesSubject.next(msg));
      } catch (e) {
        console.error('[WS] Failed to parse message:', e);
      }
    };

    this.socket.onclose = (event) => {
      this.ngZone.run(() => {
        this.connectionSubject.next(false);
        if (!this.intentionalClose && event.code !== 1008) {
          this.scheduleReconnect();
        }
      });
    };

    this.socket.onerror = () => {};
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= WebsocketService.MAX_RECONNECT_ATTEMPTS) {
      console.error('[WS] Max reconnect attempts reached');
      return;
    }

    const delay = Math.min(
      WebsocketService.BASE_DELAY_MS * Math.pow(2, this.reconnectAttempts),
      WebsocketService.MAX_DELAY_MS
    );
    this.reconnectAttempts++;

    this.reconnectTimer = setTimeout(() => {
      this.createConnection();
    }, delay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  send<T>(type: MessageType, payload?: T): void {
    if (this.socket?.readyState !== WebSocket.OPEN) {
      console.error('[WS] Cannot send — socket not open');
      return;
    }
    const message: ChessMessage<T> = { type, payload };
    this.socket.send(JSON.stringify(message));
  }

  on<T>(type: MessageType): Observable<T> {
    return this.messages$.pipe(
      filter(msg => msg.type === type),
      map(msg => msg.payload as T)
    );
  }

  close(): void {
    this.intentionalClose = true;
    this.clearReconnectTimer();
    this.token = null;
    this.closeSocket();
  }

  private closeSocket(): void {
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.close();
      this.socket = null;
    }
  }

  get isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  ngOnDestroy(): void {
    this.close();
    this.messagesSubject.complete();
    this.connectionSubject.complete();
  }
}
