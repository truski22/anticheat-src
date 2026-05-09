import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { authGuard } from './services/auth.guard';
import { confirmPasswordAccessGuard } from './services/confirm-password-access.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'password/reset',
    loadComponent: () => import('./pages/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
  },
  {
    path: 'password/reset/changePassword',
    loadComponent: () => import('./pages/confirm-reset-password/confirm-reset-password.component').then(m => m.ConfirmResetPasswordComponent),
    canActivate: [confirmPasswordAccessGuard],
  },
  {
    path: 'password/changePassword',
    loadComponent: () => import('./pages/confirm-reset-password/reset-password-logged.component').then(m => m.ResetPasswordLoggedComponent),
    canActivate: [authGuard],
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: 'games',
    loadComponent: () => import('./pages/games/games.component').then(m => m.GamesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'home',
    loadComponent: () => import('./pages/principal/principal.component').then(m => m.PrincipalComponent),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: '', pathMatch: 'full' },
];
