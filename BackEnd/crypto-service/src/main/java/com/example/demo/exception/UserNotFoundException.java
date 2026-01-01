package com.example.demo.exception;

/**
 * Exception levée quand l'utilisateur n'est pas trouvé
 */
public class UserNotFoundException extends BusinessException {
    
    public UserNotFoundException(String message) {
        super("USER_NOT_FOUND", message);
    }
}
