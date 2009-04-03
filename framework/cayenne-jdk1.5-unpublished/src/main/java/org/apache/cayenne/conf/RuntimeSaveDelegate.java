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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;

/**
 * Save delegate used for saving Cayenne access stack.
 */
public class RuntimeSaveDelegate implements ConfigSaverDelegate {

    protected Configuration config;

    public RuntimeSaveDelegate(Configuration config) {
        this.config = config;
    }

    /**
     * Constructor for RuntimeSaveDelegate.
     */
    public RuntimeSaveDelegate() {
        super();
    }

    protected DataDomain findDomain(String domainName) {
        DataDomain domain = config.getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Can't find DataDomain: " + domainName);
        }

        return domain;
    }

    protected DataNode findNode(String domainName, String nodeName) {
        DataDomain domain = findDomain(domainName);
        DataNode node = domain.getNode(nodeName);
        if (node == null) {
            throw new IllegalArgumentException("Can't find DataNode: "
                    + domainName
                    + "."
                    + nodeName);
        }

        return node;
    }

    /**
     * @since 1.1
     */
    public String projectVersion() {
        return config.getProjectVersion();
    }

    public Iterator domainNames() {
        Transformer tr = new Transformer() {

            public Object transform(Object input) {
                return ((DataDomain) input).getName();
            }
        };
        return new TransformIterator(config.getDomains().iterator(), tr);
    }

    /**
     * @since 1.1
     */
    public Iterator<String> viewNames() {
        return config.getDataViewLocations().keySet().iterator();
    }

    /**
     * @since 1.1
     */
    public String viewLocation(String dataViewName) {
        return config.getDataViewLocations().get(dataViewName);
    }

    public Iterator propertyNames(String domainName) {
        return findDomain(domainName).getProperties().keySet().iterator();
    }

    public String propertyValue(String domainName, String propertyName) {
        return (String) findDomain(domainName).getProperties().get(propertyName);
    }

    public String mapLocation(String domainName, String mapName) {
        return findDomain(domainName).getMap(mapName).getLocation();
    }

    public Iterator mapNames(String domainName) {

        // sort maps by name
        List<DataMap> maps = new ArrayList<DataMap>(findDomain(domainName).getDataMaps());
        Collections.sort(maps, new Comparator() {

            public int compare(Object o1, Object o2) {
                String name1 = (o1 != null) ? ((DataMap) o1).getName() : null;
                String name2 = (o1 != null) ? ((DataMap) o2).getName() : null;
                return Util.nullSafeCompare(true, name1, name2);
            }
        });

        Transformer tr = new Transformer() {

            public Object transform(Object input) {
                return ((DataMap) input).getName();
            }
        };
        return new TransformIterator(maps.iterator(), tr);
    }

    public String nodeAdapterName(String domainName, String nodeName) {
        DbAdapter adapter = findNode(domainName, nodeName).getAdapter();
        return (adapter != null && adapter.getClass() != AutoAdapter.class) ? adapter
                .getClass()
                .getName() : null;
    }

    public String nodeDataSourceName(String domainName, String nodeName) {
        return findNode(domainName, nodeName).getDataSourceLocation();
    }

    public String nodeFactoryName(String domainName, String nodeName) {
        return findNode(domainName, nodeName).getDataSourceFactory();
    }

    /**
     * @since 3.0
     */
    public String nodeSchemaUpdateStrategyName(String domainName, String nodeName) {
        return findNode(domainName, nodeName).getSchemaUpdateStrategyName();
    }

    public Iterator nodeNames(String domainName) {
        Transformer tr = new Transformer() {

            public Object transform(Object input) {
                return ((DataNode) input).getName();
            }
        };

        // sort nodes by name
        List<DataNode> nodes = new ArrayList<DataNode>(findDomain(domainName)
                .getDataNodes());
        Collections.sort(nodes, new Comparator() {

            public int compare(Object o1, Object o2) {
                String name1 = (o1 != null) ? ((DataNode) o1).getName() : null;
                String name2 = (o1 != null) ? ((DataNode) o2).getName() : null;
                return Util.nullSafeCompare(true, name1, name2);
            }
        });

        return new TransformIterator(nodes.iterator(), tr);
    }

    public Iterator linkedMapNames(String domainName, String nodeName) {
        Transformer tr = new Transformer() {

            public Object transform(Object input) {
                return ((DataMap) input).getName();
            }
        };

        Collection<DataMap> maps = findNode(domainName, nodeName).getDataMaps();
        return new TransformIterator(maps.iterator(), tr);
    }
}
