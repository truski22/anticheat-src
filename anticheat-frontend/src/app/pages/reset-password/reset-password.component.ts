import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { PasswordResetDataService } from '../../services/password-reset-data.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent {
  email: string = '';
  waiting: boolean = false;
  sent: boolean = false;

  constructor(
    private apiService: ApiService,
    private router: Router,
    private passwordDataService: PasswordResetDataService
  ) {}

  sendEmail(): void {
    this.waiting = true;
    this.passwordDataService.setEmail(this.email);

    this.apiService.forgotPassword(this.email).subscribe({
      next: () => {
        this.waiting = false;
        this.sent = true;
        this.passwordDataService.setEmailSend(true);
        this.router.navigate(['/password/reset/changePassword']);
      },
      error: () => {
        this.waiting = false;
        // Still navigate — backend always returns 200 to prevent user enumeration
        this.sent = true;
        this.passwordDataService.setEmailSend(true);
        this.router.navigate(['/password/reset/changePassword']);
      }
    });
  }
}
