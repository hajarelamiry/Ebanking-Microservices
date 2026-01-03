import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Apollo, gql } from 'apollo-angular';
import { Router } from '@angular/router';

const SUBMIT_KYC = gql`
  mutation SubmitKyc($documentUrl: String!) {
    submitKyc(documentUrl: $documentUrl) {
      kycStatus
    }
  }
`;

const GET_KYC_STATUS = gql`
  query GetKycStatus {
    me {
      profile {
        kycStatus
      }
    }
  }
`;

@Component({
  selector: 'app-kyc',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="kyc-container p-4 animate-fade-in">
      <header class="mb-5 px-3">
        <h1 class="fw-bold h2 mb-1 font-jakarta">Identity Verification</h1>
        <p class="text-muted small">Complete your KYC to unlock full account features</p>
      </header>

      <div class="row justify-content-center">
        <div class="col-lg-6">
          <div class="card p-5 border-0 shadow-sm bg-white rounded-5 text-center">
            
            <div class="d-flex justify-content-center gap-2 mb-4">
              <div class="step active shadow-sm"></div>
              <div class="step active shadow-sm"></div>
              <div class="step shadow-sm" [class.active]="isSubmitted"></div>
            </div>

            <ng-container *ngIf="!isSubmitted">
              <h2 class="fw-bold mb-2 font-jakarta">Confirm your Identity</h2>
              <p class="text-muted mb-5 small px-4">Help us keep Capitalis safe and secure by confirming your identity.</p>

              <div class="upload-container mb-5" (click)="triggerUpload()">
                <div class="upload-zone rounded-5 p-5 border-dashed transition d-flex flex-column align-items-center" [class.success]="isUploaded">
                    <div class="icon-blob bg-dark bg-opacity-5 rounded-circle d-flex align-items-center justify-content-center mb-4" style="width: 80px; height: 80px;">
                      <i class="bi" [class.bi-cloud-upload]="!isUploaded" [class.bi-check-circle-fill]="isUploaded" [class.text-success]="isUploaded" style="font-size: 2.5rem;"></i>
                    </div>
                    <h5 class="fw-bold mb-1 font-jakarta">{{ isUploaded ? 'Document Ready' : 'Click to upload' }}</h5>
                    <p class="text-muted extra-small">{{ fileName || 'PDF, JPG or PNG (max. 10MB)' }}</p>
                    <input type="file" #fileInput class="d-none" (change)="onFileSelected($event)">
                </div>
              </div>

              <div class="alert bg-dark bg-opacity-5 border-0 rounded-4 p-4 text-start mb-5 d-flex align-items-start">
                <i class="bi bi-shield-lock-fill me-3 fs-3 text-dark"></i>
                <div>
                    <h6 class="fw-bold mb-1 font-jakarta">Data protection</h6>
                    <p class="extra-small text-muted mb-0">Your documents are processed securely and encrypted according to international standards.</p>
                </div>
              </div>

              <button class="btn btn-dark w-100 rounded-pill py-3 fw-bold shadow-lg transition-transform hover-scale" 
                      [disabled]="!isUploaded || isSubmitting" (click)="submit()">
                <span *ngIf="!isSubmitting">Continue Verification</span>
                <span *ngIf="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
              </button>
              <p class="mt-4 extra-small text-muted fw-bold text-uppercase" style="letter-spacing: 2px">Step 2 of 3</p>
            </ng-container>

            <!-- Success State -->
            <ng-container *ngIf="isSubmitted">
              <div class="py-4 animate-fade-in">
                <div class="icon-blob bg-success bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center mx-auto mb-4" style="width: 100px; height: 100px;">
                  <i class="bi bi-patch-check-fill text-success" style="font-size: 3.5rem;"></i>
                </div>
                <h2 class="fw-bold mb-3 font-jakarta text-dark">Submission Successful</h2>
                <div class="alert bg-light border-0 rounded-4 p-4 mb-5">
                  <p class="text-dark mb-0 fw-medium">KYC Document submitted successfully! Our agents will review it shortly.</p>
                </div>
                <button class="btn btn-dark w-100 rounded-pill py-3 fw-bold shadow-lg transition-transform hover-scale" (click)="finish()">
                  Back to Dashboard
                </button>
                <p class="mt-4 extra-small text-success fw-bold text-uppercase" style="letter-spacing: 2px">Step 3 of 3 - Complete</p>
              </div>
            </ng-container>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .kyc-container { background-color: #F6F7F9; min-height: 100vh; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .step { width: 35px; height: 6px; border-radius: 10px; background-color: #DEE2E6; }
    .step.active { background-color: #1A1A1A; }
    .upload-zone { border: 2px dashed #CED4DA; background: #FFF; cursor: pointer; border-radius: 32px !important; }
    .upload-zone:hover { background: #F8F9FA; border-color: #1A1A1A; }
    .upload-zone.success { border-color: #198754; background: rgba(25, 135, 84, 0.05); }
    .extra-small { font-size: 0.75rem; }
    .transition { transition: all 0.3s ease; }
    .hover-scale:hover { transform: scale(1.01); }
    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class KycComponent implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;
  isUploaded = false;
  isSubmitting = false;
  isSubmitted = false;
  fileName = '';

  constructor(private apollo: Apollo, private router: Router) { }

  ngOnInit() {
    this.checkCurrentStatus();
  }

  checkCurrentStatus() {
    this.apollo.query({
      query: GET_KYC_STATUS,
      fetchPolicy: 'network-only'
    }).subscribe({
      next: (result: any) => {
        const status = result.data?.me?.profile?.kycStatus;
        if (status === 'SUBMITTED' || status === 'VALIDATED') {
          this.isSubmitted = true;
        }
      },
      error: (err) => console.error('Failed to fetch KYC status on init', err)
    });
  }

  triggerUpload() {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.isUploaded = true;
      this.fileName = file.name;
    }
  }

  submit() {
    this.isSubmitting = true;

    // In a real app, we would first upload the file to S3/Cloud and get a URL.
    // Here we simulate the URL generation but the CALL to the backend is REAL.
    const realDocUrl = `https://ebanking-storage.com/kyc/${this.fileName}`;

    this.apollo.mutate({
      mutation: SUBMIT_KYC,
      variables: { documentUrl: realDocUrl },
      refetchQueries: ['GetProfile', 'GetKycStatus', 'GetDashboardData']
    }).subscribe({
      next: (result: any) => {
        console.log('KYC Backend Response:', result);
        this.isSubmitting = false;
        this.isSubmitted = true;
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('KYC Submission failed', err);
        alert('Error submitting verification to backend. Please check if user-service is running.');
      }
    });
  }

  finish() {
    this.router.navigate(['/dashboard']);
  }
}
