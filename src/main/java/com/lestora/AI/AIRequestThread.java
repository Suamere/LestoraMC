package com.lestora.AI;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lestora.common.models.LestoraVillager;
import com.lestora.common.data.VillagerRepo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AIRequestThread {
    private static boolean aiAvailable = false;
    public static boolean isAiAvailable() { return aiAvailable; }
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final Gson gson = new Gson();
    private static final Map<UUID, JsonObject> msgResponses = new ConcurrentHashMap<>();
    public static JsonObject getMessageResponse(UUID msgId) { return msgResponses.get(msgId); }

    public static void getNewVillager(UUID msgID, List<String> existingNames) { newVillager.put(msgID, existingNames); }
    private static JsonObject tryGetNewVillager(List<String> existingNames) {
        JsonObject response = new JsonObject();
        String suggestion;
        while (true) {
            String usedNames = String.join(",", existingNames);
            String nameSysContent = "Your only goal is to provide a single word, a single unique name, given the user prompt.";
            String userSysContent = "Recommend the first name for a new character. It can be common, or it can lean medieval. It can be masculine or feminine. "
                    + "Be creative, but lean toward real names. Respond with a single word, which should be that name. The only names off the table are the ones listed here: " + usedNames;
            suggestion = sendAI(nameSysContent, userSysContent, null, null);
            if (!existingNames.contains(suggestion)) {
                response.addProperty("name", suggestion);
                break;
            }
        }
        String personalitySysContent = "You are a medieval person named " + suggestion + ". You speak in plain language, not medieval dialect. Stay in character at all times, and use your name as a foundation for how you answer questions.";
        String personalityUserContent = "Describe your personality. Things you like and dislike, your general disposition, and more. Be creative, as this is useful for all future discussion.  But do not include recognizable places like Camelot, or random names of fellow people.  Any nouns you use should be regarding things like pets or hobbies.";
        response.addProperty("personality", sendAI(personalitySysContent, personalityUserContent, null, null));
        //System.out.println("Got name and personality");
        return response;
    }

    public static void chatWithVillager(UUID msgID, LestoraVillager lv, UUID fromPlayerUUID, String userContent) { villagerChat.put(msgID, new ChatWithVillager(lv, userContent, fromPlayerUUID)); }
    private static JsonObject tryChatWithVillager(LestoraVillager lv, UUID fromPlayerUUID, String userContent) {
        JsonObject response = new JsonObject();
        String systemContent = "You are not a friendly chat bot.  You are a medieval person named " + lv.name + ". Do not talk with a medieval dialect or accent. "
                + "Any user request that you get, simply answer in first person, in character. "
                + "Your interactions with the user should not immediately be to help, but should change based on the history of talking with them. "
                + "Assume you know nothing in general about the world unless it's in your chat history.  So if you're asked \"Do you know Jessica\", and there is nothing in your chat history to suggest you do, then you don't. "
                + "User requests are formatted as a narrative. That means if it the user states that Jonathan says, \"Hello\", then you do not know his name is Jonathan yet.  But when the user states that Jonathan says \"My name is Jonathan\", then you do know their name from then on. Keep your responses around 10 words."
                + "Your current personality can be summed up as all of the following: " + lv.getPersonality() + ".";
        var rawChatResponse = sendAI(systemContent, userContent, fromPlayerUUID, lv.getUUID());
//        System.out.println("AI System Input: " + systemContent);
//        System.out.println("AI User Input: " + userContent);
//        System.out.println("Raw AI Chat Response: " + rawChatResponse);
        response.addProperty("response", rawChatResponse);
        return response;
    }

    private static String sendAI(String systemContent, String newUserContent, UUID fromPlayerUUID, UUID toVillagerUUID) {
        try {
            // Build system message
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemContent);


            // Create an array of messages, starting with the system message.
            List<JsonObject> messages = new ArrayList<>();
            messages.add(systemMessage);

            List<VillagerInteraction> interactions = null;
            if (fromPlayerUUID != null && toVillagerUUID != null) {
                interactions = VillagerRepo.getInteractions(fromPlayerUUID, toVillagerUUID);
            }
            // For each VillagerInteraction, create a corresponding JsonObject.
            if (interactions != null) {
                for (VillagerInteraction interaction : interactions) {
                    JsonObject interactionMessage = new JsonObject();
                    interactionMessage.addProperty("role", mapInteractionRole(interaction.getType()));
                    interactionMessage.addProperty("content", interaction.getValue());
                    messages.add(interactionMessage);
                }
            }

            // Build user message
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", newUserContent);
            messages.add(userMessage);

            // Build payload
            JsonObject payload = new JsonObject();
            payload.addProperty("model", "mistral");
            payload.add("messages", gson.toJsonTree(messages));
            payload.addProperty("stream", false);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA);
            Request request = new Request.Builder()
                    .url("http://localhost:11434/api/chat")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("Unexpected code " + response);
                }
                if (response.body() == null) {
                    throw new Exception("Response body is null");
                }
                String bodyMsg = response.body().string();
                JsonObject jsonResponse = gson.fromJson(bodyMsg, JsonObject.class);
                return jsonResponse.getAsJsonObject("message").get("content").getAsString();
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }
    }

    private static String mapInteractionRole(VillagerInteractionType type) {
        String typeName = type.name();
        if (typeName.startsWith("User")) {
            return "user";
        } else if (typeName.startsWith("Assistant")) {
            return "assistant";
        }
        return "unknown";
    }


    private static class ChatWithVillager {
        private final LestoraVillager villager;
        private final String userContent;
        private final UUID fromPlayerUUID;

        public ChatWithVillager(LestoraVillager villager, String content, UUID fromPlayerUUID) {
            this.villager = villager;
            this.userContent = content;
            this.fromPlayerUUID = fromPlayerUUID;
        }

        public LestoraVillager getVillager() {
            return villager;
        }

        public String getUserContent() {
            return userContent;
        }

        public UUID getPlayerUUID() {
            return fromPlayerUUID;
        }
    }

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    public static void startBackgroundProcessing() {
        ScheduledExecutorService checkScheduler = Executors.newSingleThreadScheduledExecutor();

        // Schedule a repeating check every 30 seconds.
        checkScheduler.scheduleWithFixedDelay(() -> {
            if (!mistralAvailable()){
                System.err.println("Mistral server not detected. AI chat with villagers will not work.");
            } else {
                aiAvailable = true;
                System.err.println("Lestora: !!!Mistral server available!!!  (Not an error, just red for visibility)");

                // Schedule the actual processing tasks every 5 seconds.
                executor.scheduleAtFixedRate(() -> {
                    iterateNewVillagers();
                    iterateVillagerChats();
                }, 0, 5, TimeUnit.SECONDS);

                // Once successful, stop further checking.
                checkScheduler.shutdown();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private static boolean mistralAvailable() {
        return (sendAI("Just say hello to me.", "Hello, friend.", null, null) != null);
    }

    private static final Map<UUID, List<String>> newVillager = new ConcurrentHashMap<>();
    private static final Map<UUID, ChatWithVillager> villagerChat = new ConcurrentHashMap<>();
    private static void iterateNewVillagers() {
        var iterator = newVillager.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                msgResponses.put(entry.getKey(), tryGetNewVillager(entry.getValue()));
            } catch (Exception e) {
                //e.printStackTrace();
            }
            iterator.remove();
        }
    }
    private static void iterateVillagerChats() {
        var iterator = villagerChat.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                msgResponses.put(entry.getKey(), tryChatWithVillager(entry.getValue().getVillager(), entry.getValue().getPlayerUUID(), entry.getValue().getUserContent()));
            } catch (Exception e) {
                //e.printStackTrace();
            }
            iterator.remove();
        }
    }
}
