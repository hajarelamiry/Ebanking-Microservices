import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-cards',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="cards-container p-4 animate-fade-in">
      <header class="d-flex justify-content-between align-items-center mb-5 px-3">
        <div>
          <h1 class="fw-bold h2 mb-1 font-jakarta">My Cards</h1>
          <p class="text-muted small">Manage your cards and spending limits</p>
        </div>
        <button class="btn btn-dark rounded-pill px-4 py-2 small fw-bold shadow-sm">
          <i class="bi bi-plus-lg me-2"></i> Add New Card
        </button>
      </header>

      <div class="row g-4 px-3">
        <!-- Cards Showcase -->
        <div class="col-lg-6">
          <div class="card-stack mb-5">
            <!-- Main Card -->
            <div class="virtual-card active shadow-lg p-5 text-white mb-4">
              <div class="d-flex justify-content-between align-items-start mb-5">
                <i class="bi bi-broadcast fs-3 opacity-50"></i>
                <span class="fw-bold fs-3">VISA</span>
              </div>
              <div class="mb-5">
                <div class="card-number fw-bold mb-2">5895 7436 1102 7513</div>
                <div class="d-flex gap-4">
                  <div>
                    <span class="extra-small opacity-50 text-uppercase d-block">Exp Date</span>
                    <span class="fw-bold small">12/28</span>
                  </div>
                  <div>
                    <span class="extra-small opacity-50 text-uppercase d-block">CVV</span>
                    <span class="fw-bold small">***</span>
                  </div>
                </div>
              </div>
              <div class="d-flex justify-content-between align-items-end">
                <div>
                  <span class="extra-small opacity-50 text-uppercase d-block">Card Holder</span>
                  <span class="fw-bold small">SAIDA ELHAOURANI</span>
                </div>
                <div class="card-circles">
                  <div class="circle red"></div>
                  <div class="circle yellow"></div>
                </div>
              </div>
            </div>
            
            <!-- Secondary Card Peek -->
            <div class="virtual-card secondary p-4 text-white opacity-25">
               <span class="fw-bold">MASTERCARD</span>
            </div>
          </div>

          <div class="card border-0 rounded-5 p-4 shadow-sm bg-white">
            <h5 class="fw-bold mb-4">Recent Card Activity</h5>
            <div class="d-grid gap-3">
              <div *ngFor="let tx of cardActivity" class="d-flex align-items-center justify-content-between py-2">
                <div class="d-flex align-items-center">
                  <div class="icon-box-sm bg-light rounded-3 me-3 d-flex align-items-center justify-content-center">
                    <i class="bi {{ tx.icon }}"></i>
                  </div>
                  <div>
                    <span class="fw-bold d-block small">{{ tx.name }}</span>
                    <span class="extra-small text-muted">{{ tx.date }}</span>
                  </div>
                </div>
                <span class="fw-bold small">-{{ tx.amount | number:'1.2-2' }} MAD</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Card Control Sidebar -->
        <div class="col-lg-6">
          <div class="card border-0 rounded-5 p-5 shadow-sm bg-white h-100">
            <h5 class="fw-bold mb-5">Card Controls</h5>
            
            <div class="control-group mb-5">
              <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                  <h6 class="fw-bold mb-1">Freeze Card</h6>
                  <p class="extra-small text-muted mb-0">Temporarily disable all transactions</p>
                </div>
                <div class="form-check form-switch fs-4">
                  <input class="form-check-input" type="checkbox">
                </div>
              </div>
              
              <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                  <h6 class="fw-bold mb-1">Contactless Payments</h6>
                  <p class="extra-small text-muted mb-0">Allow NFC based interactions</p>
                </div>
                <div class="form-check form-switch fs-4">
                  <input class="form-check-input" type="checkbox" checked>
                </div>
              </div>

              <div class="d-flex justify-content-between align-items-center">
                <div>
                  <h6 class="fw-bold mb-1">Online Purchases</h6>
                  <p class="extra-small text-muted mb-0">Enable usage for web transactions</p>
                </div>
                <div class="form-check form-switch fs-4">
                  <input class="form-check-input" type="checkbox" checked>
                </div>
              </div>
            </div>

            <div class="limit-section pt-5 border-top">
              <h6 class="fw-bold mb-4">Monthly Spending Limit</h6>
              <div class="d-flex justify-content-between mb-3">
                <span class="fw-bold h4 mb-0">15 000.00 MAD</span>
                <span class="text-muted small">Max 50k</span>
              </div>
              <input type="range" class="form-range custom-range" min="0" max="50000" step="1000" value="15000">
              <p class="extra-small text-muted mt-3">
                You've spent 4 580.00 MAD this month. You're well within your limit.
              </p>
            </div>

            <button class="btn btn-outline-danger w-100 rounded-pill py-3 fw-bold mt-5">
              <i class="bi bi-trash3 me-2"></i> Terminate Card
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .cards-container { background-color: #F6F7F9; min-height: 100vh; font-family: 'Plus Jakarta Sans', sans-serif; }
    .font-jakarta { font-family: 'Plus Jakarta Sans', sans-serif; }
    
    .virtual-card {
      border-radius: 40px;
      position: relative;
      overflow: hidden;
      transition: all 0.3s ease;
    }
    .virtual-card.active {
      background: linear-gradient(135deg, #1A1A1A 0%, #333333 100%);
      min-height: 320px;
      z-index: 2;
    }
    .virtual-card.secondary {
      background: #DEE2E6;
      height: 100px;
      margin-top: -60px;
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .card-number { font-size: 1.8rem; letter-spacing: 4px; }
    .extra-small { font-size: 0.7rem; }
    
    .card-circles { display: flex; }
    .circle { width: 35px; height: 35px; border-radius: 50%; opacity: 0.8; }
    .circle.red { background: #FF4D4D; margin-right: -15px; }
    .circle.yellow { background: #FFB302; }

    .icon-box-sm { width: 40px; height: 40px; }
    
    .custom-range::-webkit-slider-runnable-track { background: #E9ECEF; height: 8px; border-radius: 4px; }
    .custom-range::-webkit-slider-thumb { background: #1A1A1A; border: 4px solid #FFF; box-shadow: 0 4px 10px rgba(0,0,0,0.2); margin-top: -6px; height: 20px; width: 20px; }

    .animate-fade-in { animation: fadeIn 0.8s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class CardsComponent implements OnInit {
    cardActivity = [
        { name: 'Apple.com/Store', amount: 12900.00, date: '02 Jan, 2026', icon: 'bi-apple' },
        { name: 'Netflix Subscription', amount: 150.00, date: '01 Jan, 2026', icon: 'bi-play-circle' },
        { name: 'Uber Trips', amount: 85.00, date: '30 Dec, 2025', icon: 'bi-car-front' }
    ];

    constructor() { }

    ngOnInit(): void { }
}
