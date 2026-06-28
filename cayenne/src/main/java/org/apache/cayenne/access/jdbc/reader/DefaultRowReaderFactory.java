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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

import java.util.List;

/**
 * @since 4.0
 */
public class DefaultRowReaderFactory implements RowReaderFactory {

    @Override
    public RowReader<?> rowReader(RSColumn[] columns, QueryMetadata queryMetadata, DbAdapter adapter) {

        List<Object> rsMapping = queryMetadata.getResultSetMapping();
        if (rsMapping == null) {
            return createFullRowReader(columns, queryMetadata);
        }

        int resultWidth = rsMapping.size();
        if (resultWidth == 0) {
            throw new CayenneRuntimeException("Empty result columns");
        }

        if (queryMetadata.isSingleResultSetMapping()) {
            return segmentRowReader(rsMapping.getFirst(), columns, queryMetadata);
        } else {
            CompoundRowReader reader = new CompoundRowReader(resultWidth);
            for (int i = 0; i < resultWidth; i++) {
                reader.addRowReader(i, segmentRowReader(rsMapping.get(i), columns, queryMetadata));
            }

            return reader;
        }
    }

    private RowReader<?> segmentRowReader(Object segment, RSColumn[] columns, QueryMetadata queryMetadata) {
        return switch (segment) {
            case EntityResultSegment ers -> createEntityRowReader(columns, queryMetadata, ers);
            case EmbeddableResultSegment ers -> createEmbeddableRowReader(columns, ers);
            case ScalarResultSegment srs -> createScalarRowReader(columns, queryMetadata, srs);
            case null, default -> throw new IllegalStateException("Unknown segment type: " + segment);
        };
    }

    private RowReader<?> createEmbeddableRowReader(RSColumn[] columns, EmbeddableResultSegment segment) {
        int segmentWidth = segment.getFields().size();
        int startIndex = segment.getColumnOffset();
        ExtendedType<?>[] converters = new ExtendedType[segmentWidth];
        int[] types = new int[segmentWidth];
        String[] labels = new String[segmentWidth];

        for (int i = 0; i < segmentWidth; i++) {
            converters[i] = columns[startIndex + i].reader();
            types[i] = columns[startIndex + i].rsType();
            labels[i] = segment.getFields().get(columns[startIndex + i].rsName());
        }

        return new EmbeddableRowReader(converters, types, labels, startIndex);
    }

    protected RowReader<?> createScalarRowReader(RSColumn[] columns, QueryMetadata queryMetadata, ScalarResultSegment segment) {
        int scalarIndex = segment.getColumnOffset();
        return new ScalarRowReader<>(
                columns[scalarIndex].reader(),
                // jdbc column indexes start from 1
                scalarIndex + 1,
                columns[scalarIndex].rsType());
    }

    protected RowReader<?> createEntityRowReader(RSColumn[] columns, QueryMetadata queryMetadata,
                                                 EntityResultSegment resultMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader<>(columns, queryMetadata, resultMetadata);
        } else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
            return new InheritanceAwareEntityRowReader(columns, resultMetadata);
        } else {
            return new EntityRowReader(columns, resultMetadata);
        }
    }

    protected RowReader<?> createFullRowReader(RSColumn[] columns, QueryMetadata queryMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader<>(columns, queryMetadata, null);
        } else if (queryMetadata.getClassDescriptor() != null && queryMetadata.getClassDescriptor().hasSubclasses()) {
            return new InheritanceAwareRowReader(columns, queryMetadata);
        } else {
            return new FullRowReader(columns, queryMetadata);
        }
    }

}
