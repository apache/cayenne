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

package org.apache.cayenne.project;

import java.io.File;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;

/**
 * Stores information necessary to reconfigure nodes of existing projects.
 * 
 * @deprecated since 3.0. {@link ProjectConfigurator} approach turned out to be not
 *             usable, and is in fact rarely used (if ever). It will be removed in
 *             subsequent releases.
 */
public class DataNodeConfigInfo {

    protected String name;
    protected String domain;
    protected String adapter;
    protected String dataSource;
    protected File driverFile;

    /**
     * Searches for the DataNode described by this DataNodeConfigInfo in the provided
     * configuration object. Throws ProjectException if there is no matching DataNode.
     */
    public DataNode findDataNode(Configuration config) throws ProjectException {
        DataDomain domainObj = null;

        // domain name is either explicit, or use default domain
        if (domain != null) {
            domainObj = config.getDomain(domain);

            if (domainObj == null) {
                throw new ProjectException("Can't find domain named " + domain);
            }
        }
        else {
            try {
                domainObj = config.getDomain();
            }
            catch (Exception ex) {
                throw new ProjectException("Project has no default domain.", ex);
            }

            if (domainObj == null) {
                throw new ProjectException("Project has no domains configured.");
            }
        }

        DataNode node = domainObj.getNode(name);
        if (node == null) {
            throw new ProjectException("Domain "
                    + domainObj.getName()
                    + " has no node named '"
                    + name
                    + "'.");
        }
        return node;
    }

    /**
     * Returns the adapter.
     * 
     * @return String
     */
    public String getAdapter() {
        return adapter;
    }

    /**
     * Returns the dataSource.
     * 
     * @return String
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Returns the domain.
     * 
     * @return String
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the driverFile.
     * 
     * @return File
     */
    public File getDriverFile() {
        return driverFile;
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the adapter.
     * 
     * @param adapter The adapter to set
     */
    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    /**
     * Sets the dataSource.
     * 
     * @param dataSource The dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Sets the domain.
     * 
     * @param domain The domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Sets the driverFile.
     * 
     * @param driverFile The driverFile to set
     */
    public void setDriverFile(File driverFile) {
        this.driverFile = driverFile;
    }

    /**
     * Sets the name.
     * 
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

}
