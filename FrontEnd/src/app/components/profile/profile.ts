import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Apollo, gql } from 'apollo-angular';
import { FormsModule } from '@angular/forms';

const GET_PROFILE = gql`
  query GetProfile {
    me {
      profile {
        firstName
        lastName
        email
        phoneNumber
        address
        kycStatus
      }
    }
  }
`;

const UPDATE_PROFILE = gql`
  mutation UpdateProfile($input: ProfileInput!) {
    updateProfile(input: $input) {
      firstName
      lastName
      phoneNumber
      address
    }
  }
`;

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="profile-card p-4 animate-fade-in" *ngIf="profile">
      <header class="mb-5 px-3">
        <h1 class="fw-bold h2 mb-1 font-jakarta">My Profile</h1>
        <p class="text-muted small">Manage your personal information and account preferences</p>
      </header>

      <div class="row justify-content-center">
        <div class="col-lg-10">
          <div class="card p-5 border-0 shadow-sm bg-white rounded-5">
            <div class="d-flex align-items-center mb-5 pb-4 border-bottom">
              <div class="position-relative">
                <img [src]="'https://ui-avatars.com/api/?name=' + profile.firstName + '+' + profile.lastName + '&background=000&color=fff&size=256'" class="rounded-5 shadow-sm" style="width: 120px; height: 120px; object-fit: cover;">
                <button class="btn btn-dark rounded-circle position-absolute bottom-0 end-0 p-2 shadow-sm d-flex align-items-center justify-content-center" style="width: 35px; height: 35px; margin: -5px">
                  <i class="bi bi-camera-fill small"></i>
                </button>
              </div>
              <div class="ms-4">
                <h2 class="fw-bold mb-1 text-dark font-jakarta">{{ profile.firstName }} {{ profile.lastName }}</h2>
                <div class="d-flex align-items-center gap-2">
                  <span class="badge bg-light text-dark rounded-pill px-3 py-2 extra-small">
                    <i class="bi bi-patch-check-fill text-primary me-1"></i> {{ profile.kycStatus }}
                  </span>
                  <span class="text-muted extra-small">Member since Jan 2026</span>
                </div>
              </div>
            </div>

            <form (ngSubmit)="saveProfile()">
              <div class="row g-4 mb-5">
                <div class="col-md-6">
                  <label class="form-label">First Name</label>
                  <input type="text" class="form-control premium-input" [(ngModel)]="profile.firstName" name="fn">
                </div>
                <div class="col-md-6">
                  <label class="form-label">Last Name</label>
                  <input type="text" class="form-control premium-input" [(ngModel)]="profile.lastName" name="ln">
                </div>
                <div class="col-md-8">
                  <label class="form-label">Email Address</label>
                  <input type="email" class="form-control premium-input opacity-75" [value]="profile.email" disabled name="em">
                </div>
                <div class="col-md-4">
                  <label class="form-label">Phone Number</label>
                  <input type="tel" class="form-control premium-input" [(ngModel)]="profile.phoneNumber" name="ph">
                </div>
                <div class="col-12">
                  <label class="form-label">Home Address</label>
                  <textarea class="form-control premium-input" style="min-height: 100px" [(ngModel)]="profile.address" name="ad"></textarea>
                </div>
              </div>

              <div class="d-flex justify-content-end gap-3 pt-4">
                <div *ngIf="saving" class="spinner-border text-dark me-2" role="status"></div>
                <button type="button" class="btn btn-light rounded-pill px-5 py-3 fw-bold border" (click)="ngOnInit()">Reset</button>
                <button type="submit" class="btn btn-dark rounded-pill px-5 py-3 fw-bold shadow-lg" [disabled]="saving">Save Changes</button>
              </div>

              <div *ngIf="successMessage" class="alert alert-success mt-4 rounded-4 border-0 shadow-sm">
                 <i class="bi bi-check-circle-fill me-2"></i> {{ successMessage }}
              </div>
            </form>
          </div>

          <!-- New Section: Rewards & Referral -->
          <div class="row g-4 mt-2">
            <div class="col-md-6">
              <div class="card border-0 rounded-5 p-4 shadow-sm bg-dark text-white h-100">
                <div class="d-flex justify-content-between align-items-start mb-4">
                  <div class="icon-box-sm bg-primary rounded-4 d-flex align-items-center justify-content-center">
                    <i class="bi bi-gift-fill text-white"></i>
                  </div>
                  <span class="badge bg-primary rounded-pill px-3 py-2 small">Level 1</span>
                </div>
                <h5 class="fw-bold mb-2 font-jakarta">Sponsorship Program</h5>
                <p class="extra-small opacity-50 mb-4">Invite your friends and earn 250 MAD for each successful validation.</p>
                <div class="p-3 bg-white bg-opacity-10 rounded-4 d-flex justify-content-between align-items-center mb-0">
                  <code class="text-white fw-bold">CAPITALIS-{{ profile.lastName | uppercase }}-2026</code>
                  <button class="btn btn-link text-white p-0"><i class="bi bi-copy"></i></button>
                </div>
              </div>
            </div>

            <div class="col-md-6">
              <div class="card border-0 rounded-5 p-4 shadow-sm bg-white h-100">
                <div class="d-flex justify-content-between align-items-start mb-4">
                  <div class="icon-box-sm bg-light rounded-4 d-flex align-items-center justify-content-center">
                    <i class="bi bi-stars text-primary"></i>
                  </div>
                  <div class="text-end">
                    <h3 class="fw-bold mb-0 font-jakarta">1,250</h3>
                    <span class="extra-small text-muted fw-bold">Points</span>
                  </div>
                </div>
                <h5 class="fw-bold mb-2 font-jakarta">Capitalis Rewards</h5>
                <p class="extra-small text-muted mb-4">You are close to redeeming a free month of Premium Insurance.</p>
                <div class="progress rounded-pill mb-0" style="height: 6px;">
                  <div class="progress-bar bg-primary rounded-pill" style="width: 75%"></div>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .profile-card { background-color: #F6F7F9; min-height: 100vh; font-family: 'Plus Jakarta Sans', sans-serif; padding-bottom: 100px !important; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .premium-input {
      background-color: #F8F9FA !important;
      border: 2px solid transparent !important;
      border-radius: 16px !important;
      padding: 12px 20px !important;
      font-size: 1rem !important;
      font-weight: 600;
      transition: all 0.2s ease;
      font-family: 'Inter', sans-serif;
    }
    .premium-input:focus {
      background-color: white !important;
      border-color: #1A1A1A !important;
      box-shadow: 0 10px 20px rgba(0,0,0,0.05) !important;
    }
    .form-label { 
      font-size: 0.7rem; 
      font-weight: 800; 
      text-transform: uppercase; 
      letter-spacing: 1.5px; 
      color: #ADB5BD;
      margin-left: 10px;
      margin-bottom: 8px;
    }
    .btn-dark:hover { transform: translateY(-3px); box-shadow: 0 15px 30px rgba(0,0,0,0.2) !important; }
    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProfileComponent implements OnInit {
  profile: any = null;
  saving = false;
  successMessage = '';

  constructor(private apollo: Apollo) { }

  ngOnInit() {
    this.apollo.query<any>({
      query: GET_PROFILE,
      fetchPolicy: 'network-only'
    }).subscribe(({ data }) => {
      this.profile = { ...data.me.profile };
    });
  }

  saveProfile() {
    this.saving = true;
    this.successMessage = '';

    const input = {
      firstName: this.profile.firstName,
      lastName: this.profile.lastName,
      phoneNumber: this.profile.phoneNumber,
      address: this.profile.address
    };

    this.apollo.mutate({
      mutation: UPDATE_PROFILE,
      variables: { input },
      refetchQueries: [{ query: GET_PROFILE }]
    }).subscribe({
      next: (result) => {
        this.saving = false;
        this.successMessage = 'Profile updated successfully!';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.saving = false;
        console.error('Mutation error:', err);
        alert('Failed to update profile. Please check if services are running.');
      }
    });
  }
}
