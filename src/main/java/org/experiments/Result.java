package org.experiments;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Result {

    private final Map<String, String> queriesAndResponses = new HashMap<>();

    public Result(String filename) {
        FileReader reader = null;
        try {
            String filepath = "cache" + System.getProperty("file.separator") + filename + ".csv";
            reader = new FileReader(filepath);
            BufferedReader bufferedReader = new BufferedReader(reader);
            // for each line in the file, split the line into query and response and add it to the map
            bufferedReader.lines().forEach(line -> {
                String[] queryAndResponse = line.split(",");
                queriesAndResponses.put(queryAndResponse[0], queryAndResponse[1]);
            });
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getResponse(String query) {
        if (queriesAndResponses.containsKey(query)) {
            return queriesAndResponses.get(query);
        } else {
            throw new IllegalArgumentException("Query not found in the cache");
        }
    }

    public boolean isStrictlyTrue(String query) {
        String response = getResponse(query);
        return response.toLowerCase(Locale.ROOT).replace(".","").replace("\\n","").trim().equals("true");
    }

    public boolean isTrue(String query) {
        String response = getResponse(query);
        boolean containsTrue = response.toLowerCase(Locale.ROOT).contains("true");
        boolean containsFalse = response.toLowerCase(Locale.ROOT).contains("false");
        return containsTrue && !containsFalse;
    }

    public boolean isFalse(String query) {
        String response = getResponse(query);
        boolean containsTrue = response.toLowerCase(Locale.ROOT).contains("true");
        boolean containsFalse = response.toLowerCase(Locale.ROOT).contains("false");
        return !containsTrue && containsFalse;
    }

    public boolean isUnknown(String query) {
        return !isTrue(query) && !isFalse(query);
    }

}
