import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResetPasswordLoggedComponent } from './confirm-reset-password.component';

describe('ResetPasswordLoggedComponent', () => {
  let component: ResetPasswordLoggedComponent;
  let fixture: ComponentFixture<ResetPasswordLoggedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPasswordLoggedComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ResetPasswordLoggedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
