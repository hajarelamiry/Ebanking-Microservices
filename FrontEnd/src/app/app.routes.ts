import { Routes } from '@angular/router';
import { LandingComponent } from './components/landing/landing';
import { DashboardComponent } from './components/dashboard/dashboard';
import { ProfileComponent } from './components/profile/profile';
import { KycComponent } from './components/kyc/kyc';
import { AdminUsersComponent } from './components/admin-users/admin-users';
import { AdminKycComponent } from './components/admin-kyc/admin-kyc';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard';
import { SecurityComponent } from './components/security/security';
import { TransactionsComponent } from './components/transactions/transactions';
import { PaymentsComponent } from './components/payments/payments';
import { CardsComponent } from './components/cards/cards';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: '', component: LandingComponent },
    {
        path: 'dashboard',
        component: DashboardComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    {
        path: 'transactions',
        component: TransactionsComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    {
        path: 'payments',
        component: PaymentsComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    {
        path: 'cards',
        component: CardsComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    {
        path: 'admin/dashboard',
        component: AdminDashboardComponent,
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'AGENT'] }
    },
    {
        path: 'profile',
        component: ProfileComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    {
        path: 'kyc',
        component: KycComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    {
        path: 'admin/users',
        component: AdminUsersComponent,
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'AGENT'] }
    },
    {
        path: 'admin/kyc',
        component: AdminKycComponent,
        canActivate: [AuthGuard],
        data: { roles: ['ADMIN', 'AGENT'] }
    },
    {
        path: 'security',
        component: SecurityComponent,
        canActivate: [AuthGuard],
        data: { roles: ['CLIENT', 'ADMIN', 'AGENT'] }
    },
    { path: '**', redirectTo: '' }
];
