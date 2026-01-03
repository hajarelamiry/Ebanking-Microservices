import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <nav class="navbar navbar-expand-lg py-4 px-4 px-lg-5 bg-transparent">
      <div class="container-fluid p-0">
        <!-- Logo (Ghulam Style Minimal) -->
        <a class="navbar-brand d-flex align-items-center me-5" routerLink="/">
          <div class="brand-bar bg-dark me-1" style="width: 18px; height: 4px; border-radius: 2px;"></div>
          <div class="brand-bar bg-dark" style="width: 12px; height: 4px; border-radius: 2px;"></div>
        </a>

        <!-- Middle Navigation (Optional) -->
        <div class="d-none d-lg-flex gap-4">
           <a routerLink="/dashboard" class="nav-link-noir active">Income</a>
           <a routerLink="/dashboard" class="nav-link-noir">Outcome</a>
        </div>

        <!-- Actions -->
        <div class="ms-auto d-flex align-items-center gap-3">
           <button class="btn btn-outline-dark rounded-circle d-flex align-items-center justify-content-center p-0" style="width: 40px; height: 40px;">
              <i class="bi bi-search small"></i>
           </button>
           <button class="btn btn-dark rounded-circle d-flex align-items-center justify-content-center p-0 shadow-lg" style="width: 40px; height: 40px;" (click)="logout()">
              <i class="bi bi-power small"></i>
           </button>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .nav-link-noir { text-decoration: none; color: #8E8E93; font-weight: 600; font-size: 0.95rem; padding: 8px 20px; border-radius: 12px; transition: all 0.2s; }
    .nav-link-noir.active { background: white; color: #000; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
    .nav-link-noir:hover:not(.active) { color: #000; }
  `]
})
export class NavbarComponent implements OnInit {
  constructor(private readonly keycloak: KeycloakService) { }
  async ngOnInit() { }
  logout() { this.keycloak.logout(); }
}
