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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Types;
import java.util.List;

/**
 * A flavor of OracleAdapter that implements workarounds for some old driver
 * limitations.
 * 
 * @since 1.2
 */
public class Oracle8Adapter extends OracleAdapter {

	private static Method outputStreamFromBlobMethod;
	private static Method writerFromClobMethod;

	static {
		initOracle8DriverInformation();
	}

	public Oracle8Adapter(@Inject RuntimeProperties runtimeProperties,
						  @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
						  @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
						  @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
						  @Inject(Constants.RESOURCE_LOCATOR) ResourceLocator resourceLocator,
						  @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
		super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);
	}

	private static void initOracle8DriverInformation() {
		initDone = true;

		// configure static information
		try {
			outputStreamFromBlobMethod = Class.forName("oracle.sql.BLOB").getMethod("getBinaryOutputStream");
			writerFromClobMethod = Class.forName("oracle.sql.CLOB").getMethod("getCharacterOutputStream");
		} catch (Throwable th) {
			// ignoring...
		}
	}

	static Method getWriterFromClobMethod() {
		return writerFromClobMethod;
	}

	static Method getOutputStreamFromBlobMethod() {
		return outputStreamFromBlobMethod;
	}

	/**
	 * Uses OracleActionBuilder to create the right action.
	 */
	@Override
	public SQLAction getAction(Query query, DataNode node) {
		return query.createSQLAction(new Oracle8ActionBuilder(node));
	}

	@Override
	protected URL findResource(String name) {

		if ("/types.xml".equals(name)) {
			name = "/types-oracle8.xml";
		}

		return super.findResource(name);
	}

	/**
	 * @since 5.0
	 */
	@Override
	public boolean typeSupportsScale(int type) {
		return type != Types.TIMESTAMP && super.typeSupportsScale(type);
	}
}
