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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Looks up DataSource objects via JNDI.
 * 
 */
public class JNDIDataSourceFactory implements DataSourceFactory {

    private static final Log logger = LogFactory.getLog(JNDIDataSourceFactory.class);

    protected Configuration parentConfig;

    public void initializeWithParentConfiguration(Configuration conf) {
        this.parentConfig = conf;
    }

    /**
     * Attempts to load DataSource using JNDI. In case of failure tries to get the
     * DataSource with the same name from CayenneModeler preferences.
     */
    public DataSource getDataSource(String location) throws Exception {

        try {
            return loadViaJNDI(location);
        }
        catch (Exception ex) {

            logger.info("failed JNDI lookup, attempt to load "
                    + "from local preferences. Location key:"
                    + location);

            // failover to preferences loader to allow local development
            try {
                return loadFromPreferences(location);
            }
            catch (Exception preferencesException) {

                logger.info("failed loading from local preferences", Util
                        .unwindException(preferencesException));

                // giving up ... rethrow original exception...
                QueryLogger.logConnectFailure(ex);
                throw ex;
            }
        }
    }

    DataSource loadViaJNDI(String location) throws NamingException {
        QueryLogger.logConnect(location);

        Context initCtx = new InitialContext();
        DataSource ds;
        try {
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            ds = (DataSource) envCtx.lookup(location);
        }
        catch (NamingException namingEx) {
            // try looking up the location directly...
            ds = (DataSource) initCtx.lookup(location);
        }

        QueryLogger.logConnectSuccess();
        return ds;
    }

    DataSource loadFromPreferences(String location) throws Exception {
        // as we don't want compile dependencies on the Modeler, instantiate factory via
        // reflection ...

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        DataSourceFactory prefsFactory = (DataSourceFactory) Class
                .forName("org.apache.cayenne.modeler.pref.PreferencesDataSourceFactory", true, loader)
                .newInstance();

        prefsFactory.initializeWithParentConfiguration(parentConfig);
        return prefsFactory.getDataSource(location);
    }
}
