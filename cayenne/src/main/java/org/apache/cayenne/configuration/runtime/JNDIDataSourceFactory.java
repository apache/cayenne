/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration.runtime;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locates DataSource mapped via JNDI.
 * 
 * @since 3.1
 * @deprecated since 5.0, unused by Cayenne
 */
@Deprecated(since = "5.0")
public class JNDIDataSourceFactory implements DataSourceFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(JNDIDataSourceFactory.class);

	@Override
	public DataSource getDataSource(DataNodeDescriptor nodeDescriptor) throws Exception {

		String location = getLocation(nodeDescriptor);

		try {
			return lookupViaJNDI(location);
		} catch (Exception e) {
			LOGGER.info("*** failed JNDI lookup of DataSource at location: " + location, e);
			throw e;
		}
	}

	protected String getLocation(DataNodeDescriptor nodeDescriptor) {
		String location = nodeDescriptor.getParameters();
		if (location == null) {
			throw new CayenneRuntimeException("Null 'location' for nodeDescriptor '%s'", nodeDescriptor.getName());
		}

		return location;
	}

	DataSource lookupViaJNDI(String location) throws NamingException {

		LOGGER.info("Connecting. JNDI path: " + location);

		Context context = new InitialContext();
		DataSource dataSource;
		try {
			Context envContext = (Context) context.lookup("java:comp/env");
			dataSource = (DataSource) envContext.lookup(location);
		} catch (NamingException namingEx) {
			// try looking up the location directly...
			dataSource = (DataSource) context.lookup(location);
		}

		LOGGER.info("Found JNDI DataSource at location: " + location);

		return dataSource;
	}

}
