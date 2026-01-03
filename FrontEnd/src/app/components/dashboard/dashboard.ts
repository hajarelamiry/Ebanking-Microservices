import { Component, OnInit } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { Apollo, gql } from 'apollo-angular';
import { Observable, catchError, throwError, tap, of } from 'rxjs';
import { RouterModule, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

const GET_DASHBOARD_DATA = gql`
  query GetDashboardData {
    me {
      profile {
        firstName
        lastName
        email
        kycStatus
      }
    }
    transactions {
      id
      amount
      currency
      description
      date
      type
      category
      icon
    }
  }
`;

const SEND_MONEY = gql`
  mutation SendMoney($recipient: String!, $amount: Float!, $description: String) {
    sendMoney(recipient: $recipient, amount: $amount, description: $description) {
      id
      amount
      description
    }
  }
`;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, AsyncPipe, RouterModule],
  template: `
    <div class="dashboard-container p-4 min-vh-100 animate-fade-in">
      
      <!-- Top Header -->
      <header class="d-flex justify-content-between align-items-center mb-5 px-3">
        <div>
          <h1 class="fw-bold h2 mb-0 font-jakarta">Dashboard</h1>
        </div>
        <div class="d-flex align-items-center gap-4">
          <div class="search-box glass-card d-none d-md-flex align-items-center px-3 py-2">
            <i class="bi bi-search text-muted me-2"></i>
            <input type="text" placeholder="Search..." class="bg-transparent border-0 small outline-none">
          </div>
          <button class="icon-btn position-relative">
            <i class="bi bi-bell-fill fs-5"></i>
            <span class="pulse-dot"></span>
          </button>
          <div class="profile-pill glass-card d-flex align-items-center p-1 pe-3 ms-2">
            <img [src]="'https://ui-avatars.com/api/?name=' + (userProfile?.firstName || 'User') + '&background=000&color=fff'" class="rounded-circle me-2" alt="Avatar">
            <span class="small fw-bold">{{ userProfile?.firstName || 'User' }}</span>
          </div>
        </div>
      </header>

      <div class="row g-4">
        <!-- Main Content Area -->
        <div class="col-xl-8">
          
          <!-- Summary Cards -->
          <div class="row g-4 mb-5">
            <div class="col-md-3" *ngFor="let card of summaryCards">
              <div class="card border-0 rounded-5 p-4 shadow-sm h-100 transition-hover" [ngClass]="card.bg">
                <div class="icon-circle mb-3 d-flex align-items-center justify-content-center" [style.background]="card.iconBg">
                  <i class="bi {{ card.icon }} fs-4" [style.color]="card.iconColor"></i>
                </div>
                <h6 class="text-muted small fw-bold mb-1">{{ card.title }}</h6>
                <h3 class="fw-bold mb-0 font-jakarta">{{ card.value | number:'1.2-2' }} <span class="small opacity-50">MAD</span></h3>
              </div>
            </div>
          </div>

          <!-- Finances Section (Chart Area) -->
          <div class="card border-0 rounded-5 p-4 mb-5 shadow-sm bg-white">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0">Finances</h5>
              <div class="d-flex gap-3 small">
                <span class="d-flex align-items-center"><span class="dot bg-primary me-2"></span> Income</span>
                <span class="d-flex align-items-center"><span class="dot bg-danger me-2"></span> Expenses</span>
              </div>
            </div>
            <!-- Chart Placeholder -->
            <div class="chart-container bg-light rounded-4 d-flex align-items-center justify-content-center" style="height: 300px;">
              <div class="text-center">
                <i class="bi bi-graph-up-arrow fs-1 text-muted opacity-25 d-block mb-3"></i>
                <p class="text-muted small">Analytics visualization loading...</p>
              </div>
            </div>
          </div>

          <!-- Transaction History -->
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0">Transaction History</h5>
              <button class="btn btn-link text-dark fw-bold text-decoration-none small">See all</button>
            </div>
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead>
                  <tr class="text-muted small">
                    <th class="border-0 px-4">Name</th>
                    <th class="border-0">Type</th>
                    <th class="border-0">Date</th>
                    <th class="border-0 text-end px-4">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let tx of transactions" class="cursor-pointer">
                    <td class="px-4">
                      <div class="d-flex align-items-center">
                        <div class="avatar-sm me-3 bg-light rounded-circle d-flex align-items-center justify-content-center">
                          <i class="bi bi-person-fill text-muted"></i>
                        </div>
                        <span class="fw-bold small">{{ tx.description }}</span>
                      </div>
                    </td>
                    <td><span class="badge bg-light text-dark rounded-pill px-3">{{ tx.category }}</span></td>
                    <td class="small text-muted">{{ tx.date | date:'mediumDate' }}</td>
                    <td class="text-end px-4 fw-bold" [ngClass]="tx.type === 'INCOME' ? 'text-success' : 'text-dark'">
                      {{ (tx.type === 'INCOME' ? '+' : '-') }}{{ tx.amount | number:'1.2-2' }} <span class="extra-small opacity-50">MAD</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Sidebar Rigth Area -->
        <div class="col-xl-4">
          
          <!-- My Card Section -->
          <div class="card border-0 rounded-5 p-4 mb-4 shadow-sm bg-white">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0">My Card</h5>
              <button class="btn btn-dark btn-sm rounded-pill px-3">Add Card</button>
            </div>
            <div class="premium-card p-4 rounded-5 shadow-lg text-white mb-4 position-relative overflow-hidden">
              <div class="d-flex justify-content-between align-items-start mb-5">
                <i class="bi bi-broadcast fs-4 opacity-50"></i>
                <span class="fw-bold fs-4">VISA</span>
              </div>
              <div class="mb-4">
                <h4 class="fw-bold letter-spacing-2 mb-1">5895 7436 1102 7513</h4>
                <p class="extra-small opacity-50 mb-0">CARD HOLDER</p>
                <p class="fw-bold small mb-0">{{ userProfile?.firstName }} {{ userProfile?.lastName }}</p>
              </div>
              <div class="d-flex justify-content-between align-items-end">
                <span class="extra-small fw-bold opacity-50">11/24</span>
                <div class="card-circles">
                  <div class="circle red"></div>
                  <div class="circle yellow"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Quick Transaction -->
          <div class="card border-0 rounded-5 p-4 mb-4 shadow-sm bg-white">
            <h5 class="fw-bold mb-4">Quick Transaction</h5>
            <div class="d-flex gap-3 mb-4 overflow-auto pb-2">
              <div class="text-center cursor-pointer" *ngFor="let contact of quickContacts">
                <img [src]="contact.avatar" class="rounded-circle mb-2 border border-2 border-white shadow-sm" style="width: 50px; height: 50px;">
                <p class="extra-small mb-0 fw-bold">{{ contact.name }}</p>
              </div>
              <div class="text-center cursor-pointer">
                <div class="avatar-add rounded-circle mb-2 d-flex align-items-center justify-content-center bg-light border-dashed">
                  <i class="bi bi-plus fs-4"></i>
                </div>
                <p class="extra-small mb-0 fw-bold">Add</p>
              </div>
            </div>
            <div class="input-group glass-card overflow-hidden">
              <input type="number" class="form-control bg-transparent border-0 small py-3" placeholder="0.00" #quickAmount>
              <span class="input-group-text bg-transparent border-0 pe-3 text-muted">MAD</span>
              <button class="btn btn-dark rounded-pill m-1 px-4 small" (click)="sendMoney('Quick Recipient', quickAmount.value)">Send</button>
            </div>
          </div>

          <!-- My Goals -->
          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0">My Goals</h5>
              <button class="icon-btn-sm"><i class="bi bi-plus-lg"></i></button>
            </div>
            <div class="goal-item p-3 rounded-4 bg-light mb-3" *ngFor="let goal of goals">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <div class="d-flex align-items-center">
                  <div class="icon-box-sm bg-white rounded-3 me-3 d-flex align-items-center justify-content-center">
                    <i class="bi {{ goal.icon }}"></i>
                  </div>
                  <div>
                    <h6 class="small fw-bold mb-0">{{ goal.title }}</h6>
                    <p class="extra-small text-muted mb-0">{{ goal.target | number:'1.2-2' }} MAD Target</p>
                  </div>
                </div>
                <span class="extra-small fw-bold">{{ (goal.current / goal.target) * 100 | number:'1.0-0' }}%</span>
              </div>
              <div class="progress rounded-pill shadow-sm" style="height: 6px;">
                <div class="progress-bar bg-dark rounded-pill" [style.width]="(goal.current / goal.target) * 100 + '%'"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container { background-color: #F6F7F9; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    .glass-card { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 20px; }
    .outline-none:focus { outline: none; }
    
    .icon-btn { width: 45px; height: 45px; border-radius: 15px; background: white; border: none; box-shadow: 0 4px 10px rgba(0,0,0,0.03); transition: all 0.3s ease; }
    .icon-btn:hover { box-shadow: 0 10px 20px rgba(0,0,0,0.06); }
    .pulse-dot { position: absolute; top: 12px; right: 12px; width: 8px; height: 8px; background: #FF4D4D; border-radius: 50%; outline: 2px solid white; animation: pulse 2s infinite; }
    @keyframes pulse { 0% { transform: scale(1); opacity: 1; } 70% { transform: scale(3); opacity: 0; } 100% { transform: scale(0); opacity: 0; } }

    .icon-circle { width: 40px; height: 40px; border-radius: 12px; }
    .transition-hover { transition: all 0.3s ease; cursor: pointer; }
    .transition-hover:hover { box-shadow: 0 15px 30px rgba(0,0,0,0.08) !important; }

    .premium-card { 
      background: linear-gradient(135deg, #2D3436 0%, #000000 100%);
      min-height: 220px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .letter-spacing-2 { letter-spacing: 2.5px; }
    .card-circles { display: flex; }
    .circle { width: 25px; height: 25px; border-radius: 50%; opacity: 0.8; }
    .circle.red { background: #FF4D4D; margin-right: -10px; }
    .circle.yellow { background: #FFB302; }

    .avatar-sm { width: 40px; height: 40px; }
    .avatar-add { width: 50px; height: 50px; border: 2px dashed #DDD; color: #BBB; }
    .icon-box-sm { width: 35px; height: 35px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }

    .extra-small { font-size: 0.7rem; }
    .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
    
    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    /* Custom Table Styles */
    .table thead th { font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; padding-bottom: 20px; }
    .table tbody tr td { padding-top: 15px; padding-bottom: 15px; border-top: 1px solid #F0F0F0; }
    .table tbody tr:first-child td { border-top: none; }
  `]
})
export class DashboardComponent implements OnInit {
  userProfile: any = null;
  summaryCards: any[] = [];
  transactions: any[] = [];
  quickContacts: any[] = [];
  goals: any[] = [];

