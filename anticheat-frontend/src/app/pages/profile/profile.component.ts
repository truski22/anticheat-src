import { Component, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import { WebsocketService } from '../../services/websocket.service';
import { AuthService } from '../../services/auth.service';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { MessageType, UserInfoPayload } from '../../models/protocol';

@Component({
  selector: 'app-profile',
  imports: [NgIf, NavbarComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnDestroy {
  user: string | null = '';
  correoUser: string = '';
  numPartidas: number = 0;
  numPartidasLegales: number = 0;
  numPartidasTrampa: number = 0;
  dataLoaded = false;
  private sub?: Subscription;

  constructor(
    private wsService: WebsocketService,
    private authService: AuthService,
    private router: Router
  ) {
    this.user = authService.getUser();
    const token = authService.getToken();

    if (this.user && token) {
      if (!this.wsService.isConnected) {
        this.wsService.connect(token);
      }

      this.sub = this.wsService.on<UserInfoPayload>(MessageType.USER_INFO).subscribe(payload => {
        this.correoUser = payload.email;
        this.numPartidas = payload.totalGames;
        this.numPartidasTrampa = payload.cheatGames;
        this.numPartidasLegales = payload.legalGames;
        this.dataLoaded = true;
      });

      // Wait briefly for connection to establish, then send request
      const sendRequest = () => {
        if (this.wsService.isConnected) {
          this.wsService.send(MessageType.USER_INFO_REQUEST);
        } else {
          setTimeout(sendRequest, 200);
        }
      };
      setTimeout(sendRequest, 100);
    }
  }

  changePassword(): void { this.router.navigate(['/password/changePassword']); }

  logout(): void {
    this.wsService.close();
    this.authService.logout();
    this.router.navigate(['']);
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
