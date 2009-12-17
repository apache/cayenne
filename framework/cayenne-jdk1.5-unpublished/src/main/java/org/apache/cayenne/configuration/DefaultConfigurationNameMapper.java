/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;

/**
 * @since 3.1
 */
public class DefaultConfigurationNameMapper implements ConfigurationNameMapper {

    private static final String CAYENNE_PREFIX = "cayenne-";
    private static final String CAYENNE_SUFFIX = ".xml";

    private static final String DATA_MAP_SUFFIX = ".map.xml";

    protected ConfigurationNodeVisitor<String> nameMapper;

    public DefaultConfigurationNameMapper() {
        nameMapper = new NameMapper();
    }

    public String configurationLocation(ConfigurationNode node) {
        return node.acceptVisitor(nameMapper);
    }

    public String configurationLocation(
            Class<? extends ConfigurationNode> type,
            String name) {
        if (DataChannelDescriptor.class.isAssignableFrom(type)) {
            return getDataChannelName(name);
        }
        else if (DataMap.class.isAssignableFrom(type)) {
            return getDataMapName(name);
        }

        throw new IllegalArgumentException("Unrecognized configuration type: "
                + type.getName());
    }

    public String configurationNodeName(
            Class<? extends ConfigurationNode> type,
            Resource resource) {

        String path = resource.getURL().getPath();
        if (path == null || path.length() == 0) {
            return null;
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {

            if (lastSlash == path.length() - 1) {
                return null;
            }

            path = path.substring(lastSlash + 1);

        }

        if (DataChannelDescriptor.class.isAssignableFrom(type)) {
            if (!path.startsWith(CAYENNE_PREFIX) || !path.endsWith(CAYENNE_SUFFIX)) {
                return null;
            }

            return path.substring(CAYENNE_PREFIX.length(), path.length()
                    - CAYENNE_SUFFIX.length());
        }
        else if (DataMap.class.isAssignableFrom(type)) {
            if (!path.endsWith(DATA_MAP_SUFFIX)) {
                return null;
            }
            return path.substring(0, path.length() - DATA_MAP_SUFFIX.length());
        }

        throw new IllegalArgumentException("Unrecognized configuration type: "
                + type.getName());
    }

    protected String getDataChannelName(String name) {
        if (name == null) {
            throw new NullPointerException("Null DataChannelDescriptor name");
        }

        return CAYENNE_PREFIX + name + CAYENNE_SUFFIX;
    }

    protected String getDataMapName(String name) {
        if (name == null) {
            throw new NullPointerException("Null DataMap name");
        }

        return name + DATA_MAP_SUFFIX;
    }

    final class NameMapper extends BaseConfigurationNodeVisitor<String> {

        @Override
        public String visitDataChannelDescriptor(DataChannelDescriptor descriptor) {
            return getDataChannelName(descriptor.getName());
        }

        @Override
        public String visitDataMap(DataMap dataMap) {
            return getDataMapName(dataMap.getName());
        }
    }
}
