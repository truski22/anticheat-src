import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  @Input() activePage: 'home' | 'games' | 'profile' = 'home';
  menuOpen = false;

  constructor(private router: Router) {}

  home()    { this.router.navigate(['/home']); this.menuOpen = false; }
  games()   { this.router.navigate(['/games']);     this.menuOpen = false; }
  profile() { this.router.navigate(['/profile']);   this.menuOpen = false; }

  toggleMenu() { this.menuOpen = !this.menuOpen; }
}
