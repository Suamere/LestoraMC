package com.lestora.data;

import java.util.UUID;

public class LestoraVillager {
    public UUID uuid;
    public String name;
    public String outgoingMessage; // will be null until AI response returns
    public boolean responseReceived; // tracks if we've already gotten a response
    private String personality = "Make it up as you go along and be creative given the other rules.";

    public LestoraVillager(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.outgoingMessage = null;
        this.responseReceived = false;
    }

    public String getPersonality() {
        return personality;
    }
}