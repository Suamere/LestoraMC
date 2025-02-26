package com.lestora.data;

public class VillagerEntity {
    private final String name;
    private final String personality;

    public VillagerEntity(String name, String personality) {
        this.name = name;
        this.personality = personality;
    }

    public String getName() {
        return name;
    }

    public String getPersonality() {
        return personality;
    }
}