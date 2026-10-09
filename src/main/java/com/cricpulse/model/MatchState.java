package com.cricpulse.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchState {
    private int score;
    private int wickets;
    private int target;
    private int currentOver;
    private List<Over> overs = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private List<PartnershipLink> partnerships = new ArrayList<>();

    @JsonIgnore
    private final Map<Integer, List<PartnershipLink>> adjacencyMap = new LinkedHashMap<>();

    public MatchState() {
    }

    public MatchState(int score, int wickets, int target, int currentOver) {
        this.score = score;
        this.wickets = wickets;
        this.target = target;
        this.currentOver = currentOver;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getWickets() {
        return wickets;
    }

    public void setWickets(int wickets) {
        this.wickets = wickets;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getCurrentOver() {
        return currentOver;
    }

    public void setCurrentOver(int currentOver) {
        this.currentOver = currentOver;
    }

    public List<Over> getOvers() {
        return overs;
    }

    public void setOvers(List<Over> overs) {
        this.overs = overs;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
        for (Player p : players) {
            adjacencyMap.putIfAbsent(p.id(), new ArrayList<>());
        }
    }

    public List<PartnershipLink> getPartnerships() {
        return partnerships;
    }

    public void setPartnerships(List<PartnershipLink> partnerships) {
        this.partnerships = partnerships;
        rebuildAdjacency();
    }

    public void addOver(int number, int runs) {
        this.overs.add(new Over(number, runs));
    }

    public void addPlayer(int id, String name, String role) {
        this.players.add(new Player(id, name, role));
        this.adjacencyMap.putIfAbsent(id, new ArrayList<>());
    }

    public void addPartnership(int a, int b, int runs) {
        PartnershipLink link = new PartnershipLink(a, b, runs);
        this.partnerships.add(link);
        this.adjacencyMap.computeIfAbsent(a, k -> new ArrayList<>()).add(link);
        this.adjacencyMap.computeIfAbsent(b, k -> new ArrayList<>()).add(link);
    }

    public List<PartnershipLink> getPartnershipsForPlayer(int playerId) {
        return adjacencyMap.getOrDefault(playerId, Collections.emptyList());
    }

    public List<Integer> getNeighbors(int playerId) {
        List<PartnershipLink> links = getPartnershipsForPlayer(playerId);
        List<Integer> neighbors = new ArrayList<>(links.size());
        for (PartnershipLink link : links) {
            neighbors.add(link.getOther(playerId));
        }
        return neighbors;
    }

    public int getPartnershipRuns(int a, int b) {
        for (PartnershipLink link : getPartnershipsForPlayer(a)) {
            if (link.getOther(a) == b) {
                return link.runs();
            }
        }
        return 0;
    }

    private void rebuildAdjacency() {
        adjacencyMap.clear();
        for (Player p : players) {
            adjacencyMap.put(p.id(), new ArrayList<>());
        }
        for (PartnershipLink link : partnerships) {
            adjacencyMap.computeIfAbsent(link.from(), k -> new ArrayList<>()).add(link);
            adjacencyMap.computeIfAbsent(link.to(), k -> new ArrayList<>()).add(link);
        }
    }
}