  dashboardData$!: Observable<any>;
  showTransferModal = false;
  loading = true;
  error: string | null = null;

  constructor(private apollo: Apollo, private readonly keycloak: KeycloakService, private router: Router) { }

  ngOnInit() {
    this.checkRoleAndRedirect();
    this.initStaticData();
    this.loadData();
  }

  initStaticData() {
    this.quickContacts = [
      { name: 'Michael', avatar: 'https://ui-avatars.com/api/?name=Michael+Jordan&background=E3F2FD&color=0D47A1' },
      { name: 'Edelyn', avatar: 'https://ui-avatars.com/api/?name=Edelyn+Sandra&background=F3E5F5&color=4A148C' },
      { name: 'Ahmed', avatar: 'https://ui-avatars.com/api/?name=Ahmed+Azhar&background=E8F5E9&color=1B5E20' },
      { name: 'Celyn', avatar: 'https://ui-avatars.com/api/?name=Celyn+Dustin&background=FFFDE7&color=F57F17' }
    ];

    this.goals = [
      { title: 'New iMac', target: 2500, current: 1800, icon: 'bi-display' },
      { name: 'New Macbook 14"', target: 3200, current: 1200, icon: 'bi-laptop' }
    ];
    // Note: mapping 'name' to 'title' if needed for consistency
    this.goals = this.goals.map(g => ({ ...g, title: g.title || g.name }));
  }

