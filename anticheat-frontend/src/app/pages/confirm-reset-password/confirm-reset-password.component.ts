import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { PasswordResetDataService } from '../../services/password-reset-data.service';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-confirm-reset-password',
  imports: [FormsModule, NgIf],
  templateUrl: './confirm-reset-password.component.html',
  styleUrl: './confirm-reset-password.component.css'
})
export class ConfirmResetPasswordComponent {
  code: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  errorMessage: string = '';
  loading: boolean = false;

  constructor(
    private passwordDataService: PasswordResetDataService,
    private apiService: ApiService,
    private router: Router
  ) {}

  changePassword(): void {
    if (!this.code || this.code.trim().length === 0) {
      this.errorMessage = 'Introduce el código de verificación';
      return;
    }
    if (this.newPassword.length < 8) {
      this.errorMessage = 'La contraseña debe tener al menos 8 caracteres';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    this.errorMessage = '';
    this.loading = true;
    const email = this.passwordDataService.getEmail();

    this.apiService.resetPassword(email, this.code.trim(), this.newPassword).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.router.navigate(['']);
        } else {
          this.errorMessage = res.message || 'Error al cambiar la contraseña';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Código inválido o expirado';
      }
    });
  }
}
