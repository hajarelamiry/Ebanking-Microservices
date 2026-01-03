import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar';
import { SidebarComponent } from './components/sidebar/sidebar';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    <div class="app-container d-flex h-100 overflow-hidden">
      <!-- Sidebar is always visible on large screens, or integrated for mobile -->
      <app-sidebar *ngIf="!isLandingPage()" class="d-none d-lg-block"></app-sidebar>
      
      <div class="main-content-wrapper d-flex flex-column flex-grow-1 overflow-hidden">
        <main class="content-area flex-grow-1 overflow-auto" [ngClass]="{'p-0': isLandingPage()}">
          <router-outlet></router-outlet>
        </main>
        
        <footer *ngIf="!isLandingPage()" class="text-center py-4 text-muted extra-small bg-white border-top">
          <div class="d-flex justify-content-center align-items-center gap-2">
            <span class="fw-bold text-dark font-jakarta">Capitalis</span>
            <span class="opacity-50">|</span>
            <span>&copy; 2026 Private Banking Service</span>
            <span class="opacity-50">•</span>
            <span class="text-primary cursor-pointer hover-underline">Terms</span>
            <span class="text-primary cursor-pointer hover-underline">Transparency</span>
          </div>
        </footer>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100vh;
      width: 100vw;
      overflow: hidden;
      font-family: 'Plus Jakarta Sans', sans-serif;
    }
    .app-container {
      background-color: #F8F9FA;
    }
    .content-area {
      background: #F8F9FA; /* Light grey background for the dashboard area to make cards pop */
    }
    .extra-small { font-size: 0.75rem; letter-spacing: 0.5px; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .cursor-pointer { cursor: pointer; }
    .hover-underline:hover { text-decoration: underline; }
  `]

})
export class App {
  constructor(private router: Router) { }

  isLandingPage(): boolean {
    return this.router.url === '/' || this.router.url === '';
  }
}
