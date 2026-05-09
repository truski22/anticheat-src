import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { confirmPasswordAccessGuard } from './confirm-password-access.guard';

describe('confirmPasswordAccessGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => confirmPasswordAccessGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
