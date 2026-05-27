import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from '../models/protocol';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  login(user: string, password: string): Observable<AuthResponse> {
    const body: LoginRequest = { user, password };
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, body);
  }

  register(user: string, email: string, password: string): Observable<AuthResponse> {
    const body: RegisterRequest = { user, email, password };
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, body);
  }

  forgotPassword(email: string): Observable<AuthResponse> {
    const body: ForgotPasswordRequest = { email };
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/forgot-password`, body);
  }

  resetPassword(email: string, code: string, password: string): Observable<AuthResponse> {
    const body: ResetPasswordRequest = { email, code, password };
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/reset-password`, body);
  }
}
