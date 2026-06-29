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
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

import java.util.Arrays;
import java.util.List;

/**
 * @since 4.0
 */
public class DefaultRowReaderFactory implements RowReaderFactory {

    @Override
    public RowReader<?> rowReader(RSColumn[] columns, QueryMetadata metadata, DbAdapter adapter) {

        List<Object> segments = metadata.getResultSetMapping();
        if (segments == null || segments.isEmpty()) {
            return noSegmentReader(columns, metadata);
        }

        if (metadata.isSingleResultSetMapping()) {
            return segmentReader(segments.getFirst(), columns, metadata);
        }

        int w = segments.size();
        RowReader<?>[] readers = new RowReader[w];
        for (int i = 0; i < w; i++) {
            readers[i] = segmentReader(segments.get(i), columns, metadata);
        }

        return new CompoundRowReader(readers);
    }

    private RowReader<?> segmentReader(Object segment, RSColumn[] columns, QueryMetadata metadata) {
        return switch (segment) {
            case EntityResultSegment ers -> entitySegmentReader(columns, metadata, ers);
            case EmbeddableResultSegment ers -> embeddableSegmentReader(columns, ers);
            case ScalarResultSegment srs -> scalarSegmentReader(columns, metadata, srs);
            case null, default -> throw new IllegalStateException("Unknown segment type: " + segment);
        };
    }

    private RowReader<?> embeddableSegmentReader(RSColumn[] columns, EmbeddableResultSegment segment) {
        int startIndex = segment.getColumnOffset();
        int segmentWidth = segment.getFields().size();

        // recast the segment's columns into a compact array so their dataRowName carries the embeddable field
        // label (keyed by the result-set column name); OffsetRowReader reads them from startIndex onward
        RSColumn[] relabeled = new RSColumn[segmentWidth];

        for (int i = 0; i < segmentWidth; i++) {
            RSColumn column = columns[startIndex + i];
            relabeled[i] = new RSColumn(
                    column.rsName(),
                    column.rsType(),
                    segment.getFields().get(column.rsName()),
                    column.reader(),
                    column.attribute());
        }

        // an embeddable segment carries no entity - no entity name, no inheritance
        return OffsetRowReader.of(relabeled, startIndex);
    }

    protected RowReader<?> scalarSegmentReader(RSColumn[] columns, QueryMetadata metadata, ScalarResultSegment segment) {
        int scalarIndex = segment.getColumnOffset();
        return new ScalarRowReader<>(
                columns[scalarIndex].reader(),
                // jdbc column indexes start from 1
                scalarIndex + 1,
                columns[scalarIndex].rsType());
    }

    protected RowReader<?> entitySegmentReader(RSColumn[] columns, QueryMetadata metadata, EntityResultSegment segment) {

        if (metadata.getPageSize() > 0) {
            ObjEntity objEntity = metadata.getObjEntity();
            return idReader(columns,
                    segment.getColumnOffset(),
                    objEntity != null ? objEntity.getName() : null,
                    segment.getClassDescriptor().getEntity().getDbEntity());
        }

        int startIndex = segment.getColumnOffset();
        int segmentWidth = segment.getFields().size();

        // recast the segment's columns into a compact array so their dataRowName carries the resolved DataRow
        // label (which is how the reader keys the DataRow); OffsetRowReader reads them from startIndex onward
        RSColumn[] relabeled = new RSColumn[segmentWidth];

        for (int i = 0; i < segmentWidth; i++) {
            RSColumn column = columns[startIndex + i];

            // the query translator may reorder fields compared to the entity result, so resolve the
            // DataRow label by reverse lookup of the column name; a dotted dataRowName is a prefetched
            // column, used directly instead of by alias
            String name = column.dataRowName();
            String label = name.contains(".") ? name : segment.getColumnPath(name);

            relabeled[i] = new RSColumn(
                    column.rsName(),
                    column.rsType(),
                    label,
                    column.reader(),
                    column.attribute());
        }

        return OffsetRowReader.of(relabeled, startIndex, segment.getClassDescriptor());
    }

    protected RowReader<?> noSegmentReader(RSColumn[] columns, QueryMetadata metadata) {
        if (metadata.getPageSize() > 0) {
            ObjEntity objEntity = metadata.getObjEntity();
            return idReader(
                    columns,
                    0,
                    objEntity != null ? objEntity.getName() : null,
                    metadata.getDbEntity());
        }
        return FullRowReader.of(columns, metadata);
    }

    private RowReader<?> idReader(RSColumn[] columns, int offset, String entityName, DbEntity dbEntity) {
        if (dbEntity == null) {
            throw new CayenneRuntimeException("Null root DbEntity, can't index PK");
        }

        int pkLen = dbEntity.getPrimaryKeys().size();
        if (pkLen == 0) {
            throw new CayenneRuntimeException("Root DBEntity has no PK defined: %s", dbEntity);
        }

        // TODO: An implicit assumption below is that "RSColumn[] columns" starts with a PK block (with an optional
        //  segment offset), and PK columns are contiguous. If the upstream algorithm ever changes, this will no longer
        //  work.

        if (pkLen == 1) {
            RSColumn column = columns[offset];
            // jdbc column indexes start from 1
            return new ScalarRowReader<>(column.reader(), offset + 1, column.rsType());
        }

        RSColumn[] pkColumns = Arrays.copyOfRange(columns, offset, offset + pkLen);
        return OffsetRowReader.of(pkColumns, offset, entityName);
    }

}
