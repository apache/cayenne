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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.ResourceLocator;

import java.lang.reflect.Method;
import java.net.URL;
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
			@Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
			@Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
			@Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
			@Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator) {
		super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator);
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
	 * @since 4.0
	 */
	@Override
	public SelectTranslator getSelectTranslator(SelectQuery<?> query, EntityResolver entityResolver) {
		return new Oracle8SelectTranslator(query, this, entityResolver);
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

	@Override
	public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
		QualifierTranslator translator = new Oracle8QualifierTranslator(queryAssembler);
		translator.setCaseInsensitive(caseInsensitiveCollations);
		return translator;
	}
}
