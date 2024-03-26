package org.analysis;

import org.experiments.utility.SHA256Hash;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class ClassesResultsReader implements BaseResultReader {

    private final String fileNameToAnalyze;
    private String ParentClassName = "";
    private String childClassName = "";

    public ClassesResultsReader(String taskTypeUsed, String modelUsed, String ontologyUsed, String queryUsed, String systemUsed) {
        fileNameToAnalyze = SHA256Hash.sha256(taskTypeUsed + modelUsed + ontologyUsed + queryUsed + systemUsed);
    }

    @Override
    public void computeResults() {
        File file = new File("cache/" + fileNameToAnalyze + ".csv");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String str = new String(data, StandardCharsets.UTF_8);
            String charToSplit = ",";
            var strArr = str.split(charToSplit);
            strArr[1] = strArr[1].toLowerCase();
            if ((strArr[1].contains("true") || strArr[1].contains("yes"))) {
                strArr[0] = strArr[0].replace("?", "")
                        .replace("Is ", "")
                        .replace("a ", "")
                        .replace("subclass ", "")
                        .replace("of ", "").trim();
                ParentClassName = strArr[0].split(" ")[1];
                childClassName = strArr[0].split(" ")[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public String getFileNameToAnalyze() {
        return fileNameToAnalyze;
    }

    public String getParentClassName() {
        return ParentClassName;
    }

    public String getChildClassName() {
        return childClassName;
    }
}