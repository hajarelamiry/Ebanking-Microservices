import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Apollo, gql } from 'apollo-angular';

const GET_ALL_USERS = gql`
  query GetAllUsers {
    users {
      keycloakId
      firstName
      lastName
      email
      kycStatus
      kycDocumentUrl
    }
  }
`;

const VALIDATE_KYC = gql`
  mutation ValidateKyc($id: ID!) {
    validateKyc(id: $id) {
      keycloakId
      kycStatus
    }
  }
`;

@Component({
  selector: 'app-admin-kyc',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-kyc-container p-4 min-vh-100 animate-fade-in">
      
      <!-- Header -->
      <header class="d-flex justify-content-between align-items-center mb-5 px-3">
        <div>
          <h1 class="fw-bold h2 mb-1 font-jakarta text-dark">Compliance Center</h1>
          <p class="text-muted small mb-0">Review identity submissions and manage KYC workflows.</p>
        </div>
        <div class="d-flex gap-3">
          <div class="stats-pill glass-card d-flex align-items-center px-3 py-2">
            <i class="bi bi-shield-lock-fill text-warning me-2"></i>
            <span class="small fw-bold text-dark">{{ pendingUsers.length }} Pending Validation</span>
          </div>
          <button (click)="loadPendingUsers()" class="icon-btn-refresh" title="Refresh Queue">
            <i class="bi bi-arrow-clockwise"></i>
          </button>
        </div>
      </header>

      <!-- KYC Queue Table Card -->
      <div class="card border-0 shadow-sm bg-white rounded-5 overflow-hidden">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr class="bg-light text-muted extra-small fw-bold">
                <th class="px-4 py-4 border-0 text-uppercase letter-spacing-1">Client Applicant</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1">Document Verification</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1 text-end px-4">Verification Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of pendingUsers" class="cursor-pointer transition-hover">
                <td class="px-4 py-3">
                  <div class="d-flex align-items-center">
                    <img [src]="'https://ui-avatars.com/api/?name=' + user.firstName + '+' + user.lastName + '&background=FFF5F5&color=C53030'" class="rounded-circle me-3 border border-2 border-white shadow-sm" style="width: 48px; height: 48px;">
                    <div>
                      <h6 class="mb-0 fw-bold text-dark">{{ user.firstName }} {{ user.lastName }}</h6>
                      <p class="extra-small text-muted mb-0 font-monospace">Identity ID: {{ user.keycloakId | slice:0:8 }}...</p>
                    </div>
                  </div>
                </td>
                <td class="py-3">
                  <a [href]="user.kycDocumentUrl" target="_blank" class="document-link px-3 py-2 rounded-4 d-inline-flex align-items-center" *ngIf="user.kycDocumentUrl">
                    <i class="bi bi-file-earmark-pdf-fill fs-5 me-2 text-danger"></i>
                    <div class="d-flex flex-column">
                      <span class="small fw-bold text-dark">Identity_Doc.pdf</span>
                      <span class="extra-small text-muted">Click to inspect</span>
                    </div>
                  </a>
                  <span class="status-badge error" *ngIf="!user.kycDocumentUrl">
                    <i class="bi bi-exclamation-circle-fill me-2"></i> No Document
                  </span>
                </td>
                <td class="text-end px-4 py-3">
                  <div class="d-flex gap-2 justify-content-end">
                    <button class="btn btn-success btn-sm rounded-pill px-4 py-2 fw-bold shadow-sm d-flex align-items-center transition-hover" (click)="validate(user.keycloakId)">
                      <i class="bi bi-check-lg me-2"></i> Approve
                    </button>
                    <button class="btn btn-outline-danger btn-sm rounded-pill px-4 py-2 fw-bold transition-hover">
                      Reject
                    </button>
                  </div>
                </td>
              </tr>
              
              <!-- Empty State -->
              <tr *ngIf="pendingUsers.length === 0">
                <td colspan="3" class="text-center py-5 border-0 bg-white">
                  <div class="py-5">
                    <div class="icon-circle bg-light d-inline-flex align-items-center justify-content-center mb-4" style="width: 80px; height: 80px; border-radius: 25px;">
                      <i class="bi bi-shield-check fs-1 text-success opacity-50"></i>
                    </div>
                    <h5 class="fw-bold text-dark">Queue Empty</h5>
                    <p class="text-muted small">All identity verifications are currently up to date.</p>
                    <button class="btn btn-dark rounded-pill px-4 py-2 mt-3 fw-bold shadow-sm" (click)="loadPendingUsers()">
                      Refresh Queue
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-kyc-container { background-color: #F6F7F9; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .glass-card { background: rgba(255, 255, 255, 0.8); backdrop-filter: blur(15px); border: 1px solid rgba(255, 255, 255, 0.3); border-radius: 15px; }
    
    .letter-spacing-1 { letter-spacing: 0.1rem; }
    .extra-small { font-size: 0.7rem; }
    
    .icon-btn-refresh { width: 45px; height: 45px; border-radius: 15px; background: white; border: none; box-shadow: 0 4px 10px rgba(0,0,0,0.03); color: #4A5568; transition: all 0.3s; }
    .icon-btn-refresh:hover { transform: rotate(180deg); background: #F7FAFC; color: #2D3748; }

    .document-link { background: #F7FAFC; border: 1px solid #E2E8F0; transition: all 0.2s; text-decoration: none; }
    .document-link:hover { background: #EDF2F7; border-color: #CBD5E0; }

    .status-badge.error { display: inline-flex; align-items: center; padding: 6px 16px; border-radius: 50px; font-size: 0.75rem; font-weight: 700; background: #FEF2F2; color: #991B1B; text-transform: uppercase; }

    .transition-hover { transition: all 0.2s ease; }
    .transition-hover:hover { }

    .animate-fade-in { animation: fadeIn 0.6s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class AdminKycComponent implements OnInit {
  pendingUsers: any[] = [];

  constructor(
    private apollo: Apollo,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.loadPendingUsers();
  }

  loadPendingUsers() {
    this.apollo.query({
      query: GET_ALL_USERS,
      fetchPolicy: 'network-only'
    }).subscribe({
      next: (result: any) => {
        console.log('Admin KYC result:', result);
        const users = result.data.users || [];
        // Filter for users who have submitted their KYC
        this.pendingUsers = users.filter((u: any) => u.kycStatus === 'SUBMITTED');
        console.log('Filtered pending KYC users:', this.pendingUsers);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load KYC queue via Apollo', err);
        this.cdr.detectChanges();
      }
    });
  }

  validate(userId: string) {
    this.apollo.mutate({
      mutation: VALIDATE_KYC,
      variables: { id: userId },
      refetchQueries: [{ query: GET_ALL_USERS }]
    }).subscribe({
      next: () => {
        alert('KYC Validated successfully via GraphQL!');
        this.loadPendingUsers();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Validation failed via Apollo', err);
        alert('Failed to validate. Backend resolver might have an issue.');
        this.cdr.detectChanges();
      }
    });
  }
}
