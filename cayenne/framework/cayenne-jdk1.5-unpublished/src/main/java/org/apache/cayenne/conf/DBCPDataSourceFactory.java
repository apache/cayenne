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

import org.apache.cayenne.util.ResourceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 */
public class DBCPDataSourceFactory implements DataSourceFactory {

    private static final Log logger = LogFactory.getLog(DBCPDataSourceFactory.class);

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
     * Creates and returns a {{org.apache.commons.dbcp.PoolingDataSource}} instance.
     */
    public DataSource getDataSource(String location) throws Exception {

        ResourceFinder resourceFinder;

        if (parentConfiguration != null) {
            resourceFinder = parentConfiguration.getResourceFinder();
        }
        else {
            ResourceLocator resourceLocator = new ResourceLocator();
            resourceLocator.setSkipAbsolutePath(false);
            resourceLocator.setSkipHomeDirectory(true);
            resourceLocator.setSkipClasspath(false);
            resourceLocator.setSkipCurrentDirectory(false);
            resourceFinder = resourceLocator;
        }

        DBCPDataSourceProperties properties = new DBCPDataSourceProperties(
                resourceFinder,
                location);

        if (logger.isDebugEnabled()) {
            logger.debug("DBCP Properties: " + properties.getProperties());
        }

        DBCPDataSourceBuilder builder = new DBCPDataSourceBuilder(properties);
        return builder.createDataSource();
    }
}
