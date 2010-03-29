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

package org.apache.cayenne.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Interface defines API to check the status of Cayenne configuration.
 * 
 */
public class ConfigStatus {

    protected List<String> otherFailures = new ArrayList<String>();
    protected Map<String, String> failedMaps = new HashMap<String, String>();
    protected Map<String, String> failedAdapters = new HashMap<String, String>();
    protected Map<String, String> failedDataSources = new HashMap<String, String>();
    protected List<String> failedMapRefs = new ArrayList<String>();
    protected Map<String, Object> messages = new HashMap<String, Object>();

    public void addFailedMap(String name, String location, Object extraMessage) {
        failedMaps.put(name, location);
        if (extraMessage != null) {
            messages.put(getMapMessageKey(name, location), extraMessage);
        }
    }

    public void addFailedAdapter(String name, String location, String extraMessage) {
        failedAdapters.put(name, location);
        if (extraMessage != null) {
            messages.put(getAdapterMessageKey(name, location), extraMessage);
        }
    }

    public void addFailedDataSource(String name, String location, String extraMessage) {
        failedDataSources.put(name, location);
        if (extraMessage != null) {
            messages.put(getDataSourceMessageKey(name, location), extraMessage);
        }
    }

    public void addFailedMapRefs(String name, String extraMessage) {
        failedMapRefs.add(name);
        if (extraMessage != null) {
            messages.put(getMapRefMessageKey(name), extraMessage);
        }
    }

    protected String getMapMessageKey(String name, String location) {
        return "map:" + name + ":" + location;
    }

    protected String getAdapterMessageKey(String name, String location) {
        return "adapter:" + name + ":" + location;
    }

    protected String getDataSourceMessageKey(String name, String location) {
        return "dataSource:" + name + ":" + location;
    }

    protected String getMapRefMessageKey(String name) {
        return "map-ref:" + name;
    }

    /**
     * Returns a String description of failed configuration pieces. Returns a canned "no
     * failures" message if no failures occurred.
     */
    public String describeFailures() {
        if (!hasFailures()) {
            return "[No failures]";
        }

        StringBuilder buf = new StringBuilder();

        for (final String name : failedMaps.keySet()) {
            String location = failedMaps.get(name);
            Object message = messages.get(getMapMessageKey(name, location));
            buf.append("\n\tdomain.map.name=").append(name).append(
                    ", domain.map.location=").append(location);
            if (message != null) {
                buf.append(", reason: ").append(message);
            }
        }

        for (final String node : failedAdapters.keySet()) {
            String adapter = failedAdapters.get(node);
            Object message = messages.get(getAdapterMessageKey(node, adapter));
            buf.append("\n\tdomain.node.name=").append(node).append(
                    ", domain.node.adapter=").append(adapter);
            if (message != null) {
                buf.append(", reason: ").append(message);
            }
        }

        for (final String node : failedDataSources.keySet()) {
            String location = failedDataSources.get(node);
            Object message = messages.get(getDataSourceMessageKey(node, location));
            buf.append("\n\tdomain.node.name=").append(node).append(
                    ", domain.node.datasource=").append(location);
            if (message != null) {
                buf.append(", reason: ").append(message);
            }
        }

        for (final String mapName : failedMapRefs) {
            // don't report failed links if the DataMap itself failed to load
            if (failedMaps.get(mapName) == null) {
                buf.append("\n\tdomain.node.map-ref.name=").append(mapName);

                Object message = messages.get(getMapRefMessageKey(mapName));
                if (message != null) {
                    buf.append(", reason: ").append(message);
                }
            }
        }
        return buf.toString();
    }

    /**
     * Returns a list of error messages not directly associated with project objects, such
     * as XML parse exceptions, IOExceptions, etc.
     */
    public List<String> getOtherFailures() {
        return otherFailures;
    }

    /**
     * Returns a list of map reference names that failed to load.
     */
    public List<String> getFailedMapRefs() {
        return failedMapRefs;
    }

    /**
     * Returns a map of locations for names of the data maps that failed to load.
     */
    public Map<String, String> getFailedMaps() {
        return failedMaps;
    }

    /**
     * Returns a map of DataSource locations for node names that failed to load.
     */
    public Map<String, String> getFailedDataSources() {
        return failedDataSources;
    }

    /**
     * Returns a map of adapter classes for node names that failed to load.
     */
    public Map<String, String> getFailedAdapters() {
        return failedAdapters;
    }

    /**
     * Returns true if any of the "failed.." methods return true.
     */
    public boolean hasFailures() {
        return failedMaps.size() > 0
                || failedDataSources.size() > 0
                || failedAdapters.size() > 0
                || failedMapRefs.size() > 0
                || otherFailures.size() > 0;
    }
}
