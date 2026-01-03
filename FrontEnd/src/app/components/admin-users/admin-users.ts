import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Apollo, gql } from 'apollo-angular';
import { KeycloakService } from 'keycloak-angular';

const GET_ALL_USERS = gql`
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

const GET_PENDING_REQUESTS = gql`
  query GetPendingRequests {
    pendingRequests {
      id
      firstName
      lastName
      email
      status
      agentId
      createdAt
    }
  }
`;

const PROCESS_REQUEST = gql`
  mutation ProcessUserCreation($id: ID!, $status: RequestStatus!, $reason: String) {
    processUserCreation(id: $id, status: $status, reason: $reason) {
      id
      status
    }
  }
`;

const REQUEST_USER_CREATION = gql`
  mutation RequestUserCreation($input: UserRequestInput!) {
    requestUserCreation(input: $input) {
      id
      status
    }
  }
`;

const ONBOARD_USER = gql`
  mutation OnboardUser($input: UserRequestInput!) {
    onboardUser(input: $input) {
      keycloakId
      email
    }
  }
`;

const DELETE_USER = gql`
  mutation DeleteUser($id: ID!) {
    deleteUser(id: $id)
  }
`;

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-users-container p-4 min-vh-100 animate-fade-in">
      
      <!-- Header -->
      <header class="d-flex justify-content-between align-items-center mb-5 px-3">
        <div>
          <h1 class="fw-bold h2 mb-1 font-jakarta text-dark">Identity Management</h1>
          <p class="text-muted small mb-0">Manage customer records and creation requests.</p>
        </div>
        <div class="d-flex gap-3">
          <div class="stats-pill glass-card d-flex align-items-center px-3 py-2">
            <i class="bi bi-people-fill text-primary me-2"></i>
            <span class="small fw-bold">{{ users.length }} Clients</span>
          </div>
          <button class="btn btn-dark rounded-pill px-4 py-2 fw-bold shadow-sm d-flex align-items-center transition-hover" (click)="showAddModal = true">
            <i class="bi bi-person-plus-fill me-2"></i> {{ isAdmin ? 'Add New User' : 'Request Creation' }}
          </button>
        </div>
      </header>

      <!-- Tabs for Admin -->
      <div class="nav-tabs-premium mb-4 px-3 d-flex gap-4" *ngIf="isAdmin">
        <button [class.active]="activeTab === 'users'" (click)="activeTab = 'users'">Active Clients</button>
        <button [class.active]="activeTab === 'requests'" (click)="activeTab = 'requests'" class="position-relative">
          Pending Requests
          <span class="badge rounded-pill bg-danger ms-2 extra-small" *ngIf="pendingRequests.length > 0">{{ pendingRequests.length }}</span>
        </button>
      </div>

      <!-- Users Table -->
      <div class="card border-0 shadow-sm bg-white rounded-5 overflow-hidden" *ngIf="activeTab === 'users'">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr class="bg-light text-muted extra-small fw-bold">
                <th class="px-4 py-4 border-0 text-uppercase letter-spacing-1">Client Profile</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1">Contact Info</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1">Compliance Status</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1 text-end px-4">Management</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of users" class="cursor-pointer transition-hover">
                <td class="px-4 py-3">
                  <div class="d-flex align-items-center">
                    <div class="avatar-wrapper position-relative me-3">
                      <img [src]="'https://ui-avatars.com/api/?name=' + user.firstName + '+' + user.lastName + '&background=F0F4F8&color=1A202C'" class="rounded-circle shadow-sm" style="width: 48px; height: 48px;">
                      <span class="status-indicator bg-success" *ngIf="user.kycStatus === 'VALIDATED'"></span>
                    </div>
                    <div>
                      <h6 class="mb-0 fw-bold text-dark">{{ user.firstName }} {{ user.lastName }}</h6>
                      <p class="extra-small text-muted mb-0 font-monospace">UUID: {{ user.keycloakId | slice:0:8 }}...</p>
                    </div>
                  </div>
                </td>
                <td class="py-3">
                  <div class="d-flex flex-column">
                    <span class="small fw-bold text-dark">{{ user.email }}</span>
                    <span class="extra-small text-muted">Verified Account</span>
                  </div>
                </td>
                <td class="py-3">
                  <span class="status-badge" [ngClass]="{
                    'validated': user.kycStatus === 'VALIDATED',
                    'pending': user.kycStatus === 'SUBMITTED' || user.kycStatus === 'PENDING',
                    'rejected': user.kycStatus === 'REJECTED'
                  }">
                    <i class="bi me-2" [ngClass]="{
                      'bi-patch-check-fill': user.kycStatus === 'VALIDATED',
                      'bi-clock-fill': user.kycStatus === 'SUBMITTED' || user.kycStatus === 'PENDING',
                      'bi-x-circle-fill': user.kycStatus === 'REJECTED'
                    }"></i>
                    {{ user.kycStatus }}
                  </span>
                </td>
                <td class="text-end px-4 py-3">
                  <div class="d-flex gap-2 justify-content-end">
                    <button class="action-btn" title="Edit Profile"><i class="bi bi-pencil-square"></i></button>
                    <button class="action-btn delete" title="Delete User" (click)="deleteUser(user.keycloakId)">
                      <i class="bi bi-trash3-fill"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- User Requests Table (Admin Only) -->
      <div class="card border-0 shadow-sm bg-white rounded-5 overflow-hidden shadow-lg animate-fade-in" *ngIf="activeTab === 'requests' && isAdmin">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr class="bg-dark text-white extra-small fw-bold">
                <th class="px-4 py-4 border-0 text-uppercase letter-spacing-1">Requested Identity</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1">Request Info</th>
                <th class="py-4 border-0 text-uppercase letter-spacing-1 text-end px-4">Decision Hub</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let req of pendingRequests" class="transition-hover border-bottom">
                <td class="px-4 py-4">
                  <div class="d-flex align-items-center">
                    <div class="icon-box-md bg-light rounded-circle me-3 d-flex align-items-center justify-content-center">
                      <i class="bi bi-person-plus text-primary fs-4"></i>
                    </div>
                    <div>
                      <h6 class="mb-0 fw-bold text-dark">{{ req.firstName }} {{ req.lastName }}</h6>
                      <p class="extra-small text-primary mb-0 fw-bold">{{ req.email }}</p>
                    </div>
                  </div>
                </td>
                <td class="py-4">
                  <div class="d-flex flex-column">
                    <span class="small fw-bold text-dark"><i class="bi bi-shield-lock me-2"></i> Agent: {{ req.agentId | slice:0:8 }}</span>
                    <span class="extra-small text-muted">{{ req.createdAt | date:'medium' }}</span>
                  </div>
                </td>
                <td class="text-end px-4 py-4">
                  <div class="d-flex gap-3 justify-content-end">
                    <button class="btn btn-outline-danger btn-sm rounded-pill px-3 py-1 fw-bold" (click)="processRequest(req.id, 'REJECTED')">
                      Reject
                    </button>
                    <button class="btn btn-success btn-sm rounded-pill px-4 py-1 fw-bold shadow-sm" [disabled]="processingId === req.id" (click)="processRequest(req.id, 'APPROVED')">
                      <span *ngIf="processingId !== req.id">Approve & Notify</span>
                      <span *ngIf="processingId === req.id" class="spinner-border spinner-border-sm me-2"></span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="pendingRequests.length === 0">
                <td colspan="3" class="text-center py-5">
                   <i class="bi bi-inbox fs-1 text-muted opacity-25 d-block mb-3"></i>
                   <p class="text-muted fw-medium">No pending user creation requests.</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Modern Request Modal (Agent/Admin) -->
      <div class="modal-overlay" *ngIf="showAddModal" (click)="showAddModal = false">
         <div class="modal-card glass-card p-5 rounded-5 shadow-2xl animate-modal-in" (click)="$event.stopPropagation()">
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div>
                <h3 class="fw-bold mb-1 font-jakarta">{{ isAdmin ? 'Direct User Onboarding' : 'Account Opening Request' }}</h3>
                <p class="text-muted small">{{ isAdmin ? 'Instantly create Keycloak ID, Bank Profile and Notify User.' : 'Submit a request to Admin for account provisioning.' }}</p>
              </div>
              <button class="btn-close" (click)="showAddModal = false"></button>
            </div>

            <div class="info-alert mb-4">
               <i class="bi bi-info-circle-fill me-3"></i>
               <span>{{ isAdmin ? 'This action sends credentials via Email immediately.' : 'Admins will review the request and trigger Keycloak creation & email notification.' }}</span>
            </div>

            <div class="row g-4 mb-4">
               <div class="col-md-6">
                  <label class="extra-small fw-bold text-muted mb-2 text-uppercase">First Name</label>
                  <input type="text" class="form-control glass-input py-3" #fn>
               </div>
               <div class="col-md-6">
                  <label class="extra-small fw-bold text-muted mb-2 text-uppercase">Last Name</label>
                  <input type="text" class="form-control glass-input py-3" #ln>
               </div>
            </div>

            <div class="form-group mb-5">
               <label class="extra-small fw-bold text-muted mb-2 text-uppercase">Client Email Address</label>
               <input type="email" class="form-control glass-input py-3" #em placeholder="client@email.com">
            </div>

            <div class="d-flex gap-3">
               <button class="btn btn-light rounded-pill flex-grow-1 py-3 fw-bold shadow-sm" (click)="showAddModal = false">Cancel</button>
               <button class="btn btn-dark rounded-pill flex-grow-1 py-3 fw-bold shadow-lg" [disabled]="isSubmitting" (click)="submitRequest(fn.value, ln.value, em.value)">
                 <span *ngIf="!isSubmitting">{{ isAdmin ? 'Onboard & Notify' : 'Submit Request' }}</span>
                 <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
               </button>
            </div>
         </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-users-container { background-color: #F6F7F9; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .glass-card { background: rgba(255, 255, 255, 0.8); backdrop-filter: blur(15px); border: 1px solid rgba(255, 255, 255, 0.3); }
    
    .nav-tabs-premium button { background: none; border: none; padding: 10px 0; color: #ADB5BD; font-weight: 700; font-size: 0.9rem; transition: all 0.3s; border-bottom: 2px solid transparent; }
    .nav-tabs-premium button.active { color: #1A1A1A; border-bottom-color: #1A1A1A; }
    
    .letter-spacing-1 { letter-spacing: 0.1rem; }
    .extra-small { font-size: 0.7rem; }
    .icon-box-md { width: 48px; height: 48px; border: 1px solid #EEE; }
    
    /* Status Badge Styling */
    .status-badge { display: inline-flex; align-items: center; padding: 6px 16px; border-radius: 50px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
    .status-badge.validated { background: #E6FFFA; color: #047481; }
    .status-badge.pending { background: #FFFBEB; color: #92400E; }
    .status-badge.rejected { background: #FEF2F2; color: #991B1B; }

    .status-indicator { position: absolute; bottom: 2px; right: 2px; width: 12px; height: 12px; border-radius: 50%; border: 2px solid white; }
    
    /* Action Buttons */
    .action-btn { width: 38px; height: 38px; border-radius: 12px; border: none; background: #F7FAFC; color: #4A5568; transition: all 0.2s; }
    .action-btn:hover { background: #EDF2F7; }
    .action-btn.delete { color: #E53E3E; }
    .action-btn.delete:hover { background: #FFF5F5; }

    /* Modal Styling */
    .modal-overlay { position: fixed; top:0; left:0; width:100%; height:100%; background:rgba(26, 32, 44, 0.4); z-index:1000; display:flex; align-items:center; justify-content:center; backdrop-filter: blur(8px); }
    .modal-card { width:100%; max-width:550px; }
    .animate-modal-in { animation: modalIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
    @keyframes modalIn { from { transform: scale(0.9) translateY(20px); opacity:0; } to { transform: scale(1) translateY(0); opacity:1; } }

    .info-alert { background: #EBF8FF; color: #2B6CB0; padding: 15px; border-radius: 15px; display: flex; align-items: center; font-size: 0.85rem; }
    .glass-input { background: #F8FAFC !important; border: 1px solid #E2E8F0 !important; border-radius: 15px !important; }
    .glass-input:focus-within { border-color: #3182CE !important; box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.1) !important; }

    .transition-hover { transition: all 0.2s ease; }
    .transition-hover:hover { box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); }

    .animate-fade-in { animation: fadeIn 0.6s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]

})
export class AdminUsersComponent implements OnInit {
  users: any[] = [];
  pendingRequests: any[] = [];
  showAddModal = false;
  activeTab: 'users' | 'requests' = 'users';
  isSubmitting = false;
  processingId: number | null = null;
  isAdmin = false;
  isAgent = false;

  constructor(
    private apollo: Apollo,
    private cdr: ChangeDetectorRef,
    private keycloak: KeycloakService
  ) { }

  ngOnInit() {
    this.checkRoles();
    this.loadUsers();
    if (this.isAdmin) {
      this.loadRequests();
    }
  }

  checkRoles() {
    const roles = this.keycloak.getUserRoles();
    this.isAdmin = roles.includes('ADMIN');
    this.isAgent = roles.includes('AGENT');
    if (this.isAgent && !this.isAdmin) {
      this.activeTab = 'users'; // Agents only see users (or eventually their own requests)
    }
  }

  loadUsers() {
    this.apollo.query({
      query: GET_ALL_USERS,
      fetchPolicy: 'network-only'
    }).subscribe({
      next: (result: any) => {
        this.users = result.data.users;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load users', err)
    });
  }

  loadRequests() {
    this.apollo.query({
      query: GET_PENDING_REQUESTS,
      fetchPolicy: 'network-only'
    }).subscribe({
      next: (result: any) => {
        this.pendingRequests = result.data.pendingRequests;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load pending requests', err)
    });
  }

  submitRequest(firstName: string, lastName: string, email: string) {
    if (!firstName || !lastName || !email) {
      alert('Please fill all fields');
      return;
    }
    this.isSubmitting = true;

    const mutation = this.isAdmin ? ONBOARD_USER : REQUEST_USER_CREATION;
    const successMsg = this.isAdmin ? 'User onboarded successfully! (Keycloak created & Email sent)' : 'Request submitted to administration.';

    this.apollo.mutate({
      mutation: mutation,
      variables: { input: { firstName, lastName, email } }
    }).subscribe({
      next: () => {
        alert(successMsg);
        this.showAddModal = false;
        this.isSubmitting = false;
        this.loadUsers();
        this.cdr.detectChanges();
      },
      error: (err) => {
        alert('Operation failed: ' + err.message);
        this.isSubmitting = false;
        this.cdr.detectChanges();
      }
    });
  }

  processRequest(id: number, status: 'APPROVED' | 'REJECTED') {
    if (status === 'REJECTED' && !confirm('Are you sure you want to reject this request?')) return;

    this.processingId = id;
    this.apollo.mutate({
      mutation: PROCESS_REQUEST,
      variables: { id, status }
    }).subscribe({
      next: () => {
        const msg = status === 'APPROVED' ? 'User created & Email notification sent!' : 'Request rejected.';
        alert(msg);
        this.processingId = null;
        this.loadUsers();
        this.loadRequests();
        this.cdr.detectChanges();
      },
      error: (err) => {
        alert('Processing failed: ' + err.message);
        this.processingId = null;
        this.cdr.detectChanges();
      }
    });
  }

  deleteUser(id: string) {
    if (!confirm('Permanently delete this user profile?')) return;
    this.apollo.mutate({
      mutation: DELETE_USER,
      variables: { id },
      refetchQueries: [{ query: GET_ALL_USERS }]
    }).subscribe({
      next: () => {
        this.loadUsers();
        this.cdr.detectChanges();
      },
      error: (err) => alert('Delete failed: ' + err.message)
    });
  }
}
