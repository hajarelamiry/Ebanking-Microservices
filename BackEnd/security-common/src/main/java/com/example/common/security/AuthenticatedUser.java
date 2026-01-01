package com.example.common.security;

import java.util.Set;

/**
 * Représente un utilisateur authentifié avec toutes ses informations extraites du JWT
 */
public final class AuthenticatedUser {
    private final String userId;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final Set<String> roles;

    public AuthenticatedUser(String userId, String username, String email, 
                             String firstName, String lastName, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isClient() {
        return hasRole("CLIENT");
    }

    public boolean isAgent() {
        return hasRole("AGENT");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", roles=" + roles +
                '}';
    }
}
