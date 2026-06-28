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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
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

        List<Object> segments = queryMetadata.getResultSetMapping();
        if (segments == null || segments.isEmpty()) {
            return createFullRowReader(columns, queryMetadata);
        }

        if (queryMetadata.isSingleResultSetMapping()) {
            return segmentRowReader(segments.getFirst(), columns, queryMetadata);
        }

        int w = segments.size();
        RowReader<?>[] readers = new RowReader[w];
        for (int i = 0; i < w; i++) {
            readers[i] = segmentRowReader(segments.get(i), columns, queryMetadata);
        }

        return new CompoundRowReader(readers);
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

    protected RowReader<?> createEntityRowReader(
            RSColumn[] columns,
            QueryMetadata queryMetadata,
            EntityResultSegment resultMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return createIdRowReader(columns, queryMetadata, resultMetadata);
        }

        int startIndex = resultMetadata.getColumnOffset();
        int segmentWidth = resultMetadata.getFields().size();
        ExtendedType<?>[] readers = new ExtendedType[segmentWidth];
        int[] types = new int[segmentWidth];
        String[] labels = new String[segmentWidth];

        for (int i = 0; i < segmentWidth; i++) {
            RSColumn column = columns[startIndex + i];
            readers[i] = column.reader();
            types[i] = column.rsType();

            // the query translator may reorder fields compared to the entity result, so resolve the
            // DataRow label by reverse lookup of the column name...
            if (column.dataRowName().contains(".")) {
                // a dotted dataRowName is a prefetched column - use it directly instead of by alias
                labels[i] = column.dataRowName();
            } else {
                labels[i] = resultMetadata.getColumnPath(column.dataRowName());
            }
        }

        return EntityRowReader.of(readers, types, labels, startIndex, resultMetadata.getClassDescriptor());
    }

    protected RowReader<?> createFullRowReader(RSColumn[] columns, QueryMetadata queryMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return createIdRowReader(columns, queryMetadata, null);
        }

        return FullRowReader.of(columns, queryMetadata);
    }

    private RowReader<?> createIdRowReader(RSColumn[] columns, QueryMetadata queryMetadata,
                                           EntityResultSegment resultMetadata) {
        int[] pk = pkIndices(columns, queryMetadata, resultMetadata);

        // single-column PK - read the value directly as a scalar
        if (pk.length == 1) {
            RSColumn column = columns[pk[0]];
            // jdbc column indexes start from 1
            return new ScalarRowReader<>(column.reader(), pk[0] + 1, column.rsType());
        }

        return new IndexedRowReader(columns, entityName(queryMetadata), pk);
    }

    private static String entityName(QueryMetadata queryMetadata) {
        ObjEntity objEntity = queryMetadata.getObjEntity();
        return objEntity != null ? objEntity.getName() : null;
    }

    private static int[] pkIndices(RSColumn[] columns, QueryMetadata queryMetadata, EntityResultSegment resultMetadata) {
        DbEntity dbEntity = resultMetadata == null
                ? queryMetadata.getDbEntity()
                : resultMetadata.getClassDescriptor().getEntity().getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException("Null root DbEntity, can't index PK");
        }

        int len = dbEntity.getPrimaryKeys().size();
        if (len == 0) {
            throw new CayenneRuntimeException("Root DBEntity has no PK defined: %s", dbEntity);
        }

        int[] pk = new int[len];
        int offset = resultMetadata != null ? resultMetadata.getColumnOffset() : 0;
        for (int i = offset, j = 0; i < offset + len; i++) {
            DbAttribute a = dbEntity.getAttribute(columns[i].rsName());
            if (a != null && a.isPrimaryKey()) {
                pk[j++] = i;
            }
        }
        return pk;
    }

}
