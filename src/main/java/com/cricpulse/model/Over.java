package com.cricpulse.model;

public record Over(int number, int runs) {
    public int getNumber() {
        return number;
    }

    public int getRuns() {
        return runs;
    }
}
