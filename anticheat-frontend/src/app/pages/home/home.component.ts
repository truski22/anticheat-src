import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  user: string = '';
  password: string = '';
  errorMessage: string = '';
  loading: boolean = false;

  constructor(
    private authService: AuthService,
    private apiService: ApiService,
    private wsService: WebsocketService,
    private router: Router
  ) {}

  login(): void {
    this.errorMessage = '';
    this.loading = true;

    this.apiService.login(this.user, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.token) {
          this.authService.setSession(this.user, res.token);
          this.wsService.connect(res.token);
          this.router.navigate(['/home']);
        } else {
          this.errorMessage = res.message || 'Usuario o contraseña incorrectos';
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Usuario o contraseña incorrectos';
        } else {
          this.errorMessage = 'Error de conexión con el servidor';
        }
      }
    });
  }

  resetPassword(): void {
    this.router.navigate(['/password/reset']);
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }
}
