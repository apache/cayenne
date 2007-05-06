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

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A DataSourceFactory that creates a connection pool based on Apache Jakarta <a
 * href="http://jakarta.apache.org/commons/dbcp/">commons-dbcp</a>. If you are using this
 * factory, commons-pool and commons-dbcp jars must be present in runtime. <p/>
 * DBCPDataSourceFactory can be selected in the Modeler for a DataNode. DBCP pool
 * configuration is done via a properties file that is specified in the modeler. See this
 * <a href="http://cwiki.apache.org/CAYDOC/DBCPDataSourceFactory">wiki page</a> for the
 * list of supported properties.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class DBCPDataSourceFactory implements DataSourceFactory {

    private static final Logger logger = Logger.getLogger(DBCPDataSourceFactory.class);

    /**
     * @deprecated since 2.0 - this information is now private.
     */
    public static final String PROPERTY_PREFIX = "cayenne.dbcp.";

    /**
     * @deprecated since 2.0 - this information is now private.
     */
    public static final String PS_PROPERTY_PREFIX = PROPERTY_PREFIX + "ps.";

    protected Configuration parentConfiguration;

    /**
     * Stores parent configuration in an ivar, using it later to resolve resources.
     */
    public void initializeWithParentConfiguration(Configuration parentConfiguration) {
        this.parentConfiguration = parentConfiguration;
    }
    
    /**
     * @deprecated since 1.2
     */
    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        return getDataSource(location);
    }

    /**
     * Creates and returns a {{org.apache.commons.dbcp.PoolingDataSource}} instance.
     */
    public DataSource getDataSource(String location) throws Exception {

        DBCPDataSourceProperties properties = new DBCPDataSourceProperties(
                parentConfiguration.getResourceLocator(),
                location);

        if (logger.isDebugEnabled()) {
            logger.debug("DBCP Properties: " + properties.getProperties());
        }

        DBCPDataSourceBuilder builder = new DBCPDataSourceBuilder(properties);
        return builder.createDataSource();
    }
}
