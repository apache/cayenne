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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // For a paginated query only the root PK is read into the IncrementalFaultList - locate the PK columns within
        // this segment by their DbAttribute (see idReader).
        if (metadata.getPageSize() > 0) {
            ObjEntity objEntity = segment.getClassDescriptor().getEntity();
            return idReader(columns,
                    segment.getColumnOffset(),
                    segment.getColumnOffset() + columns.length,
                    objEntity.getDbEntity(),
                    objEntity.getName());
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
            return idReader(columns, 0, columns.length, metadata.getDbEntity(),
                    objEntity != null ? objEntity.getName() : null);
        }
        return FullRowReader.of(columns, metadata);
    }

    private RowReader<?> idReader(RSColumn[] columns, int from, int to, DbEntity dbEntity, String entityName) {
        if (dbEntity == null) {
            throw new CayenneRuntimeException("Null root DbEntity, can't index PK");
        }

        int pkLen = dbEntity.getPrimaryKeys().size();
        if (pkLen == 0) {
            throw new CayenneRuntimeException("Root DbEntity has no PK defined: %s", dbEntity.getName());
        }

        RSColumn[] pkColumns = new RSColumn[pkLen];
        int[] pkIndexes = new int[pkLen];
        int found = 0;

        Set<DbAttribute> seen = new HashSet<>();
        for (int i = from; i < to && found < pkLen; i++) {
            DbAttribute attribute = columns[i].attribute();
            if (attribute != null && attribute.isPrimaryKey() && seen.add(attribute)) {
                pkColumns[found] = columns[i];
                pkIndexes[found] = i + 1;
                found++;
            }
        }

        if (found != pkLen) {
            // TODO: HACK: the result columns don't carry resolvable PK DbAttributes (these are most likely aliased
            //  EJBQL columns). Fall back to the legacy assumption that the PK is the first pkLen columns starting at
            //  'from'.
            if (from + pkLen > columns.length) {
                throw new CayenneRuntimeException(
                        "Result set for paginated query is missing PK column(s) of entity '%s'; expected %s",
                        entityName, dbEntity.getPrimaryKeys().stream().map(DbAttribute::getName).toList());
            }
            for (int i = 0; i < pkLen; i++) {
                pkColumns[i] = columns[from + i];
                // jdbc column indexes start from 1
                pkIndexes[i] = from + i + 1;
            }
        }

        return pkLen == 1
                ? new ScalarRowReader<>(pkColumns[0].reader(), pkIndexes[0], pkColumns[0].rsType())
                : IndexRowReader.of(pkColumns, pkIndexes, entityName);
    }

}
