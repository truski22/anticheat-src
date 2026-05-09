import { Component, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { NgIf, NgFor } from '@angular/common';
import { Subscription } from 'rxjs';
import { WebsocketService } from '../../services/websocket.service';
import { AuthService } from '../../services/auth.service';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { MessageType, GameEntry, GamesPayload } from '../../models/protocol';

@Component({
  selector: 'app-games',
  imports: [NgIf, NgFor, NavbarComponent],
  templateUrl: './games.component.html',
  styleUrl: './games.component.css'
})
export class GamesComponent implements OnDestroy {
  dataLoaded = false;
  games_list: GameEntry[] = [];
  private sub?: Subscription;

  constructor(
    private wsService: WebsocketService,
    private authService: AuthService,
    private router: Router
  ) {
    const user = authService.getUser();
    const token = authService.getToken();

    if (user && token) {
      if (!this.wsService.isConnected) {
        this.wsService.connect(token);
      }

      this.sub = this.wsService.on<GamesPayload>(MessageType.GAMES).subscribe(payload => {
        this.games_list = payload.games;
        this.dataLoaded = true;
      });

      const sendRequest = () => {
        if (this.wsService.isConnected) {
          this.wsService.send(MessageType.GAMES_REQUEST);
        } else {
          setTimeout(sendRequest, 200);
        }
      };
      setTimeout(sendRequest, 100);
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}

