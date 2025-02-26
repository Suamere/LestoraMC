package com.lestora;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lestora.data.LestoraVillager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AIRequestThread {
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
            suggestion = sendAI(nameSysContent, userSysContent);
            if (!existingNames.contains(suggestion)) {
                response.addProperty("name", suggestion);
                break;
            }
        }
        String personalitySysContent = "You are a medieval person named " + suggestion + ". You speak in plain language, not medieval dialect. Stay in character at all times, and use your name as a foundation for how you answer questions.";
        String personalityUserContent = "Describe your personality. Things you like and dislike, your general disposition, and more. Be creative, as this is useful for all future discussion.";
        response.addProperty("personality", sendAI(personalitySysContent, personalityUserContent));
        return response;
    }

    public static void chatWithVillager(UUID msgID, LestoraVillager lv, String userContent) { villagerChat.put(msgID, new ChatWithVillager(lv, userContent)); }
    private static JsonObject tryChatWithVillager(LestoraVillager lv, String userContent) {
        JsonObject response = new JsonObject();
        String systemContent = "You are a medieval person named " + lv.name + ". Do not talk with a medieval dialect or accent. Your current personality can be summed up as: " + lv.getPersonality() + ". "
                + "Be creative. Any user request that you get, simply answer in first person. Keep your responses well below 300 char length.";
        response.addProperty("response", sendAI(systemContent, userContent));
        return response;
    }

    private static String sendAI(String systemContent, String userContent) {
        try {
            // Build messages array
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemContent);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", userContent);

            JsonObject payload = new JsonObject();
            payload.addProperty("model", "mistral");
            payload.add("messages", gson.toJsonTree(new JsonObject[]{systemMessage, userMessage}));
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
            e.printStackTrace();
            return null;
        }
    }

    private static class ChatWithVillager {
        private final LestoraVillager villager;
        private final String userContent;
        public ChatWithVillager(LestoraVillager villager, String content) {
            this.villager = villager;
            this.userContent = content;
        }

        public LestoraVillager getVillager() {
            return villager;
        }

        public String getUserContent() {
            return userContent;
        }
    }

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    public static void startBackgroundProcessing() {
        executor.scheduleAtFixedRate(() -> {
            iterateNewVillagers();
            iterateVillagerChats();
        }, 0, 5, TimeUnit.SECONDS);
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
                e.printStackTrace();
            }
            iterator.remove();
        }
    }
    private static void iterateVillagerChats() {
        var iterator = villagerChat.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                msgResponses.put(entry.getKey(), tryChatWithVillager(entry.getValue().getVillager(), entry.getValue().getUserContent()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            iterator.remove();
        }
    }
}
