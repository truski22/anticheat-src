import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { Subscription } from 'rxjs';
import { WebsocketService } from '../../services/websocket.service';
import { AuthService } from '../../services/auth.service';
import {
  MessageType,
  ChangePasswordPayload,
  ResponsePayload,
} from '../../models/protocol';

@Component({
  selector: 'app-reset-password-logged',
  imports: [FormsModule, NgIf],
  templateUrl: './reset-password-logged.component.html',
  styleUrl: './confirm-reset-password.component.css'
})
export class ResetPasswordLoggedComponent implements OnDestroy {
  newPassword: string = '';
  confirmPassword: string = '';
  errorMessage: string = '';
  loading: boolean = false;
  private sub?: Subscription;

  constructor(
    private wsService: WebsocketService,
    private authService: AuthService,
    private router: Router
  ) {}

  changePassword(): void {
    if (this.newPassword.length < 8) {
      this.errorMessage = 'La contraseña debe tener al menos 8 caracteres';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    const token = this.authService.getToken();
    if (!token) {
      this.router.navigate(['']);
      return;
    }

    this.errorMessage = '';
    this.loading = true;

    if (!this.wsService.isConnected) {
      this.wsService.connect(token);
    }

    this.sub = this.wsService.on<ResponsePayload>(MessageType.CHANGE_PASSWORD_RESPONSE).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.router.navigate(['/profile']);
        } else {
          this.errorMessage = res.message || 'Error al cambiar la contraseña';
        }
      }
    });

    const payload: ChangePasswordPayload = { password: this.newPassword };
    this.wsService.send(MessageType.CHANGE_PASSWORD, payload);
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
