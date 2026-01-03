import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-transactions',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="transactions-container p-4 animate-fade-in">
      <header class="mb-5 px-3">
        <h1 class="fw-bold h2 mb-1 font-jakarta">Transaction History</h1>
        <p class="text-muted small">Monitor and manage all your account activities</p>
      </header>

      <!-- Filter Bar -->
      <div class="d-flex flex-wrap gap-3 mb-4 px-3">
        <div class="filter-pill active">All Activity</div>
        <div class="filter-pill">Income</div>
        <div class="filter-pill">Expenses</div>
        <div class="filter-pill">Transfer</div>
        <div class="ms-md-auto d-flex gap-2">
          <button class="btn btn-light rounded-pill px-4 border shadow-sm small">
            <i class="bi bi-calendar3 me-2"></i> Last 30 Days
          </button>
          <button class="btn btn-dark rounded-pill px-4 shadow-sm small">
            <i class="bi bi-download me-2"></i> Export
          </button>
        </div>
      </div>

      <!-- Transactions List -->
      <div class="card border-0 rounded-5 p-4 shadow-sm bg-white">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr class="text-muted small">
                <th class="border-0 px-4">Transaction Details</th>
                <th class="border-0">Category</th>
                <th class="border-0">Status</th>
                <th class="border-0">Date</th>
                <th class="border-0 text-end px-4">Amount</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let tx of mockTransactions" class="cursor-pointer">
                <td class="px-4 py-3">
                  <div class="d-flex align-items-center">
                    <div class="icon-box-sm rounded-4 me-3 d-flex align-items-center justify-content-center" [ngClass]="tx.type === 'INCOME' ? 'bg-success-light' : 'bg-light'">
                      <i class="bi {{ tx.icon }} " [ngClass]="tx.type === 'INCOME' ? 'text-success' : 'text-dark'"></i>
                    </div>
                    <div>
                      <span class="fw-bold d-block small">{{ tx.description }}</span>
                      <span class="extra-small text-muted">{{ tx.id }}</span>
                    </div>
                  </div>
                </td>
                <td><span class="badge bg-light text-dark rounded-pill px-3">{{ tx.category }}</span></td>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <span class="dot-sm bg-success"></span>
                    <span class="small fw-semibold">Completed</span>
                  </div>
                </td>
                <td class="small text-muted">{{ tx.date | date:'medium' }}</td>
                <td class="text-end px-4 fw-bold" [ngClass]="tx.type === 'INCOME' ? 'text-success' : 'text-dark'">
                  {{ (tx.type === 'INCOME' ? '+' : '-') }}{{ tx.amount | number:'1.2-2' }} <span class="extra-small opacity-50">MAD</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .transactions-container { background-color: #F6F7F9; min-height: 100vh; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    
    .filter-pill { 
      padding: 8px 24px; 
      border-radius: 50px; 
      background: white; 
      font-size: 0.85rem; 
      font-weight: 600; 
      color: #6C757D; 
      cursor: pointer; 
      transition: all 0.3s ease;
      border: 1px solid transparent;
      box-shadow: 0 4px 10px rgba(0,0,0,0.02);
    }
    .filter-pill:hover { background: #F8F9FA; color: #000; }
    .filter-pill.active { background: #1A1A1A; color: #FFF; box-shadow: 0 8px 20px rgba(0,0,0,0.1); }

    .icon-box-sm { width: 45px; height: 45px; font-size: 1.2rem; }
    .bg-success-light { background: rgba(25, 135, 84, 0.1); }
    
    .dot-sm { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
    .extra-small { font-size: 0.7rem; }
    
    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    .table thead th { font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; padding-bottom: 20px; }
    .table tbody tr td { border-top: 1px solid #F0F0F0; }
    .table tbody tr:hover { background-color: #FAFAFA; }
  `]
})
export class TransactionsComponent implements OnInit {
    mockTransactions = [
        { id: 'TX-9842', amount: 4580.00, description: 'Client Payment - Project X', date: new Date(2026, 0, 2, 14, 30), type: 'INCOME', category: 'Project', icon: 'bi-briefcase-fill' },
        { id: 'TX-9841', amount: 120.50, description: 'Starbucks Coffee', date: new Date(2026, 0, 2, 9, 15), type: 'EXPENSE', category: 'Food', icon: 'bi-cup-hot-fill' },
        { id: 'TX-9840', amount: 850.00, description: 'Amazon.com Order', date: new Date(2026, 0, 1, 18, 45), type: 'EXPENSE', category: 'Shopping', icon: 'bi-cart-fill' },
        { id: 'TX-9839', amount: 15000.00, description: 'Monthly Salary', date: new Date(2025, 11, 31, 10, 0), type: 'INCOME', category: 'Salary', icon: 'bi-bank2' },
        { id: 'TX-9838', amount: 450.00, description: 'Netflix Subscription', date: new Date(2025, 11, 30, 22, 30), type: 'EXPENSE', category: 'Entertainment', icon: 'bi-play-circle-fill' },
        { id: 'TX-9837', amount: 2100.00, description: 'Rent Payment', date: new Date(2025, 11, 28, 8, 0), type: 'EXPENSE', category: 'Housing', icon: 'bi-house-fill' },
        { id: 'TX-9836', amount: 320.00, description: 'Marjane Supermarket', date: new Date(2025, 11, 27, 16, 20), type: 'EXPENSE', category: 'Groceries', icon: 'bi-basket-fill' }
    ];

    constructor() { }

    ngOnInit(): void { }
}
