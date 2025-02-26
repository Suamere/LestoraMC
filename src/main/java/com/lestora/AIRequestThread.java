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

public class AIRequestThread {
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final Gson gson = new Gson();
    private static final Map<UUID, String> msgResponses = new ConcurrentHashMap<>();

    public static String getMessageResponse(UUID msgId) {
        return msgResponses.get(msgId);
    }

    public static void getNameSuggestion(UUID msgID, List<String> currentNames) {
        try {
            String usedNames = String.join(",", currentNames);
            String systemContent = "Your only goal is to provide a single word, a single unique name, given the user prompt.";
            String userContent = "Recommend the first name for a new character. It can be common, or it can lean medieval. It can be masculine or feminine. "
                    + "Be creative, but lean toward real names. Respond with a single word, which should be that name. The only names off the table are the ones listed here: " + usedNames;
            msgResponses.put(msgID, sendAI(systemContent, userContent));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void chatWithVillager(UUID msgID, LestoraVillager lv, String userContent) {
        try {
            String systemContent = "You are a medieval person named " + lv.name + ". Do Not talk with a medieval dialect or accent. Your current personality can be summed up as: " + lv.getPersonality() + ". "
                    + "Be creative. Any user request that you get, simply answer in first person. Keep your responses well below 300 char length.";
            msgResponses.put(msgID, sendAI(systemContent, userContent));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
