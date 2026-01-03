import { Injectable } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    Router,
    RouterStateSnapshot
} from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { Apollo, gql } from 'apollo-angular';
import { firstValueFrom } from 'rxjs';

const GET_KYC_STATUS = gql`
  query GetKycStatus {
    me {
      profile {
        kycStatus
      }
    }
  }
`;

@Injectable({
    providedIn: 'root'
})
export class AuthGuard implements CanActivate {
    constructor(
        protected readonly router: Router,
        protected readonly keycloak: KeycloakService,
        private apollo: Apollo
    ) { }

    public async canActivate(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Promise<boolean> {
        try {
            const authenticated = await this.keycloak.isLoggedIn();

            if (!authenticated) {
                console.log('AuthGuard: User not authenticated, redirecting to login...');
                await this.keycloak.login({
                    redirectUri: window.location.origin + state.url
                });
                return false;
            }

            // Get the roles required from the route.
            const requiredRoles = route.data['roles'];
            if (!Array.isArray(requiredRoles) || requiredRoles.length === 0) {
                return true;
            }

            // Check roles
            const userRoles = this.keycloak.getUserRoles();
            const hasAccess = requiredRoles.some((role) => userRoles.includes(role));

            if (!hasAccess) {
                console.warn('AuthGuard: Access denied (insufficient roles). Redirecting to /');
                this.router.navigate(['/']);
                return false;
            }

            // Real KYC Check
            // We only enforce this for non-admin/agent roles or specific dashboard access
            // If user is trying to reach dashboard, profile, or account-related pages
            try {
                const { data } = await firstValueFrom(this.apollo.query<any>({
                    query: GET_KYC_STATUS,
                    fetchPolicy: 'network-only'
                }));

                const status = data?.me?.profile?.kycStatus;
                console.log('AuthGuard: KYC Status is', status);

                if (status !== 'VALIDATED' && state.url !== '/kyc' && !userRoles.includes('ADMIN') && !userRoles.includes('AGENT')) {
                    console.log('AuthGuard: KYC not validated, redirecting to /kyc');
                    this.router.navigate(['/kyc']);
                    return false;
                }
            } catch (kycError) {
                console.error('AuthGuard: Failed to fetch KYC status', kycError);
                // Fail secure: If we can't verify KYC, we assume it's not done.
                if (state.url !== '/kyc' && !userRoles.includes('ADMIN') && !userRoles.includes('AGENT')) {
                    this.router.navigate(['/kyc']);
                    return false;
                }
            }

            return true;
        } catch (error) {
            console.error('AuthGuard error', error);
            return false;
        }
    }
}
