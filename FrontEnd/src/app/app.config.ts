import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { KeycloakAngularModule, KeycloakService, KeycloakBearerInterceptor } from 'keycloak-angular';
import { APOLLO_OPTIONS, Apollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { InMemoryCache } from '@apollo/client/core';

import { environment } from '../environments/environment';

function initializeKeycloak(keycloak: KeycloakService) {
  return () => {
    console.log('Initializing Keycloak...');

    return keycloak.init({
      config: {
        url: 'http://localhost:8080',
        realm: 'ebanking-realm',
        clientId: 'ebanking-client',
      },
      initOptions: {
        onLoad: 'check-sso',
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
      },
      enableBearerInterceptor: true,
      bearerExcludedUrls: ['/assets', '/clients/public']
    }).then(isLoggedIn => {
      console.log('Keycloak initialized. Logged in?', isLoggedIn);
      return isLoggedIn;
    }).catch(err => {
      console.error('Keycloak failed to initialize. Error object:', err);
      // We still return true to let the app start, but AuthGuard will catch unauthorized access
      return true;
    });
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    importProvidersFrom(KeycloakAngularModule),
    Apollo,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: KeycloakBearerInterceptor,
      multi: true
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService],
    },
    {
      provide: APOLLO_OPTIONS,
      useFactory: (httpLink: HttpLink) => {
        return {
          cache: new InMemoryCache(),
          link: httpLink.create({
            uri: 'http://localhost:4000/graphql',
          }),
        };
      },
      deps: [HttpLink],
    },
  ],
};
