package org.apache.cayenne.gen;

import java.util.ArrayList;
import java.util.List;

public class CgenConfigList {

    public static final String DEFAULT_CONFIG_NAME = "Default";
    private final List<CgenConfiguration> configurations;

    public CgenConfigList() {
        this.configurations = new ArrayList<>();
    }

    public CgenConfiguration getByName(String name) {
        return configurations.stream().filter(config -> config.getName().equals(name)).findFirst().orElse(null);
    }

    public void removeByName(String name) {
        configurations.removeIf(configuration -> configuration.getName().equals(name));
    }

    public void add(CgenConfiguration configuration) {
        configurations.add(configuration);
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        configurations.forEach(configuration -> names.add(configuration.getName()));
        return names;
    }

    public List<CgenConfiguration> getAll() {
        return configurations;
    }

    public boolean isExist(String name) {
        return getNames().contains(name);
    }
}
