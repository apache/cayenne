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

package org.apache.cayenne.modeler.action;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.RuntimeLoadDelegate;
import org.apache.cayenne.modeler.util.ModelerDbAdapter;

/**
 * Project loader delegate customized for use in CayenneModeler.
 * 
 * @since 1.2
 */
class ModelerProjectLoadDelegate extends RuntimeLoadDelegate {

    public ModelerProjectLoadDelegate(Configuration configuration) {
        super(configuration, configuration.getLoadStatus());
    }

    protected void updateDefaults(DataDomain domain) {
        // do nothing...
    }

    protected void initAdapter(DataNode node, String adapterName) {
        node.setAdapter(new ModelerDbAdapter(adapterName, node.getDataSource()));
    }

    public void shouldLoadDataDomain(String domainName) {
        super.shouldLoadDataDomain(domainName);

        try {
            // disable class indexing
            findDomain(domainName).getEntityResolver().setIndexedByClass(false);
        }
        catch (Exception ex) {
            throw new ConfigurationException("Domain is not loaded: " + domainName);
        }
    }

    public void shouldLoadDataDomainProperties(
            String domainName,
            Map<String, String> properties) {

        // remove factory property to avoid instantiation attempts for unknown/invalid
        // classes

        Map<String, String> propertiesClone = new HashMap<String, String>(properties);
        String dataContextFactory = propertiesClone
                .remove(DataDomain.DATA_CONTEXT_FACTORY_PROPERTY);

        super.shouldLoadDataDomainProperties(domainName, propertiesClone);

        // stick property back in...
        if (dataContextFactory != null) {
            try {
                findDomain(domainName).getProperties().put(
                        DataDomain.DATA_CONTEXT_FACTORY_PROPERTY,
                        dataContextFactory);
            }
            catch (Exception ex) {
                throw new ConfigurationException("Domain is not loaded: " + domainName);
            }
        }
    }

    @Override
    public void finishedLoading() {
        // execute a simplified version of the super method to avoid runtime relationships
        // and other runtime artifacts...
        // load missing relationships and update configuration object
        for (DataDomain domain : getDomains().values()) {
            config.addDomain(domain);
        }
    }

    /**
     * Creates a subclass of the DataNode that does not decorate its DataSource, exposing
     * the version that was set on it.
     */
    protected DataNode createDataNode(String nodeName) {
        return new DataNode(nodeName) {

            public DataSource getDataSource() {
                return dataSource;
            }
        };
    }
}
