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
import org.apache.cayenne.access.jdbc.RowDescriptor;
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
	public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter) {

		List<Object> rsMapping = queryMetadata.getResultSetMapping();
		if (rsMapping == null) {
			return createFullRowReader(descriptor, queryMetadata);
		}

		int resultWidth = rsMapping.size();
		if (resultWidth == 0) {
			throw new CayenneRuntimeException("Empty result descriptor");
		}

		if (queryMetadata.isSingleResultSetMapping()) {

			Object segment = rsMapping.get(0);

			if (segment instanceof EntityResultSegment) {
				return createEntityRowReader(descriptor, queryMetadata, (EntityResultSegment) segment);
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
					reader.addRowReader(i,
							createEntityRowReader(descriptor, queryMetadata, (EntityResultSegment) segment));
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
			EntityResultSegment resultMetadata) {

		if (queryMetadata.getPageSize() > 0) {
			return new IdRowReader<>(descriptor, queryMetadata, resultMetadata);
		} else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
			return new InheritanceAwareEntityRowReader(descriptor, resultMetadata);
		} else {
			return new EntityRowReader(descriptor, resultMetadata);
		}
	}

	protected RowReader<?> createFullRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata) {

		if (queryMetadata.getPageSize() > 0) {
			return new IdRowReader<>(descriptor, queryMetadata, null);
		} else if (queryMetadata.getClassDescriptor() != null && queryMetadata.getClassDescriptor().hasSubclasses()) {
			return new InheritanceAwareRowReader(descriptor, queryMetadata);
		} else {
			return new FullRowReader(descriptor, queryMetadata);
		}
	}

}
