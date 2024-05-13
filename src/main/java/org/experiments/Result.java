package org.experiments;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public class Result {

    private final String query;
    private final String response;

    public Result(String filename) {
        FileReader reader = null;
        try {
            String filepath = "cache" + System.getProperty("file.separator") + filename + ".csv";
            reader = new FileReader(filepath);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            query = line.split(",")[0];
            response = line.split(",")[1];
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Result(String query, String response) {
        this.query = query;
        this.response = response;
    }

    public boolean isStrictlyTrue() {
        return response.toLowerCase(Locale.ROOT).replace(".","").trim().equals("true");
    }

    public boolean isTrue() {
        boolean containsTrue = response.toLowerCase(Locale.ROOT).contains("true");
        boolean containsFalse = response.toLowerCase(Locale.ROOT).contains("false");
        return containsTrue && !containsFalse;
    }

    public boolean isFalse() {
        boolean containsTrue = response.toLowerCase(Locale.ROOT).contains("true");
        boolean containsFalse = response.toLowerCase(Locale.ROOT).contains("false");
        return !containsTrue && containsFalse;
    }

    public boolean isUnknown() {
        return !isTrue() && !isFalse();
    }

    public String getQuery() {
        return query;
    }

}
