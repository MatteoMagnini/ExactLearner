package org.exactlearner.connection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

enum ChatGPTCodes {
    OK(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private static final Map<Integer, ChatGPTCodes> map = new HashMap<>(values().length, 1);

    static {
        for (ChatGPTCodes c : values()) map.put(c.code, c);
    }

    private final int code;

    private ChatGPTCodes(int code) {
        this.code = code;
    }

    public static ChatGPTCodes valueOf(int code) {
        return map.get(code);
    }

    public int code() {
        return code;
    }
}

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
            connection.connect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return true;
    }

    public boolean checkConnection(String key) {
        return checkConnection(url, key);
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
            System.out.println(ChatGPTCodes.valueOf(extractErrorCode(e.getMessage())));
            return null;
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

    private int extractErrorCode(String error) {
        //The error number is the first number after the string "code:" and then take the first integer
        int start = error.indexOf("code: ") + 6;
        int end = error.indexOf(" ", start);
        return Integer.parseInt(error.substring(start, end));
    }
}
