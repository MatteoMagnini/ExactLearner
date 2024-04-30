package org.utility;

import org.configurations.Configuration;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class YAMLConfigLoader {
    public Configuration getConfig(String args, Class<Configuration> configuration) {
        try {
            return new Yaml().loadAs(new FileInputStream(args), configuration);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
