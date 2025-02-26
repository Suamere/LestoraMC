package com.lestora.data;

import com.google.gson.JsonObject;
import com.lestora.AIRequestThread;
import net.minecraft.world.entity.npc.Villager;

import java.util.*;

public class LestoraVillager {
    private final Villager mcVillager;
    public UUID uuid;
    public String name;
    private String personality;
    private UUID newVillagerMsgID; // Should almost always be null, except when starting a fresh villager
    private static final Map<UUID, ChatMessage> chatMessages = new HashMap<>();

    public String getPersonality() {
        return personality;
    }

    private static final Map<UUID, LestoraVillager> newVillagers = new HashMap<>();
    private static final Map<UUID, LestoraVillager> villagers = new HashMap<>();

    public static Map<UUID, LestoraVillager> getVillagers() {
        return Collections.unmodifiableMap(villagers);
    }

    private LestoraVillager(Villager villager) {
        this.mcVillager = villager;
        this.uuid = villager.getUUID();
    }

    public static LestoraVillager get(Villager villager) {
        return villagers.computeIfAbsent(villager.getUUID(), key -> {
            var lestoraVillager = new LestoraVillager(villager);
            lestoraVillager.calcNew(villager);
            return lestoraVillager;
        });
    }

    private void calcNew(Villager villager) {
        var dbVillager = VillagerRepo.getVillager(this.uuid);
        if (dbVillager == null) {
            this.newVillagerMsgID = UUID.randomUUID();
            AIRequestThread.getNewVillager(this.newVillagerMsgID, VillagerRepo.getAllVillagerNames());
            newVillagers.put(this.newVillagerMsgID, this);
        }
        else {
            this.name = dbVillager.getName();
            this.personality = dbVillager.getPersonality();
        }
    }

    public void processChatMessage(JsonObject aiResponse, LestoraPlayer player, String userContent) {
        var msg = aiResponse.get("response").getAsString();
        player.sendMsg(msg);

        List<VillagerInteraction> interactions = new ArrayList<>();
        interactions.add(new VillagerInteraction(VillagerInteractionType.UserWords, userContent));
        interactions.add(new VillagerInteraction(VillagerInteractionType.AssistantWords, msg));

        VillagerRepo.addInteraction(player.getUuid(), this.uuid, interactions);
    }

    public static void processNewVillagers() {
        var iterator = newVillagers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var lestoraVillager = entry.getValue();
            if (lestoraVillager.personality == null && lestoraVillager.newVillagerMsgID != null) {
                var aiResponse = AIRequestThread.getMessageResponse(lestoraVillager.newVillagerMsgID);
                if (aiResponse != null) {
                    lestoraVillager.newVillagerMsgID = null;
                    lestoraVillager.name = aiResponse.get("name").getAsString();
                    lestoraVillager.personality = aiResponse.get("personality").getAsString();
                    VillagerRepo.addVillager(lestoraVillager.uuid, lestoraVillager.name, lestoraVillager.personality);
                    iterator.remove();
                }
            }
        }
    }

    public static void processChatMessages() {
        var iterator = chatMessages.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var msgID = entry.getKey();
            var chatMsg = entry.getValue();

            var aiResponse = AIRequestThread.getMessageResponse(msgID);
            if (aiResponse != null) {
                chatMsg.getVillager().processChatMessage(aiResponse, chatMsg.getPlayer(), chatMsg.getUserContent());
                iterator.remove();
            }
        }
    }

    public void newFocus(LestoraPlayer lestoraPlayer) {
        var newFocusMsgID = UUID.randomUUID();
        var userContent = "Hello, I'm " + lestoraPlayer.getName();
        var chatMsg = new ChatMessage(this, lestoraPlayer, userContent);
        chatMessages.put(newFocusMsgID, chatMsg);
        AIRequestThread.chatWithVillager(newFocusMsgID, this, userContent);
    }

    public static class ChatMessage {

        private final LestoraVillager lestoraVillager;
        private final LestoraPlayer lestoraPlayer;
        private final String userContent;

        public ChatMessage(LestoraVillager lestoraVillager, LestoraPlayer lestoraPlayer, String userContent) {
            this.lestoraVillager = lestoraVillager;
            this.lestoraPlayer = lestoraPlayer;
            this.userContent = userContent;
        }

        public LestoraVillager getVillager() {
            return lestoraVillager;
        }

        public LestoraPlayer getPlayer() {
            return lestoraPlayer;
        }

        public String getUserContent() {
            return userContent;
        }
    }
}