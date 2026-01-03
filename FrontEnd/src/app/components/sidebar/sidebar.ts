import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router, RouterModule } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive],
  template: `
    <div class="sidebar d-flex flex-column p-3">
      
      <!-- Brand / Logo Section -->
      <div class="brand-section mb-4 d-flex justify-content-center pt-3 w-100">
        <div class="logo-img-container">
          <img src="/img/logo.png" alt="Capitalis Logo" class="brand-logo">
        </div>
      </div>

      <!-- Navigation Links -->
      <nav class="flex-grow-1 mt-2">
        <div class="menu-label text-uppercase mb-2 px-3">Main Menu</div>
        <ul class="nav flex-column gap-1 mb-4">
          <li class="nav-item">
            <a [routerLink]="isAdminOrAgent ? '/admin/dashboard' : '/dashboard'" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
              <i class="bi bi-grid-fill"></i>
              <span class="fw-semibold">Dashboard</span>
            </a>
          </li>
          
          <ng-container *ngIf="!isAdminOrAgent">
            <li class="nav-item">
              <a routerLink="/transactions" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
                <i class="bi bi-arrow-left-right"></i>
                <span class="fw-semibold">Transactions</span>
              </a>
            </li>
            <li class="nav-item">
              <a routerLink="/payments" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
                <i class="bi bi-wallet2"></i>
                <span class="fw-semibold">Payments</span>
              </a>
            </li>
            <li class="nav-item">
              <a routerLink="/cards" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
                <i class="bi bi-credit-card"></i>
                <span class="fw-semibold">My Cards</span>
              </a>
            </li>
          </ng-container>

          <ng-container *ngIf="isAdminOrAgent">
             <li class="nav-item">
                <a routerLink="/admin/users" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
                  <i class="bi bi-people-fill"></i>
                  <span class="fw-semibold">Users List</span>
                </a>
              </li>
              <li class="nav-item">
                <a routerLink="/admin/kyc" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
                  <i class="bi bi-shield-check"></i>
                  <span class="fw-semibold">KYC Verification</span>
                </a>
              </li>
          </ng-container>
        </ul>

        <div class="menu-label text-uppercase mb-2 px-3">Personal & Security</div>
        <ul class="nav flex-column gap-1">
          <li class="nav-item" *ngIf="!isAdminOrAgent">
            <a routerLink="/profile" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
              <i class="bi bi-person-circle"></i>
              <span class="fw-semibold">My Profile</span>
            </a>
          </li>
          <li class="nav-item" *ngIf="!isAdminOrAgent">
            <a routerLink="/kyc" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
              <i class="bi bi-patch-check"></i>
              <span class="fw-semibold">Verification</span>
            </a>
          </li>
          <li class="nav-item">
            <a routerLink="/security" routerLinkActive="active" class="sidebar-link d-flex align-items-center gap-3 px-3 py-2">
              <i class="bi bi-lock-fill"></i>
              <span class="fw-semibold">Security Vault</span>
            </a>
          </li>
        </ul>
      </nav>

      <!-- Logout Section -->
      <div class="mt-auto pt-4 border-top border-light">
        <button (click)="logout()" class="logout-btn w-100 d-flex align-items-center gap-3 px-3 py-2 border-0 bg-transparent text-muted">
          <i class="bi bi-box-arrow-right"></i>
          <span class="fw-semibold">Sign Out</span>
        </button>
      </div>

    </div>
  `,
  styles: [`
    .sidebar { width: 230px; background-color: #FFFFFF; height: 100vh; position: sticky; top: 0; border-right: 1px solid #F0F0F0; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .fw-extrabold { font-weight: 800; }
    
    .logo-img-container { width: 100%; height: 60px; display: flex; align-items: center; justify-content: center; }
    .brand-logo { max-width: 140px; max-height: 50px; object-fit: contain; }

    .menu-label { font-size: 0.65rem; font-weight: 800; letter-spacing: 1px; color: #ADB5BD; }

    .sidebar-link { 
      color: #6C757D; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); border-radius: 12px; text-decoration: none; font-size: 0.9rem; font-family: 'Inter', sans-serif;
    }
    .sidebar-link i { font-size: 1.1rem; opacity: 0.7; }
    
    .sidebar-link:hover { color: #000; background: #F8F9FA; }
    
    .sidebar-link.active { 
       background: #1A1A1A; 
       color: #FFFFFF !important;
       box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    .sidebar-link.active i { color: #FFFFFF; opacity: 1; }

    .logout-btn { transition: all 0.2s; cursor: pointer; border-radius: 12px; font-size: 0.9rem; font-family: 'Inter', sans-serif; }
    .logout-btn:hover { background: #FFF5F5; color: #E53E3E !important; }

    @media (max-width: 991px) {
      .sidebar { width: 80px; padding: 1rem !important; }
      .brand-name, .menu-label, .sidebar-link span, .logout-btn span { display: none; }
      .sidebar-link, .logout-btn { justify-content: center; padding: 12px !important; }
      .brand-section { justify-content: center; padding: 0 !important; }
    }
  `]



})
export class SidebarComponent implements OnInit {
  isAdminOrAgent = false;

  constructor(
    private readonly keycloak: KeycloakService,
    private readonly router: Router
  ) { }

  async ngOnInit() {
    try {
      const roles = this.keycloak.getUserRoles();
      this.isAdminOrAgent = roles.includes('ADMIN') || roles.includes('AGENT');
    } catch (e) {
      console.warn('Sidebar: Failed to get roles', e);
    }
  }

  logout() {
    this.keycloak.logout();
  }
}
