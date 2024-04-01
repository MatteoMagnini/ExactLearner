package org.exactlearner.connection;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

public class ChatGPTBridge extends BasicBridge {

    private static final String defaultModel = "gpt-3.5-turbo";
    private static final String url = "https://api.openai.com/v1/chat/completions";

    private int maxTokens = 100;

    public ChatGPTBridge() {
        super();
        BasicBridge.model = defaultModel;
        BasicBridge.url = url;
    }

    public ChatGPTBridge(int maxTokens) {
        super();
        BasicBridge.model = defaultModel;
        BasicBridge.url = url;
        this.maxTokens = maxTokens;
    }

    public ChatGPTBridge(String model, int maxTokens) {
        super();
        BasicBridge.model = model;
        BasicBridge.url = url;
        this.maxTokens = maxTokens;
    }

    public String ask(String message, String key, String system) {
        try {
            HttpURLConnection connection = getConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Content-Type", "application/json");
            String jsonInputString = "{\"model\": \"" + model + "\", \"max_tokens\": " + maxTokens +
                    ", \"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"" + system + "\"}," +
                        "{\"role\": \"user\", \"content\": \"" + message + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(jsonInputString);
            writer.flush();
            writer.close();
            String jsonResponse = getChatGPTResponse(connection);
            return extractMessageFromJSON(jsonResponse);
        } catch (Exception e) {
            System.out.println(ChatGPTCodes.valueOf(extractErrorCode(e.getMessage())));
            return null;
        }
    }

    public String extractMessageFromJSON(String json) {
        String key = "content\": \"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

}
