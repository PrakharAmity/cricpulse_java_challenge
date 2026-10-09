package com.cricpulse.model;

public record PartnershipLink(int from, int to, int runs) {
    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public int getRuns() {
        return runs;
    }

    public int getOther(int player) {
        return (from == player) ? to : from;
    }

    public boolean connects(int player) {
        return from == player || to == player;
    }
}
