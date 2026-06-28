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

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

/**
 * @since 4.0
 */
public class DefaultRowReaderFactory implements RowReaderFactory {

	@Override
	public RowReader<?> rowReader(ColumnDescriptor[] columns, QueryMetadata queryMetadata, DbAdapter adapter) {

		List<Object> rsMapping = queryMetadata.getResultSetMapping();
		if (rsMapping == null) {
			return createFullRowReader(columns, queryMetadata);
		}

		int resultWidth = rsMapping.size();
		if (resultWidth == 0) {
			throw new CayenneRuntimeException("Empty result columns");
		}

		if (queryMetadata.isSingleResultSetMapping()) {

			Object segment = rsMapping.get(0);

			if (segment instanceof EntityResultSegment) {
				return createEntityRowReader(columns, queryMetadata, (EntityResultSegment) segment);
			} else if (segment instanceof EmbeddableResultSegment) {
				return createEmbeddableRowReader(columns, queryMetadata, (EmbeddableResultSegment) segment);
			} else {
				return createScalarRowReader(columns, queryMetadata, (ScalarResultSegment) segment);
			}
		} else {
			CompoundRowReader reader = new CompoundRowReader(resultWidth);

			for (int i = 0; i < resultWidth; i++) {
				Object segment = rsMapping.get(i);

				if (segment instanceof EntityResultSegment) {
					reader.addRowReader(i,
							createEntityRowReader(columns, queryMetadata, (EntityResultSegment) segment));
				} else if(segment instanceof EmbeddableResultSegment) {
					reader.addRowReader(i, createEmbeddableRowReader(columns, queryMetadata, (EmbeddableResultSegment) segment));
				} else {
					reader.addRowReader(i, createScalarRowReader(columns, queryMetadata, (ScalarResultSegment) segment));
				}
			}

			return reader;
		}
	}

	private RowReader<?> createEmbeddableRowReader(ColumnDescriptor[] columns, QueryMetadata queryMetadata, EmbeddableResultSegment segment) {
		return new EmbeddableRowReader(columns, queryMetadata, segment);
	}

	protected RowReader<?> createScalarRowReader(ColumnDescriptor[] columns, QueryMetadata queryMetadata, ScalarResultSegment segment) {
		return new ScalarRowReader<>(columns, segment);
	}

	protected RowReader<?> createEntityRowReader(ColumnDescriptor[] columns, QueryMetadata queryMetadata,
			EntityResultSegment resultMetadata) {

		if (queryMetadata.getPageSize() > 0) {
			return new IdRowReader<>(columns, queryMetadata, resultMetadata);
		} else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
			return new InheritanceAwareEntityRowReader(columns, resultMetadata);
		} else {
			return new EntityRowReader(columns, resultMetadata);
		}
	}

	protected RowReader<?> createFullRowReader(ColumnDescriptor[] columns, QueryMetadata queryMetadata) {

		if (queryMetadata.getPageSize() > 0) {
			return new IdRowReader<>(columns, queryMetadata, null);
		} else if (queryMetadata.getClassDescriptor() != null && queryMetadata.getClassDescriptor().hasSubclasses()) {
			return new InheritanceAwareRowReader(columns, queryMetadata);
		} else {
			return new FullRowReader(columns, queryMetadata);
		}
	}

}
