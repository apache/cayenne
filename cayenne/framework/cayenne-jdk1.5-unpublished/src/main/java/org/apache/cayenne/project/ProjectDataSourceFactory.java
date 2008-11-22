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

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DataSourceFactory;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;

/**
 * Factory of DataSource objects used by the project model. Always tries to locate file
 * with direct connection info.
 * 
 */
public class ProjectDataSourceFactory implements DataSourceFactory {

    protected File projectDir;
    protected boolean loadFromXML;
    protected Configuration parentConfiguration;

    public ProjectDataSourceFactory(File projectDir) {
        this.projectDir = projectDir;
    }

    public ProjectDataSourceFactory(File projectDir, boolean loadFromXML) {
        this.projectDir = projectDir;
        this.loadFromXML = loadFromXML;
    }

    public void initializeWithParentConfiguration(Configuration parentConfiguration) {
        this.parentConfiguration = parentConfiguration;
    }

    public DataSource getDataSource(String location) throws Exception {
        return new ProjectDataSource(getDriverInfo(location));
    }

    /**
     * Returns a {@link DataSourceInfo} object, loading it from XML file if the factory is
     * configured to do so.
     */
    protected DataSourceInfo getDriverInfo(String location) throws Exception {
        DataSourceInfo info = null;

        if (loadFromXML) {
            try {
                info = new XMLConfigLoader().loadDriverInfo(location);
            }
            catch (ConfigurationException e) {
                // ignoring...
            }
        }

        return info != null ? info : new DataSourceInfo();
    }

    // a helper class that exposes non public methods of DriverDataSourceFactory to load
    // DS XML
    final class XMLConfigLoader extends DriverDataSourceFactory {

        XMLConfigLoader() throws Exception {
            super();
            initializeWithParentConfiguration(ProjectDataSourceFactory.this.parentConfiguration);
        }

        DataSourceInfo loadDriverInfo(String location) throws Exception {
            load(location);
            return driverInfo;
        }
    }
}
