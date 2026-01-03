import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="landing-page d-flex flex-column h-100 bg-white text-dark overflow-hidden position-relative">
      
      <!-- Realistic Floating Background Cards -->
      <div class="floating-bg">
        <div class="float-card card-1"><div class="mini-chip"></div></div>
        <div class="float-card card-2"><div class="mini-chip"></div></div>
        <div class="float-card card-3"><div class="mini-chip"></div></div>
        <div class="float-card card-4"><div class="mini-chip"></div></div>
        <div class="float-card card-5"><div class="mini-chip"></div></div>
      </div>

      <!-- Main Content -->
      <div class="container-fluid flex-grow-1 d-flex flex-column justify-content-center position-relative z-index-2">
        
        <!-- Center Visual Section (Main Card) -->
        <div class="visual-container position-absolute top-50 start-50 translate-middle w-100 h-100 d-flex justify-content-center align-items-center">
          <div class="main-card-stack">
            <div class="bank-card card-noir shadow-lg float-anim">
               <div class="card-chip"></div>
               <div class="card-details">
                  <div class="small-text opacity-50">Ghulam Rasool</div>
                  <div class="card-num">4562 1122 4595 7852</div>
               </div>
            </div>
          </div>
        </div>

        <!-- Text Box Section -->
        <div class="row w-100 position-relative">
          <div class="col-lg-5 offset-lg-1">
             <div class="content-box bg-white text-dark p-lg-5 p-4 rounded-5 border shadow-sm mx-3 mx-lg-0">
                <div class="dot-indicator mb-4">
                  <span class="active"></span>
                  <span></span>
                  <span></span>
                </div>
                <h1 class="display-4 fw-bold mb-4 font-jakarta">Take control of your finances just your phone</h1>
                <p class="text-muted mb-5 fs-5">Convenience to control and manage your finances in one place to save your time.</p>
                
                <button (click)="getStarted()" 
                   class="btn btn-get-started d-flex align-items-center justify-content-between shadow-sm border-0 w-100 text-decoration-none">
                   <span class="fw-bold px-4 text-dark">{{ isLoggedIn ? 'Go to Dashboard' : 'Get started' }}</span>
                   <div class="arrow-circle bg-dark text-white rounded-circle d-flex align-items-center justify-content-center">
                      <i class="bi bi-arrow-right fs-4"></i>
                   </div>
                </button>
             </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .landing-page { background: #F4F4F9; min-height: 100vh; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .z-index-2 { z-index: 2; }
    
    .floating-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 1; overflow: hidden; pointer-events: none; }
    .float-card { position: absolute; background: rgba(0, 0, 0, 0.05); border-radius: 20px; padding: 15px; display: flex; flex-direction: column; justify-content: flex-start; border: 1px solid rgba(0,0,0,0.05); pointer-events: none; }
    .mini-chip { width: 25px; height: 18px; background: rgba(0,0,0,0.05); border-radius: 4px; }
    
    .visual-container { pointer-events: none; z-index: 1; }
    .content-box { position: relative; z-index: 10; pointer-events: all; }
    .card-1 { width: 180px; height: 280px; top: 10%; right: 15%; transform: rotate(15deg); animation-duration: 22s; }
    .card-2 { width: 200px; height: 310px; bottom: 10%; left: 8%; transform: rotate(-25deg); animation-duration: 28s; opacity: 0.6; }
    .card-3 { width: 140px; height: 210px; top: 20%; left: 12%; transform: rotate(45deg); animation-duration: 19s; opacity: 0.3; }
    .card-4 { width: 220px; height: 340px; bottom: 20%; right: 5%; transform: rotate(-10deg); animation-duration: 32s; opacity: 0.4; }
    .card-5 { width: 160px; height: 250px; top: -5%; left: 40%; transform: rotate(10deg); animation-duration: 24s; opacity: 0.2; }

    @keyframes float-around {
      0%, 100% { transform: translate(0, 0) rotate(15deg) scale(1); }
      33% { transform: translate(40px, -60px) rotate(10deg) scale(1.05); }
      66% { transform: translate(-30px, 50px) rotate(20deg) scale(0.95); }
    }

    .main-card-stack { position: relative; }
    .bank-card { width: 320px; height: 480px; border-radius: 40px; background: #000; color: white; padding: 40px; display: flex; flex-direction: column; justify-content: space-between; border: 1px solid rgba(255,255,255,0.1); }
    .float-anim { }
    @keyframes main-float { 0%, 100% { transform: translateY(0) rotate(-10deg); } 50% { transform: translateY(-35px) rotate(-8deg); } }
    .card-chip { width: 45px; height: 35px; background: rgba(255,255,255,0.15); border-radius: 8px; align-self: flex-end; }
    .card-num { font-size: 1.2rem; letter-spacing: 1px; margin-top: 20px; font-weight: 600; }
    .small-text { font-size: 0.7rem; text-transform: uppercase; font-weight: bold; }

    .dot-indicator .active { width: 25px; height: 6px; background: #000; display: inline-block; border-radius: 3px; margin-right: 5px; }
    .dot-indicator span:not(.active) { width: 6px; height: 6px; background: #CCC; display: inline-block; border-radius: 50%; margin-right: 5px; }

    .btn-get-started { background: #EBEBF2; border-radius: 50px; padding: 10px; cursor: pointer; transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); width: fit-content; min-width: 280px; }
    .btn-get-started:hover { background: #D9D9E3; }
    .arrow-circle { width: 55px; height: 55px; box-shadow: 0 10px 20px rgba(0,0,0,0.2); }

    @media (max-width: 991px) {
      .bank-card { width: 220px; height: 330px; padding: 25px; }
      .floating-bg { display: none; }
    }
  `]
})
export class LandingComponent implements OnInit {
  isLoggedIn = false;

  constructor(private keycloak: KeycloakService, private router: Router) { }

  async ngOnInit() {
    try {
      this.isLoggedIn = await this.keycloak.isLoggedIn();
      if (this.isLoggedIn) {
        this.router.navigate(['/dashboard']);
      }
    } catch (e) {
      console.error('Auth check failed', e);
    }
  }

  async getStarted() {
    if (this.isLoggedIn) {
      const roles = this.keycloak.getUserRoles();
      if (roles.includes('CLIENT') || roles.includes('ADMIN') || roles.includes('AGENT')) {
        this.router.navigate(['/dashboard']);
      } else {
        alert('Your account is active but does not have the required roles (CLIENT, ADMIN, or AGENT) to access the dashboard.');
      }
      return;
    }

    try {
      await this.keycloak.login({
        redirectUri: window.location.origin + '/dashboard'
      });
    } catch (error) {
      console.error('Login failed', error);
    }
  }
}
