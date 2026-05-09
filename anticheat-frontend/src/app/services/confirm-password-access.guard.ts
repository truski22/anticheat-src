import {CanActivateFn, Router} from '@angular/router';
import {inject} from '@angular/core';
import {PasswordResetDataService} from './password-reset-data.service';

export const confirmPasswordAccessGuard: CanActivateFn = (route, state) => {
  const passwordResetService = inject(PasswordResetDataService);
  const router = inject(Router);
  if(passwordResetService.getCorrectCode()){
    return true;
  }
  router.navigate(['password/reset'])
  return false;
};