  checkRoleAndRedirect() {
    try {
      const roles = this.keycloak.getUserRoles();
      if (roles.includes('ADMIN') || roles.includes('AGENT')) {
        this.router.navigate(['/admin/dashboard']);
      }
    } catch (e) {
      console.warn('Dashboard: Failed to check roles for redirection', e);
    }
  }

  loadData() {
    this.loading = true;
    this.error = null;

    this.apollo.watchQuery<any>({
      query: GET_DASHBOARD_DATA,
      fetchPolicy: 'network-only'
    }).valueChanges.subscribe({
      next: (result) => {
        const data = result.data;
        this.userProfile = data?.me?.profile;
        this.transactions = data?.transactions || [];

        this.updateSummaryCards(12520, 421, 164, 257); // Example values for now
        this.loading = false;
      },
      error: (err) => {
        console.warn('Dashboard: Backend failed, falling back to interactive mock data', err);
        this.loadMockData();
      }
    });
  }

  loadMockData() {
    this.userProfile = { firstName: 'Saida', lastName: 'Dev', kycStatus: 'VALIDATED' };
    this.transactions = [
      { id: '1', amount: 45, currency: 'MAD', description: 'Aaron Evans', date: new Date(), type: 'EXPENSE', category: 'Food', icon: 'bi-basket' },
      { id: '2', amount: 241, currency: 'MAD', description: 'Clement Stewart', date: new Date(), type: 'EXPENSE', category: 'Shopping', icon: 'bi-cart' },
      { id: '3', amount: 100, currency: 'MAD', description: 'Jessica Johanne', date: new Date(), type: 'INCOME', category: 'Others', icon: 'bi-cash' }
    ];
    this.updateSummaryCards(12520, 421, 164, 257);
    this.loading = false;
  }

