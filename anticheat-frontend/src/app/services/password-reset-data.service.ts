import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PasswordResetDataService {

  private email:string='';
  private code:string='';
  private emailSend:boolean=false;
  private correctCode:boolean=false;
  setEmail(email: string) {
    this.email = email;
  }

  getEmail(): string {
    return this.email;
  }

  setCode(code: string) {
    this.code = code;
  }

  getCode(): string {
    return this.code;
  }
  setCorrectCode(correctCode:boolean){
    this.correctCode=correctCode;
  }
  getCorrectCode():boolean{
    return this.correctCode;
  }
  setEmailSend(emailSend:boolean){
    this.emailSend=emailSend;
  }
  getEmailSend():boolean{
    return this.emailSend;
  }
}
