package com.lestora.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerConversations {
    // Maps villager UUID to conversation data
    public static Map<UUID, LestoraVillager> villagerConversations = new HashMap<>();
    public static int villagerCount = 0;
}