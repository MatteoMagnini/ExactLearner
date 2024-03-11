package org.exactlearner.connection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChatGPTBridge extends BasicBridge{

    public static String model = "gpt-3.5-turbo";
    public static String url = "https://api.openai.com/v1/chat/completions";
    private static final int startJSONResponseOffset = 11;

    public boolean checkConnection(String ip, int port, String key) {
        return checkConnection(url, key);
    }

    public boolean checkConnection(String stringURL, String key) {
        try {
            HttpURLConnection connection = getConnection(stringURL);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Content-Type", "application/json");
            int code = connection.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String ask(String message, String key) {
        try {
            HttpURLConnection connection = getConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Content-Type", "application/json");
            String jsonInputString = "{\"model\": \"" + ChatGPTBridge.model + "\", \"messages\": [{\"role\": \"system\", \"content\": \"" + message + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(jsonInputString);
            writer.flush();
            writer.close();
            String jsonResponse = getChatGPTResponse(connection);
            return extractMessageFromJSON(jsonResponse);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
            return "";
        }
    }

    private HttpURLConnection getConnection(String stringURL) throws Exception {
        URL url = getURL(stringURL);
        return (HttpURLConnection) url.openConnection();
    }

    private String getChatGPTResponse(HttpURLConnection connection) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        return response.toString();
    }

    private String extractMessageFromJSON(String json) {
        int start = json.indexOf("text") + startJSONResponseOffset;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
