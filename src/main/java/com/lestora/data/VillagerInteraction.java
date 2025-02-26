package com.lestora.data;

public class VillagerInteraction {
    private final VillagerInteractionType type;
    private final String value;

    public VillagerInteraction(VillagerInteractionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public VillagerInteractionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}