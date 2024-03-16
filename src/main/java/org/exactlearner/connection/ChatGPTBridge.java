package org.exactlearner.connection;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

public class ChatGPTBridge extends BasicBridge {

    private static final String model = "gpt-3.5-turbo";
    private static final String url = "https://api.openai.com/v1/chat/completions";

    public ChatGPTBridge() {
        super();
        BasicBridge.model = model;
        BasicBridge.url = url;
    }

    public String ask(String message, String key, String system) {
        try {
            HttpURLConnection connection = getConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonInputString = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \", \"system\": \"" + system + ",\"content\": \"" + message + "\"}]}";
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
        String key = "text\":\"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

}