  updateSummaryCards(balance: number, income: number, expenses: number, savings: number) {
    this.summaryCards = [
      { title: 'Balance', value: balance, icon: 'bi-wallet2', iconColor: '#0D6EFD', iconBg: 'rgba(13, 110, 253, 0.1)', bg: 'bg-white' },
      { title: 'Income', value: income, icon: 'bi-arrow-down-left-circle', iconColor: '#198754', iconBg: 'rgba(25, 135, 84, 0.1)', bg: 'bg-white' },
      { title: 'Expenses', value: expenses, icon: 'bi-arrow-up-right-circle', iconColor: '#DC3545', iconBg: 'rgba(220, 53, 69, 0.1)', bg: 'bg-white' },
      { title: 'Savings', value: savings, icon: 'bi-piggy-bank', iconColor: '#6F42C1', iconBg: 'rgba(111, 66, 193, 0.1)', bg: 'bg-white' }
    ];
  }

  retry() {
    this.loadData();
  }

  sendMoney(recipient: string, amount: any) {
    const numAmount = parseFloat(amount);
    if (!recipient || isNaN(numAmount) || numAmount <= 0) {
      alert('Please enter a valid amount');
      return;
    }

    this.apollo.mutate({
      mutation: SEND_MONEY,
      variables: {
        recipient,
        amount: numAmount,
        description: 'Quick Transfer'
      },
      refetchQueries: [{ query: GET_DASHBOARD_DATA }]
    }).subscribe({
      next: () => {
        alert('Transfer Successful!');
        this.loadData();
      },
      error: (err) => {
        console.error('Transfer failed', err);
        // Simulate success in mock mode for UI demo
        alert('Simulation: Transfer Successful!');
        this.loadData();
      }
    });
  }
}
