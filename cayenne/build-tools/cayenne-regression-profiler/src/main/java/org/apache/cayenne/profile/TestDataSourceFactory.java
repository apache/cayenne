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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.ConnectionProperties;
import org.apache.cayenne.conf.DataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolManager;

/**
 * A DataSourceFactory that loads connection info from connection properties, the same way
 * unit tests do.
 * 
 */
public class TestDataSourceFactory implements DataSourceFactory {

    // same as the one used in unit tests
    public static final String CONNECTION_SET_PROPERTY = "cayenne.test.connection";
    public static final String CONNECTION_POOL_MIN_SIZE_PROPERTY = "cayenne.test.pool.min";
    public static final String CONNECTION_POOL_MAX_SIZE_PROPERTY = "cayenne.test.pool.max";

    public static String getDataSourceName() {
        String connectionSet = System.getProperty(CONNECTION_SET_PROPERTY);
        if (connectionSet == null) {
            connectionSet = ConnectionProperties.EMBEDDED_DATASOURCE;
        }

        return connectionSet;
    }

    public DataSource getDataSource(String location) throws Exception {

        String connectionSet = getDataSourceName();
        DataSourceInfo dsi = ConnectionProperties.getInstance().getConnectionInfo(
                connectionSet);

        if (dsi == null) {
            throw new CayenneRuntimeException("Connection info for key '"
                    + connectionSet
                    + "' is not configured");
        }

        String minPool = System.getProperty(CONNECTION_POOL_MIN_SIZE_PROPERTY);
        if (minPool != null) {
            dsi.setMinConnections(Integer.parseInt(minPool));
        }

        String maxPool = System.getProperty(CONNECTION_POOL_MAX_SIZE_PROPERTY);
        if (maxPool != null) {
            dsi.setMaxConnections(Integer.parseInt(maxPool));
        }

        if (dsi.getMinConnections() > dsi.getMaxConnections()) {
            dsi.setMaxConnections(dsi.getMinConnections());
        }

        return new PoolManager(dsi.getJdbcDriver(), dsi.getDataSourceUrl(), dsi
                .getMinConnections(), dsi.getMaxConnections(), dsi.getUserName(), dsi
                .getPassword());
    }

    public void initializeWithParentConfiguration(Configuration conf) {
    }
}
