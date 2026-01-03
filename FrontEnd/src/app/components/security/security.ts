import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-security',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="security-vault-container p-4 animate-fade-in">
      <header class="mb-5 px-3">
        <h1 class="fw-bold h2 mb-1 font-jakarta">Security Vault</h1>
        <p class="text-muted small">Manage your account protection and authentication methods</p>
      </header>

      <div class="row g-4 px-3">
        <!-- Main Security Status -->
        <div class="col-lg-8">
          <div class="card border-0 shadow-sm rounded-5 bg-white p-5 h-100">
            <div class="d-flex align-items-center mb-5 pb-4 border-bottom">
              <div class="icon-pulse me-4">
                <div class="icon-square bg-success rounded-circle d-flex align-items-center justify-content-center text-white" style="width: 70px; height: 70px;">
                  <i class="bi bi-shield-check fs-2"></i>
                </div>
              </div>
              <div>
                <h3 class="fw-bold mb-1 font-jakarta">Account Protected</h3>
                <p class="text-success mb-0 small fw-bold text-uppercase d-flex align-items-center gap-2">
                  <span class="dot bg-success animate-pulse"></span> Identity Provider active
                </p>
              </div>
            </div>

            <div class="security-options d-grid gap-4 mb-5">
              <div class="p-4 bg-light rounded-5 d-flex justify-content-between align-items-center transition-hover">
                <div class="d-flex align-items-center">
                  <div class="icon-box-sm bg-white rounded-4 me-3 d-flex align-items-center justify-content-center shadow-sm">
                    <i class="bi bi-person-badge-fill text-dark"></i>
                  </div>
                  <div>
                    <span class="d-block fw-bold small font-jakarta text-dark">Verified Identity</span>
                    <span class="text-muted extra-small">Account linked to {{ username }}</span>
                  </div>
                </div>
                <span class="badge bg-dark rounded-pill px-3 py-2 extra-small">Enabled</span>
              </div>

              <div class="p-4 bg-light rounded-5 d-flex justify-content-between align-items-center transition-hover">
                <div class="d-flex align-items-center">
                  <div class="icon-box-sm bg-white rounded-4 me-3 d-flex align-items-center justify-content-center shadow-sm">
                    <i class="bi bi-key-fill text-dark"></i>
                  </div>
                  <div>
                    <span class="d-block fw-bold small font-jakarta text-dark">Multi-Factor Authentication</span>
                    <span class="text-muted extra-small">Enforced at realm level</span>
                  </div>
                </div>
                <i class="bi bi-check-circle-fill text-success fs-4"></i>
              </div>
            </div>

            <div class="alert bg-dark bg-opacity-5 border-0 rounded-4 p-4 d-flex align-items-start mt-auto">
              <i class="bi bi-info-circle-fill text-dark me-3 fs-3"></i>
              <div>
                <h6 class="fw-bold mb-1 font-jakarta">Security Notice</h6>
                <p class="extra-small text-muted mb-0">For your protection, password changes and MFA management are handled via the central security server. Click below to manage your vault credentials.</p>
              </div>
            </div>
            
            <a [href]="accountUrl" target="_blank" class="btn btn-dark w-100 rounded-pill py-3 fw-bold shadow-lg mt-4 text-decoration-none d-flex align-items-center justify-content-center">
              Open Security Console <i class="bi bi-box-arrow-up-right ms-2 small"></i>
            </a>
          </div>
        </div>

        <!-- Security Analytics/Tips -->
        <div class="col-lg-4">
          <div class="card border-0 rounded-5 bg-dark text-white p-5 shadow-lg h-100">
            <h5 class="fw-bold mb-4 font-jakarta">Safety Score</h5>
            <div class="score-visualization text-center mb-5">
               <div class="d-inline-flex position-relative">
                  <svg width="120" height="120">
                    <circle cx="60" cy="60" r="54" stroke="rgba(255,255,255,0.1)" stroke-width="12" fill="none" />
                    <circle cx="60" cy="60" r="54" stroke="white" stroke-width="12" fill="none" stroke-dasharray="340" stroke-dashoffset="34" />
                  </svg>
                  <div class="position-absolute top-50 start-50 translate-middle">
                    <span class="h1 fw-bold mb-0">90</span>
                  </div>
               </div>
               <p class="extra-small opacity-50 mt-3 text-uppercase fw-bold">Excellent Protection</p>
            </div>

            <div class="security-tips">
              <h6 class="small fw-bold mb-3 opacity-50 text-uppercase">Security Tips</h6>
              <ul class="list-unstyled d-grid gap-3">
                <li class="d-flex align-items-start gap-2">
                  <i class="bi bi-check2-circle text-success"></i>
                  <span class="extra-small opacity-75">Enable biometric login on mobile app</span>
                </li>
                <li class="d-flex align-items-start gap-2">
                  <i class="bi bi-check2-circle text-success"></i>
                  <span class="extra-small opacity-75">Check recognized devices monthly</span>
                </li>
                <li class="d-flex align-items-start gap-2">
                  <i class="bi bi-check2-circle text-success"></i>
                  <span class="extra-small opacity-75">Never share your recovery codes</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .security-vault-container { background-color: #F6F7F9; min-height: 100vh; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .icon-square { box-shadow: 0 10px 20px rgba(25, 135, 84, 0.2); }
    .extra-small { font-size: 0.75rem; }
    .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    .animate-pulse { animation: pulseSvc 2s infinite; }
    @keyframes pulseSvc { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }
    .icon-box-sm { width: 45px; height: 45px; font-size: 1.2rem; }
    .transition-hover { transition: all 0.2s ease; cursor: pointer; border: 1px solid transparent; }
    .transition-hover:hover { border-color: #DDD; background-color: #FFF !important; box-shadow: 0 10px 20px rgba(0,0,0,0.03); }
    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class SecurityComponent implements OnInit {
  username = '';
  accountUrl = 'http://localhost:8080/realms/ebanking-realm/account';

  constructor(private keycloak: KeycloakService) { }

  ngOnInit() {
    try {
      this.username = this.keycloak.getUsername();
    } catch (e) {
      this.username = 'Authenticated User';
    }
  }
}
