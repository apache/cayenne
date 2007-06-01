/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.profile;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolManager;

/**
 * A DataSourceFactory that loads connection info from connection properties, the same way
 * unit tests do.
 * 
 * @author Andrus Adamchik
 */
public class TestDataSourceFactory implements DataSourceFactory {

    // same as the one used in unit tests
    public static final String CONNECTION_SET_PROPERTY = "cayenne.test.connection";

    public static DataSourceInfo getDataSourceInfo() {
        String connectionSet = getDataSourceName();
        return ConnectionProperties.getInstance().getConnectionInfo(connectionSet);
    }

    public static String getDataSourceName() {
        String connectionSet = System.getProperty(CONNECTION_SET_PROPERTY);
        if (connectionSet == null) {
            connectionSet = ConnectionProperties.EMBEDDED_DATASOURCE;
        }

        return connectionSet;
    }

    public DataSource getDataSource(String location) throws Exception {

        DataSourceInfo dsi = getDataSourceInfo();

        if (dsi == null) {
            throw new CayenneRuntimeException("Connection info for key '"
                    + getDataSourceName()
                    + "' is not configured");
        }

        return new PoolManager(dsi.getJdbcDriver(), dsi.getDataSourceUrl(), dsi
                .getMinConnections(), dsi.getMaxConnections(), dsi.getUserName(), dsi
                .getPassword());
    }

    /**
     * @deprecated since 1.2 super is deprecated
     */
    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        return getDataSource(location);
    }

    public void initializeWithParentConfiguration(Configuration conf) {
    }
}
