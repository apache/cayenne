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
package org.apache.cayenne.configuration.server;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Locates DataSource mapped via JNDI.
 * 
 * @since 3.1
 */
public class JNDIDataSourceFactory implements DataSourceFactory {

    private static final Log logger = LogFactory.getLog(JNDIDataSourceFactory.class);
    
    @Inject
    protected JdbcEventLogger jdbcEventLogger;

    @Override
    public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

        String location = nodeDescriptor.getParameters();
        if (location == null) {
            throw new CayenneRuntimeException(
                    "Null 'location' for nodeDescriptor '%s'",
                    nodeDescriptor.getName());
        }

        try {
            return lookupViaJNDI(location);
        }
        catch (Exception ex) {
            logger.info("failed JNDI lookup of DataSource location '" + location + "'");
            jdbcEventLogger.logConnectFailure(ex);
            throw ex;
        }
    }

    DataSource lookupViaJNDI(String location) throws NamingException {
        jdbcEventLogger.logConnect(location);

        Context context = new InitialContext();
        DataSource dataSource;
        try {
            Context envContext = (Context) context.lookup("java:comp/env");
            dataSource = (DataSource) envContext.lookup(location);
        }
        catch (NamingException namingEx) {
            // try looking up the location directly...
            dataSource = (DataSource) context.lookup(location);
        }

        jdbcEventLogger.logConnectSuccess();
        return dataSource;
    }

}
