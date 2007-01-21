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
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;

/**
 * Factory of DataSource objects used by the project model. Always tries to locate file
 * with direct connection info.
 * 
 * @author Andrus Adamchik
 */
public class ProjectDataSourceFactory extends DriverDataSourceFactory {

    protected File projectDir;

    public ProjectDataSourceFactory(File projectDir) throws Exception {
        super();
        this.projectDir = projectDir;
    }

    public DataSource getDataSource(String location) throws Exception {
        try {
            this.load(location);
        }
        catch (ConfigurationException e) {
            // ignoring
        }

        return new ProjectDataSource(this.getDriverInfo());
    }

    protected DataSourceInfo getDriverInfo() {
        DataSourceInfo temp = super.getDriverInfo();
        if (null == temp) {
            temp = new DataSourceInfo();
        }

        return temp;
    }

}
