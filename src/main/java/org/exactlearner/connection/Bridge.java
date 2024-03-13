package org.exactlearner.connection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

enum ChatGPTCodes {
    MALFORMED_URL(0),
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

}

public interface Bridge {

    boolean checkConnection(String ip, int port);

    boolean checkConnection(String stringURL);

    String ask(String message, String key);

    URL getURL(String ip, int port) throws MalformedURLException;

    URL getURL(String stringURL) throws MalformedURLException;

}


abstract class BasicBridge implements Bridge {

    static String model = "";
    static String url = "";
    private static final int startJSONResponseOffset = 11;

    public URL getURL(String ip, int port) throws MalformedURLException {
        return new URL(ip.concat(":".concat(String.valueOf(port))));
    }

    public URL getURL(String stringURL) throws MalformedURLException {
        return new URL(stringURL);
    }

    public boolean checkConnection(String stringURL) {
        try {
            HttpURLConnection connection = getConnection(stringURL);
            connection.connect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return true;
    }

    public boolean checkConnection(String ip, int port) {
        return checkConnection(url);
    }

    public boolean checkConnection() {
        return checkConnection(url);
    }


    public HttpURLConnection getConnection(String stringURL) throws Exception {
        URL url = getURL(stringURL);
        return (HttpURLConnection) url.openConnection();
    }


    public String getChatGPTResponse(HttpURLConnection connection) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        return response.toString();
    }

    public String extractMessageFromJSON(String json) {
        int start = json.indexOf("text") + startJSONResponseOffset;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    public int extractErrorCode(String error) {
        //The error number is the first number after the string "code:" and then take the first integer
        if (!error.contains("code: ")) {
            return 0;
        } else {
            int start = error.indexOf("code: ") + 6;
            int end = error.indexOf(" ", start);
            return Integer.parseInt(error.substring(start, end));
        }
    }
}
