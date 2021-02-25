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

package org.apache.cayenne.dba.sqlserver;

import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * <p>
 * Cayenne DbAdapter implementation for <a
 * href="http://www.microsoft.com/sql/">Microsoft SQL Server </a> engine.
 * </p>
 * <h3>Microsoft Driver Settings</h3>
 * <p>
 * Sample connection settings to use with MS SQL Server are shown below:
 *
 * <pre>
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:sqlserver://192.168.0.65;databaseName=cayenne;SelectMethod=cursor
 *       sqlserver.jdbc.driver = com.microsoft.sqlserver.jdbc.SQLServerDriver
 * </pre>
 * <p>
 * <i>Note on case-sensitive LIKE: if your application requires case-sensitive
 * LIKE support, ask your DBA to configure the database to use a case-senstitive
 * collation (one with "CS" in symbolic collation name instead of "CI", e.g.
 * "SQL_Latin1_general_CP1_CS_AS"). </i>
 * </p>
 * <h3>jTDS Driver Settings</h3>
 * <p>
 * jTDS is an open source driver that can be downloaded from <a href=
 * "http://jtds.sourceforge.net">http://jtds.sourceforge.net </a>. It supports
 * both SQLServer and Sybase. Sample SQLServer settings are the following:
 * </p>
 *
 * <pre>
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:jtds:sqlserver://192.168.0.65/cayenne
 *       sqlserver.jdbc.driver = net.sourceforge.jtds.jdbc.Driver
 * </pre>
 *
 * @since 1.1
 */
public class SQLServerAdapter extends SybaseAdapter {

	/**
	 * Stores the major version of the database.
	 * Database versions 12 and higher supports the use of LIMIT,lower versions use TOP N.
	 *
	 * @since 4.2
	 */
	private Integer version;

	/**
	 * @deprecated since 4.2 unused
	 */
	@Deprecated
	public static final String TRIM_FUNCTION = "RTRIM";

	private final List<String> SYSTEM_SCHEMAS = Arrays.asList(
			"db_accessadmin", "db_backupoperator",
			"db_datareader", "db_datawriter", "db_ddladmin", "db_denydatareader",
			"db_denydatawriter","dbo", "sys", "db_owner", "db_securityadmin", "guest",
			"INFORMATION_SCHEMA"
	);

	public SQLServerAdapter(@Inject RuntimeProperties runtimeProperties,
							@Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
							@Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
							@Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
							@Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator,
							@Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
		super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);

		this.setSupportsBatchUpdates(true);
	}

    /**
     * Not supported, see: <a href="https://github.com/microsoft/mssql-jdbc/issues/245">mssql-jdbc #245</a>
     */
	@Override
	public boolean supportsGeneratedKeysForBatchInserts() {
		return false;
	}
	
	/**
	 * @since 4.2
	 */
	@Override
	public SQLTreeProcessor getSqlTreeProcessor() {
		if(getVersion() != null && getVersion() >= 12) {
			return new SQLServerTreeProcessorV12();
		}
		return new SQLServerTreeProcessor();
	}

	/**
	 * Uses SQLServerActionBuilder to create the right action.
	 *
	 * @since 1.2
	 */
	@Override
	public SQLAction getAction(Query query, DataNode node) {
		return query.createSQLAction(new SQLServerActionBuilder(node, getVersion()));
	}

	@Override
	public List<String> getSystemSchemas() {
		return SYSTEM_SCHEMAS;
	}

	public Integer getVersion() {
		return version;
	}

	/**
	 * @since 4.2
	 * @param version of the server as provided by the JDBC driver
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}
}
