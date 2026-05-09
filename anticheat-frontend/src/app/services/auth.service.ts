import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly USER_KEY = 'user';
  private static readonly TOKEN_KEY = 'token';

  private currentUserSubject: BehaviorSubject<string | null>;

  constructor() {
    const storedUser = this.storage?.getItem(AuthService.USER_KEY) ?? null;
    this.currentUserSubject = new BehaviorSubject<string | null>(storedUser);
  }

  private get storage(): Storage | null {
    return typeof window !== 'undefined' ? window.localStorage : null;
  }

  setSession(user: string, token: string): void {
    this.currentUserSubject.next(user);
    this.storage?.setItem(AuthService.USER_KEY, user);
    this.storage?.setItem(AuthService.TOKEN_KEY, token);
  }

  getUser(): string | null {
    return this.currentUserSubject.value;
  }

  getToken(): string | null {
    return this.storage?.getItem(AuthService.TOKEN_KEY) ?? null;
  }

  isLoggedIn(): boolean {
    return !!this.currentUserSubject.value && !!this.getToken();
  }

  logout(): void {
    this.currentUserSubject.next(null);
    this.storage?.removeItem(AuthService.USER_KEY);
    this.storage?.removeItem(AuthService.TOKEN_KEY);
  }

  getCurrentUserObservable(): Observable<string | null> {
    return this.currentUserSubject.asObservable();
  }
}
