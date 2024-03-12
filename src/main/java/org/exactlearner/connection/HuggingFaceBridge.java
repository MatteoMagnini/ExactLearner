package org.exactlearner.connection;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HuggingFaceBridge extends BasicBridge {

    private static final String url = "https://api-inference.huggingface.co/models";

    public HuggingFaceBridge(String model) {
        super();
        BasicBridge.model = model;
        BasicBridge.url = url;
    }

    @Override
    public String ask(String message, String key) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + model))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"inputs\":\"" + message + "\"}")) // Your input text
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return extractMessageFromJSON(response.body());
        } catch (Exception e) {
            System.out.println(ChatGPTCodes.valueOf(extractErrorCode(e.getMessage())));
            return null;
        }
    }

    @Override
    public String extractMessageFromJSON(String json) {
        int start = json.indexOf("text\":\"") + 7;
        int end = json.indexOf("\"}]", start);
        return json.substring(start, end);
    }
}
