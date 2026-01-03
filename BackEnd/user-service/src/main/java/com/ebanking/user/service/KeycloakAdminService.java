package com.ebanking.user.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.Collections;

@Service
public class KeycloakAdminService {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String adminRealm;

    @Value("${keycloak.admin.username}")
    private String username;

    @Value("${keycloak.admin.password}")
    private String password;

    @Value("${keycloak.target.realm}")
    private String targetRealm;

    public String createUser(String email, String firstName, String lastName, String tempPassword) {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(username)
                .password(password)
                .clientId("admin-cli")
                .build();

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(true);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(tempPassword);
        user.setCredentials(Collections.singletonList(credential));

        UsersResource usersResource = keycloak.realm(targetRealm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // Assign 'CLIENT' role
            try {
                org.keycloak.representations.idm.RoleRepresentation clientRole = keycloak.realm(targetRealm).roles()
                        .get("CLIENT").toRepresentation();
                usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(clientRole));
            } catch (Exception e) {
                System.err.println("Warning: Failed to assign CLIENT role: " + e.getMessage());
                // Don't fail the whole process if role assignment fails, but log it.
            }

            return userId;
        } else if (response.getStatus() == 409) {
            // User already exists, try to find and return the ID
            java.util.List<UserRepresentation> existingUsers = usersResource.search(email);
            if (!existingUsers.isEmpty()) {
                return existingUsers.get(0).getId();
            } else {
                throw new RuntimeException("User exists (409) but could not be found via search: " + email);
            }
        } else {
            throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
        }
    }
}
