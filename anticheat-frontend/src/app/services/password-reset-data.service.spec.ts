import { TestBed } from '@angular/core/testing';

import { PasswordResetDataService } from './password-reset-data.service';

describe('PasswordResetDataService', () => {
  let service: PasswordResetDataService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PasswordResetDataService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
