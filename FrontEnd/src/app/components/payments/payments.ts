import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-payments',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="payments-container p-4 animate-fade-in">
      <header class="mb-5 px-3">
        <h1 class="fw-bold h2 mb-1 font-jakarta">Send Money</h1>
        <p class="text-muted small">Instant and secure transfers to anyone, anywhere</p>
      </header>

      <div class="row g-4 px-3">
        <!-- Transfer Form -->
        <div class="col-lg-7">
          <div class="card border-0 rounded-5 p-5 shadow-sm bg-white h-100">
            <h5 class="fw-bold mb-4">Transfer Details</h5>
            
            <div class="mb-4">
              <label class="form-label text-muted small fw-bold text-uppercase">Select Account</label>
              <div class="account-selector p-3 rounded-4 border d-flex align-items-center justify-content-between cursor-pointer">
                <div class="d-flex align-items-center">
                  <div class="icon-box bg-dark text-white rounded-3 me-3 d-flex align-items-center justify-content-center">
                    <i class="bi bi-wallet2"></i>
                  </div>
                  <div>
                    <span class="fw-bold d-block small">Main Checking Account</span>
                    <span class="extra-small text-muted">**** 7513 | 42 500.00 MAD</span>
                  </div>
                </div>
                <i class="bi bi-chevron-down text-muted"></i>
              </div>
            </div>

            <div class="mb-4">
              <label class="form-label text-muted small fw-bold text-uppercase">Recipient</label>
              <input type="text" class="form-control premium-input" placeholder="Enter name, email or phone">
            </div>

            <div class="row g-3 mb-4">
              <div class="col-md-8">
                <label class="form-label text-muted small fw-bold text-uppercase">Amount</label>
                <div class="input-group premium-group">
                  <input type="number" class="form-control border-0" placeholder="0.00">
                  <span class="input-group-text bg-transparent border-0 pe-4 text-muted fw-bold">MAD</span>
                </div>
              </div>
              <div class="col-md-4">
                <label class="form-label text-muted small fw-bold text-uppercase">Fee</label>
                <div class="p-3 bg-light rounded-4 text-center">
                  <span class="fw-bold small">0.00 MAD</span>
                </div>
              </div>
            </div>

            <div class="mb-5">
              <label class="form-label text-muted small fw-bold text-uppercase">Message (Optional)</label>
              <textarea class="form-control premium-input" rows="2" placeholder="What is this for?"></textarea>
            </div>

            <button class="btn btn-dark w-100 rounded-pill py-3 fw-bold shadow-lg mt-auto">
              Confirm Transfer
            </button>
          </div>
        </div>

        <!-- Beneficiaries & Limits -->
        <div class="col-lg-5">
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white mb-4">
            <h5 class="fw-bold mb-4">Recent Beneficiaries</h5>
            <div class="d-grid gap-3">
              <div *ngFor="let contact of beneficiaries" class="beneficiary-item p-3 rounded-4 d-flex align-items-center justify-content-between cursor-pointer">
                <div class="d-flex align-items-center">
                  <img [src]="contact.avatar" class="rounded-circle me-3" style="width: 45px; height: 45px;">
                  <div>
                    <span class="fw-bold d-block small">{{ contact.name }}</span>
                    <span class="extra-small text-muted">{{ contact.account }}</span>
                  </div>
                </div>
                <i class="bi bi-chevron-right text-muted opacity-50"></i>
              </div>
              <button class="btn btn-light rounded-4 py-3 border-dashed text-muted small">
                <i class="bi bi-plus-lg me-2"></i> Add New Beneficiary
              </button>
            </div>
          </div>

          <div class="card border-0 rounded-5 p-4 shadow-sm bg-dark text-white">
            <h5 class="fw-bold mb-3">Transfer Limit</h5>
            <div class="d-flex justify-content-between mb-2">
              <span class="small opacity-50">Daily available</span>
              <span class="small fw-bold">25 000.00 MAD</span>
            </div>
            <div class="progress rounded-pill bg-secondary mb-4" style="height: 6px;">
              <div class="progress-bar bg-white rounded-pill" style="width: 45%"></div>
            </div>
            <p class="extra-small text-white text-opacity-50 mb-0">
              You've used 11 250.00 MAD of your daily limit. You can increase this in Security settings.
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .payments-container { background-color: #F6F7F9; min-height: 100vh; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    
    .premium-input {
      background-color: #F8F9FA !important;
      border: 2px solid transparent !important;
      border-radius: 16px !important;
      padding: 16px 20px !important;
      font-size: 0.95rem !important;
      font-weight: 600;
      transition: all 0.2s ease;
    }
    .premium-input:focus {
      background-color: white !important;
      border-color: #1A1A1A !important;
      box-shadow: 0 10px 20px rgba(0,0,0,0.05) !important;
    }

    .premium-group {
      background-color: #F8F9FA;
      border-radius: 16px;
      border: 2px solid transparent;
      padding: 4px;
    }
    .premium-group:focus-within {
      background-color: white;
      border-color: #1A1A1A;
      box-shadow: 0 10px 20px rgba(0,0,0,0.05);
    }
    .premium-group input { background: transparent; padding: 12px 16px; font-weight: 700; font-size: 1.1rem; }

    .icon-box { width: 40px; height: 40px; }
    .extra-small { font-size: 0.7rem; }
    
    .beneficiary-item { transition: all 0.2s; background: #FFF; border: 1px solid #F0F0F0; }
    .beneficiary-item:hover { background: #F8F9FA; border-color: #DDD; }
    
    .border-dashed { border: 2px dashed #DDD; background: transparent; }
    
    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class PaymentsComponent implements OnInit {
    beneficiaries = [
        { name: 'Michael Jordan', account: '**** 8821', avatar: 'https://ui-avatars.com/api/?name=Michael+Jordan&background=E3F2FD&color=0D47A1' },
        { name: 'Edelyn Sandra', account: '**** 4432', avatar: 'https://ui-avatars.com/api/?name=Edelyn+Sandra&background=F3E5F5&color=4A148C' },
        { name: 'Ahmed Azhar', account: '**** 1129', avatar: 'https://ui-avatars.com/api/?name=Ahmed+Azhar&background=E8F5E9&color=1B5E20' }
    ];

    constructor() { }

    ngOnInit(): void { }
}
