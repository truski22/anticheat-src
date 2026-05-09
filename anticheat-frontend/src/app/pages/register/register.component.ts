import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, NgIf],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  user: string = '';
  email: string = '';
  password: string = '';
  passwordConf: string = '';
  errorMessage: string = '';
  loading: boolean = false;

  constructor(private apiService: ApiService, private router: Router) {}

  register(): void {
    if (this.password.length < 8) {
      this.errorMessage = 'La contraseña debe tener al menos 8 caracteres';
      return;
    }
    if (this.password !== this.passwordConf) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    this.errorMessage = '';
    this.loading = true;

    this.apiService.register(this.user, this.email, this.password).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.router.navigate(['']);
        } else {
          this.handleRegisterError(res.message);
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.error?.message) {
          this.handleRegisterError(err.error.message);
        } else {
          this.errorMessage = 'Error de conexión con el servidor';
        }
      }
    });
  }

  private handleRegisterError(message: string): void {
    if (message.includes('UNV') || message.toLowerCase().includes('user')) {
      this.errorMessage = 'El nombre de usuario ya está en uso';
    } else if (message.includes('ENV') || message.toLowerCase().includes('email')) {
      this.errorMessage = 'El correo electrónico ya está en uso';
    } else {
      this.errorMessage = message || 'Error al registrar';
    }
  }
}
