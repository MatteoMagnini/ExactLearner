package org.exactlearner.connection;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

public class OllamaBridge extends BasicBridge {

    private int maxTokens = 100;
    private static String defaultURL = "http://clusters.almaai.unibo.it:11434/api/generate";

    public OllamaBridge(String model) {
        super();
        BasicBridge.model = model;
        BasicBridge.url = defaultURL;
    }

    public OllamaBridge(String model, int maxTokens) {
        super();
        BasicBridge.model = model;
        this.maxTokens = maxTokens;
        BasicBridge.url = defaultURL;
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
            String jsonInputString = "{\"model\": \"" + model + "\",\n" +
                    "\"system\": \"" + system + "\",\n" +
                    "\"options\": {\n\"num_predict\": " + maxTokens + "\n},\n" +
                    "\"stream\": false,\n" +
                    "\"prompt\": \"" + message + "\"}";
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
