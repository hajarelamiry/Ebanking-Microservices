import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink } from '@angular/router';
import { Apollo, gql } from 'apollo-angular';
import { KeycloakService } from 'keycloak-angular';

const GET_ADMIN_STATS = gql`
  query GetAllUsers {
    users {
      keycloakId
      firstName
      lastName
      email
      kycStatus
    }
  }
`;

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink],
  template: `
    <div class="admin-container p-4 min-vh-100 animate-fade-in">
      
      <!-- Top Header -->
      <header class="d-flex justify-content-between align-items-center mb-5 px-3">
        <div>
          <h1 class="fw-bold h2 mb-0 font-jakarta text-dark">System Command</h1>
          <p class="text-muted small mb-0">Management & Operations Control</p>
        </div>
        <div class="d-flex align-items-center gap-3">
          <div class="status-pill glass-card d-flex align-items-center px-3 py-2">
            <span class="dot bg-success me-2 animate-pulse"></span>
            <span class="small fw-bold">Gateway Online</span>
          </div>
          <button class="icon-btn">
            <i class="bi bi-gear-fill text-muted"></i>
          </button>
        </div>
      </header>

      <!-- Main Overview Cards -->
      <div class="row g-4 mb-5">
        <div class="col-md-4">
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white h-100 transition-hover">
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div class="icon-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px; border-radius: 15px;">
                <i class="bi bi-people-fill text-primary fs-4"></i>
              </div>
              <span class="badge bg-light text-dark rounded-pill px-3 py-2 extra-small">+12 This Week</span>
            </div>
            <h6 class="text-muted small fw-bold mb-1">Total Registered Users</h6>
            <h2 class="fw-bold mb-0 font-jakarta">{{ totalUsers }}</h2>
          </div>
        </div>
        
        <div class="col-md-4">
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white h-100 transition-hover border-start border-warning border-5">
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div class="icon-circle bg-warning bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px; border-radius: 15px;">
                <i class="bi bi-shield-exclamation text-warning fs-4"></i>
              </div>
              <button class="btn btn-dark btn-sm rounded-pill px-3 extra-small" routerLink="/admin/kyc">Review Now</button>
            </div>
            <h6 class="text-muted small fw-bold mb-1">Pending KYC Requests</h6>
            <h2 class="fw-bold mb-0 font-jakarta text-warning">{{ pendingKyc }}</h2>
          </div>
        </div>

        <div class="col-md-4">
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-dark text-white h-100 transition-hover">
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div class="icon-circle bg-white bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px; border-radius: 15px;">
                <i class="bi bi-cpu-fill text-white fs-4"></i>
              </div>
              <div class="extra-small px-2 py-1 bg-success rounded-pill">Optimal</div>
            </div>
            <h6 class="text-white text-opacity-50 small fw-bold mb-1">System Health Score</h6>
            <h2 class="fw-bold mb-0 font-jakarta">98%</h2>
          </div>
        </div>
      </div>

      <div class="row g-4">
        <!-- System Logs / Health -->
        <div class="col-lg-8">
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white mb-4">
            <h5 class="fw-bold mb-4">Microservices Status</h5>
            <div class="row g-3">
              <div class="col-md-6 col-lg-4" *ngFor="let svc of microservices">
                <div class="p-3 bg-light rounded-4 d-flex align-items-center justify-content-between">
                  <div class="d-flex align-items-center">
                    <span class="dot me-3" [ngClass]="svc.status === 'UP' ? 'bg-success' : 'bg-danger'"></span>
                    <span class="small fw-bold">{{ svc.name }}</span>
                  </div>
                  <i class="bi bi-check-circle-fill text-success" *ngIf="svc.status === 'UP'"></i>
                </div>
              </div>
            </div>
          </div>

          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0">System Activity</h5>
              <button class="btn btn-link text-dark small fw-bold text-decoration-none">Export Logs</button>
            </div>
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <tbody>
                  <tr *ngFor="let log of [1,2,3,4]">
                    <td class="border-0 px-0">
                      <div class="d-flex align-items-center py-2">
                        <div class="avatar-sm bg-light rounded-circle me-3 d-flex align-items-center justify-content-center">
                          <i class="bi bi-terminal-fill text-muted"></i>
                        </div>
                        <div>
                          <p class="small fw-bold mb-0 text-dark">User.Registration.Success</p>
                          <p class="extra-small text-muted mb-0">Event processed by user-service</p>
                        </div>
                      </div>
                    </td>
                    <td class="border-0 text-muted extra-small text-end">Just now</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Quick Access Sidebar -->
        <div class="col-lg-4">
          <div class="card border-0 rounded-5 p-5 shadow-sm bg-white h-100">
            <h5 class="fw-bold mb-4">Security Tools</h5>
            <div class="d-grid gap-3">
              <button class="btn btn-light rounded-4 py-3 text-start px-4 transition-hover d-flex align-items-center" routerLink="/admin/users">
                <div class="icon-box-sm bg-dark text-white rounded-3 me-3 d-flex align-items-center justify-content-center">
                  <i class="bi bi-people"></i>
                </div>
                <div>
                  <h6 class="mb-0 fw-bold small">User Management</h6>
                  <p class="extra-small text-muted mb-0">CRUD & Role Assignment</p>
                </div>
              </button>
              
              <button class="btn btn-light rounded-4 py-3 text-start px-4 transition-hover d-flex align-items-center" routerLink="/admin/kyc">
                <div class="icon-box-sm bg-primary text-white rounded-3 me-3 d-flex align-items-center justify-content-center">
                  <i class="bi bi-shield-check"></i>
                </div>
                <div>
                  <h6 class="mb-0 fw-bold small">KYC Approvals</h6>
                  <p class="extra-small text-muted mb-0">Review pending identity docs</p>
                </div>
              </button>

              <button class="btn btn-light rounded-4 py-3 text-start px-4 transition-hover d-flex align-items-center">
                <div class="icon-box-sm bg-danger text-white rounded-3 me-3 d-flex align-items-center justify-content-center">
                  <i class="bi bi-lock"></i>
                </div>
                <div>
                  <h6 class="mb-0 fw-bold small">Security Logs</h6>
                  <p class="extra-small text-muted mb-0">Audit trails & Failed logins</p>
                </div>
              </button>
            </div>

            <div class="mt-5 p-4 bg-dark rounded-5 text-white">
              <p class="extra-small fw-bold mb-2">QUICK TIP</p>
              <p class="extra-small opacity-50 mb-0">Weekly KYC review recommended to maintain regulatory compliance.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-container { background-color: #F6F7F9; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .glass-card { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 15px; }
    
    .icon-btn { width: 45px; height: 45px; border-radius: 15px; background: white; border: none; box-shadow: 0 4px 10px rgba(0,0,0,0.03); transition: all 0.3s ease; }
    .icon-btn:hover { box-shadow: 0 10px 20px rgba(0,0,0,0.06); }

    .transition-hover { transition: all 0.3s ease; }
    .transition-hover:hover { box-shadow: 0 15px 30px rgba(0,0,0,0.08) !important; }

    .extra-small { font-size: 0.7rem; }
    .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
    .animate-pulse { animation: pulseSvc 2s infinite; }
    @keyframes pulseSvc { 0% { opacity: 1; } 50% { opacity: 0.4; } 100% { opacity: 1; } }

    .icon-box-sm { width: 40px; height: 40px; }
    .avatar-sm { width: 40px; height: 40px; font-size: 1.2rem; }

    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]

})
export class AdminDashboardComponent implements OnInit {
  totalUsers = 0;
  pendingKyc = 0;
  microservices: any[] = [];

  constructor(
    private apollo: Apollo,
    private keycloak: KeycloakService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.initMicroservices();
    this.loadStats();
  }

  initMicroservices() {
    this.microservices = [
      { name: 'Gateway', status: 'UP' },
      { name: 'User Service', status: 'UP' },
      { name: 'Account Service', status: 'UP' },
      { name: 'Payment Service', status: 'UP' },
      { name: 'Auth Service', status: 'UP' },
      { name: 'Analytics Service', status: 'UP' }
    ];
  }

  loadStats() {
    this.apollo.query({
      query: GET_ADMIN_STATS,
      fetchPolicy: 'network-only'
    }).subscribe({
      next: (result: any) => {
        console.log('Admin Dashboard Stats result:', result);
        const users = result.data.users || [];
        this.totalUsers = users.length;
        this.pendingKyc = users.filter((u: any) => u.kycStatus === 'SUBMITTED').length;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Admin Dashboard: Failed to load stats', err);
        // Fallback for demo
        this.totalUsers = 154;
        this.pendingKyc = 3;
        this.cdr.detectChanges();
      }
    });
  }
}
