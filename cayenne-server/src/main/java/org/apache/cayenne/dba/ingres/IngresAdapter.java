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

package org.apache.cayenne.dba.ingres;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.access.translator.select.TrimmingQualifierTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.ResourceLocator;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for <a
 * href="http://opensource.ca.com/projects/ingres/">Ingres</a>. Sample
 * connection settings to use with Ingres are shown below:
 * 
 * <pre>
 *  ingres.jdbc.username = test
 *  ingres.jdbc.password = secret
 *  ingres.jdbc.url = jdbc:ingres://serverhostname:II7/cayenne
 *  ingres.jdbc.driver = ca.ingres.jdbc.IngresDriver
 * </pre>
 */
public class IngresAdapter extends JdbcAdapter {

	public static final String TRIM_FUNCTION = "TRIM";

	public IngresAdapter(@Inject RuntimeProperties runtimeProperties,
			@Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
			@Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
			@Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
			@Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator) {
		super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator);
		setSupportsUniqueConstraints(true);
		setSupportsGeneratedKeys(true);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public SelectTranslator getSelectTranslator(SelectQuery<?> query, EntityResolver entityResolver) {
		return new IngresSelectTranslator(query, this, entityResolver);
	}

	@Override
	public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
		return new TrimmingQualifierTranslator(queryAssembler, IngresAdapter.TRIM_FUNCTION);
	}

	@Override
	public SQLAction getAction(Query query, DataNode node) {
		return query.createSQLAction(new IngresActionBuilder(node));
	}

	@Override
	protected void configureExtendedTypes(ExtendedTypeMap map) {
		super.configureExtendedTypes(map);
		map.registerType(new IngresCharType());

		// configure boolean type to work with numeric columns
		map.registerType(new IngresBooleanType());
	}

	/**
	 * @see JdbcAdapter#createPkGenerator()
	 */
	@Override
	protected PkGenerator createPkGenerator() {
		return new IngresPkGenerator(this);
	}

	@Override
	public void bindParameter(PreparedStatement statement, Object object, int pos, int sqlType, int scale)
			throws SQLException, Exception {

		if (object == null && (sqlType == Types.BOOLEAN || sqlType == Types.BIT)) {
			statement.setNull(pos, Types.VARCHAR);
		} else {
			super.bindParameter(statement, object, pos, sqlType, scale);
		}
	}

	@Override
	public MergerFactory mergerFactory() {
		return new IngresMergerFactory();
	}

	@Override
	public void createTableAppendColumn(StringBuffer buf, DbAttribute at) {

		String[] types = externalTypesForJdbcType(at.getType());
		if (types == null || types.length == 0) {
			throw new CayenneRuntimeException("Undefined type for attribute '" + at.getEntity().getFullyQualifiedName()
					+ "." + at.getName() + "': " + at.getType());
		}

		String type = types[0];
		buf.append(quotingStrategy.quotedName(at)).append(' ').append(type);

		// append size and precision (if applicable)
		if (typeSupportsLength(at.getType())) {
			int len = at.getMaxLength();
			int scale = TypesMapping.isDecimal(at.getType()) ? at.getScale() : -1;

			// sanity check
			if (scale > len) {
				scale = -1;
			}

			if (len > 0) {
				buf.append('(').append(len);

				if (scale >= 0) {
					buf.append(", ").append(scale);
				}

				buf.append(')');
			}
		}

		if (at.isGenerated()) {
			buf.append(" GENERATED BY DEFAULT AS IDENTITY ");
		}

		// Ingres does not like "null" for non mandatory fields
		if (at.isMandatory()) {
			buf.append(" NOT NULL");
		}
	}
}
