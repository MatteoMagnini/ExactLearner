package org.exactlearner.connection;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

public class OllamaBridge extends BasicBridge {

    private int maxTokens = 100;

    public OllamaBridge(String model) {
        super();
        BasicBridge.model = model;
        BasicBridge.url = "http://clusters.almaai.unibo.it:11434/api/generate";
    }

    public OllamaBridge(String model, int maxTokens) {
        super();
        BasicBridge.model = model;
        this.maxTokens = maxTokens;
        BasicBridge.url = "http://clusters.almaai.unibo.it:11434/api/generate";
    }

    public OllamaBridge(String host, int port, String model) {
        super();
        BasicBridge.model = model;
        BasicBridge.url = "http://" + host + ":" + port + "/api/generate";
    }

    public String ask(String message, String system) {
        try {
            HttpURLConnection connection = getConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            String jsonInputString = "{\"model\": \"" + model +
                    "\",\"system\": \"" + system +
                    // "\",\"max_tokens\": " + maxTokens +
                    "\",\"stream\": false, \"prompt\": \"" + message + "\"}]}";
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

    private String extractMessageFromJSON(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    @Override
    public String ask(String message, String key, String system) {
        return ask(message, system);
    }
}
