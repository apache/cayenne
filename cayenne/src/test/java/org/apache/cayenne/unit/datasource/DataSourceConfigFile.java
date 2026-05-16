/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.unit.datasource;

import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class DataSourceConfigFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfigFile.class);

    private static final String PROPERTIES_FILE = "connection.properties";

    private static final String USER_NAME_KEY = "jdbc.username";
    private static final String PASSWORD_KEY = "jdbc.password";
    private static final String URL_KEY = "jdbc.url";
    private static final String DRIVER_KEY = "jdbc.driver";

    public static Map<String, DataSourceDescriptor> load() {

        File cayenneDir = new File(new File(System.getProperty("user.home")), ".cayenne");
        File file = new File(cayenneDir, PROPERTIES_FILE);
        if (!file.exists()) {
            return Map.of();
        }

        Properties properties = loadProperties(file);
        Map<String, DataSourceDescriptor> descriptors = new HashMap<>();
        descriptorNames(properties).forEach(n -> descriptors.put(n, buildDataSourceDescriptor(n, properties)));

        return descriptors;
    }

    private static Properties loadProperties(File file) {

        LOGGER.info("Loading DataSource configurations from {}", file.getAbsolutePath());

        Properties properties = new Properties();

        try (FileReader r = new FileReader(file)) {
            properties.load(r);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private static DataSourceDescriptor buildDataSourceDescriptor(String name, Properties props) {
        DataSourceDescriptor dsi = new DataSourceDescriptor();

        dsi.setUserName(props.getProperty(name + "." + USER_NAME_KEY));
        dsi.setPassword(props.getProperty(name + "." + PASSWORD_KEY));
        dsi.setDataSourceUrl(props.getProperty(name + "." + URL_KEY));
        dsi.setJdbcDriver(props.getProperty(name + "." + DRIVER_KEY));
        dsi.setMinConnections(DataSourceConfigLoader.MIN_CONNECTIONS);
        dsi.setMaxConnections(DataSourceConfigLoader.MAX_CONNECTIONS);

        return dsi;
    }

    private static Set<String> descriptorNames(Properties props) {
        Iterator<?> it = props.keySet().iterator();
        Set<String> names = new HashSet<>();

        while (it.hasNext()) {
            String key = (String) it.next();

            int dotInd = key.indexOf('.');
            if (dotInd <= 0) {
                continue;
            }

            names.add(key.substring(0, dotInd));
        }

        return names;
    }
}
