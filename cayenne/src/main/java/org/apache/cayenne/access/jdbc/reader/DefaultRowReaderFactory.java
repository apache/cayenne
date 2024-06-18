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
package org.apache.cayenne.access.jdbc.reader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.reader.DataRowPostProcessor.ColumnOverride;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 4.0
 */
public class DefaultRowReaderFactory implements RowReaderFactory {

	@Override
	public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
			Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {

		PostprocessorFactory postProcessorFactory = new PostprocessorFactory(descriptor, queryMetadata,
				adapter.getExtendedTypes(), attributeOverrides);

		List<Object> rsMapping = queryMetadata.getResultSetMapping();
		if (rsMapping == null) {
			return createFullRowReader(descriptor, queryMetadata, postProcessorFactory);
		}

		int resultWidth = rsMapping.size();
		if (resultWidth == 0) {
			throw new CayenneRuntimeException("Empty result descriptor");
		}

		if (queryMetadata.isSingleResultSetMapping()) {

			Object segment = rsMapping.get(0);

			if (segment instanceof EntityResultSegment) {
				return createEntityRowReader(descriptor, queryMetadata, (EntityResultSegment) segment,
						postProcessorFactory);
			} else if (segment instanceof EmbeddableResultSegment) {
				return createEmbeddableRowReader(descriptor, queryMetadata, (EmbeddableResultSegment) segment);
			} else {
				return createScalarRowReader(descriptor, queryMetadata, (ScalarResultSegment) segment);
			}
		} else {
			CompoundRowReader reader = new CompoundRowReader(resultWidth);

			for (int i = 0; i < resultWidth; i++) {
				Object segment = rsMapping.get(i);

				if (segment instanceof EntityResultSegment) {
					reader.addRowReader(
							i,
							createEntityRowReader(descriptor, queryMetadata, (EntityResultSegment) segment,
									postProcessorFactory));
				} else if(segment instanceof EmbeddableResultSegment) {
					reader.addRowReader(i, createEmbeddableRowReader(descriptor, queryMetadata, (EmbeddableResultSegment) segment));
				} else {
					reader.addRowReader(i, createScalarRowReader(descriptor, queryMetadata, (ScalarResultSegment) segment));
				}
			}

			return reader;
		}
	}

	private RowReader<?> createEmbeddableRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, EmbeddableResultSegment segment) {
		return new EmbeddableRowReader(descriptor, queryMetadata, segment);
	}

	protected RowReader<?> createScalarRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, ScalarResultSegment segment) {
		return new ScalarRowReader<>(descriptor, segment);
	}

	protected RowReader<?> createEntityRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
			EntityResultSegment resultMetadata, PostprocessorFactory postProcessorFactory) {

		if (queryMetadata.getPageSize() > 0) {
			return new IdRowReader<>(descriptor, queryMetadata, resultMetadata, postProcessorFactory.get());
		} else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
			return new InheritanceAwareEntityRowReader(descriptor, resultMetadata, postProcessorFactory.get());
		} else {
			return new EntityRowReader(descriptor, resultMetadata, postProcessorFactory.get());
		}
	}

	protected RowReader<?> createFullRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
			PostprocessorFactory postProcessorFactory) {

		if (queryMetadata.getPageSize() > 0) {
			return new IdRowReader<>(descriptor, queryMetadata, null, postProcessorFactory.get());
		} else if (queryMetadata.getClassDescriptor() != null && queryMetadata.getClassDescriptor().hasSubclasses()) {
			return new InheritanceAwareRowReader(descriptor, queryMetadata, postProcessorFactory.get());
		} else {
			return new FullRowReader(descriptor, queryMetadata, postProcessorFactory.get());
		}
	}

	protected static class PostprocessorFactory {

		private final QueryMetadata queryMetadata;
		private final ExtendedTypeMap extendedTypes;
		private final Map<ObjAttribute, ColumnDescriptor> attributeOverrides;
		private final RowDescriptor rowDescriptor;

		private boolean created;
		private DataRowPostProcessor postProcessor;

		PostprocessorFactory(RowDescriptor rowDescriptor, QueryMetadata queryMetadata, ExtendedTypeMap extendedTypes,
				Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {
			this.rowDescriptor = rowDescriptor;
			this.extendedTypes = extendedTypes;
			this.attributeOverrides = attributeOverrides;
			this.queryMetadata = queryMetadata;
		}

		DataRowPostProcessor get() {

			if (!created) {
				postProcessor = create();
				created = true;
			}

			return postProcessor;
		}

		private DataRowPostProcessor create() {

			if (attributeOverrides.isEmpty()) {
				return null;
			}

			ColumnDescriptor[] columns = rowDescriptor.getColumns();

			Map<String, Collection<ColumnOverride>> columnOverrides = new HashMap<>(2);

			for (Entry<ObjAttribute, ColumnDescriptor> entry : attributeOverrides.entrySet()) {

				ObjAttribute attribute = entry.getKey();
				ObjEntity entity = attribute.getEntity();

				String key = null;
				int jdbcType = TypesMapping.NOT_DEFINED;
				int index = -1;
				for (int i = 0; i < columns.length; i++) {
					if (columns[i] == entry.getValue()) {

						// if attribute type is the same as column, there is no conflict
						if (!attribute.getType().equals(columns[i].getJavaClass())) {
							// note that JDBC index is "1" based
							index = i + 1;
							jdbcType = columns[i].getJdbcType();
							key = columns[i].getDataRowKey();
						}

						break;
					}
				}

				if (index < 1) {
					continue;
				}

				ExtendedType<?> converter = extendedTypes.getRegisteredType(attribute.getType());
				Collection<ColumnOverride> overrides = columnOverrides
						.computeIfAbsent(entity.getName(), k -> new ArrayList<>(3));
				overrides.add(new ColumnOverride(index, key, converter, jdbcType));
			}

			// inject null post-processor
			if (columnOverrides.isEmpty()) {
				return null;
			}

			ClassDescriptor rootDescriptor = queryMetadata.getClassDescriptor();

			return new DataRowPostProcessor(rootDescriptor, columnOverrides);
		}
	}

}
