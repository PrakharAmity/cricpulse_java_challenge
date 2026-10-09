package com.cricpulse.model;

public record Player(int id, String name, String role) {
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
