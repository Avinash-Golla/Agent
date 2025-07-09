
package com.yourorg.agent;

import okhttp3.*;
import java.io.IOException;
import java.time.Duration;

public class LLMClient {

    // Local Flask server endpoint
    private static final String API_URL = "http://localhost:5005/query";

    // Reuse OkHttpClient with timeouts
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))   // connect timeout
            .readTimeout(Duration.ofSeconds(60))      // wait for model output
            .callTimeout(Duration.ofSeconds(65))      // total call time
            .build();

    // JSON media type
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static String queryLLM(String prompt) {
        // Prepare JSON body
        String json = "{\"prompt\": \"" + prompt.replace("\"", "\\\"") + "\"}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }

            if (response.body() != null) {
                // Extract only the `response` field from the JSON
                String responseJson = response.body().string();
                return extractResponseText(responseJson);
            } else {
                return "No response body";
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to LLM server: " + e.getMessage());
        }
    }

    // Basic manual JSON parsing (can replace with GSON or Jackson if needed)
    private static String extractResponseText(String json) {
        // Simple string extraction of: {"response": "...."}
        int start = json.indexOf(":") + 2;
        int end = json.lastIndexOf("\"");
        if (start > 0 && end > start) {
            return json.substring(start, end);
        }
        return "Invalid response format";
    }
}

