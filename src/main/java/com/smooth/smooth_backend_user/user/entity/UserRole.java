package com.smooth.smooth_backend_user.user.entity;

public enum UserRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");
    
    private final String authority;
    
    UserRole(String authority) {
        this.authority = authority;
    }
    
    public String getAuthority() {
        return authority;
    }
}