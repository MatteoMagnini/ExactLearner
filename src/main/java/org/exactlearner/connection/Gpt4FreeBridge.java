package org.exactlearner.connection;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Gpt4FreeBridge extends BasicBridge {

    private static final String model = "";
    private static final String url = "http://127.0.0.1:5500/?text=";

    public Gpt4FreeBridge() {
        super();
        BasicBridge.model = model;
        BasicBridge.url = url;
    }

    @Override
    public String ask(String message, String key, String system) {
        try {
            URL apiUrl = new URL(url + URLEncoder.encode(message, StandardCharsets.UTF_8));
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/html");
            connection.setDoOutput(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read and return the response
                var inputStream = connection.getInputStream();
                StringBuilder sb = new StringBuilder();
                for (int ch; (ch = inputStream.read()) != -1; ) {
                    sb.append((char) ch);
                }
                return sb.toString();
            } else {
                // Handle non-200 response codes
                System.out.println("HTTP Error: " + responseCode);
                // Optionally, read and print error response
                System.out.println("Error Response: " + connection.getErrorStream());
                return null;
            }
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
            return null;
        }
    }
}
