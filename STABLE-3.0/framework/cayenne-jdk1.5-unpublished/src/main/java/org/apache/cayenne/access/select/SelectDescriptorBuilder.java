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
package org.apache.cayenne.access.select;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A class used as a builder of SelectDescriptors of any complexity.
 * 
 * @since 3.0
 */
public class SelectDescriptorBuilder {

    private ExtendedTypeMap extendedTypes;
    private transient ScalarSegmentBuilder scalarSegmentBuilder;

    public SelectDescriptorBuilder(ExtendedTypeMap extendedTypes) {
        this.extendedTypes = extendedTypes;
    }

    public SelectDescriptor<?> fromQueryMetadata(QueryMetadata metadata) {

        List<Object> segmentMap = metadata.getResultSetMapping();
        int segmentsCount = segmentMap != null ? segmentMap.size() : 1;

        if (segmentsCount == 0) {
            throw new CayenneRuntimeException("Empty result descriptor");
        }

        int entitySegments = 0, scalarSegments = 0;
        SelectDescriptor<Object>[] segments = new SelectDescriptor[segmentsCount];

        if (segmentMap == null) {
            segments[0] = getEntitySegment(metadata, null, 0);
            entitySegments++;
        }
        else {
            for (int i = 0; i < segmentsCount; i++) {

                Object segmentDescriptor = segmentMap.get(i);

                if (segmentDescriptor instanceof EntityResultSegment) {
                    segments[i] = getEntitySegment(
                            metadata,
                            (EntityResultSegment) segmentDescriptor,
                            i);
                    entitySegments++;
                }
                else {
                    segments[i] = getScalarSegment(segmentMap, i);
                    scalarSegments++;
                }
            }
        }

        // sanity check - paginated queries are only possible if there is an "id" of each
        // row. for now this means single entity queries...
        if (metadata.getPageSize() > 0) {
            if (entitySegments != 1 || scalarSegments != 0) {
                throw new CayenneRuntimeException(
                        "Paginated queries are only supported for a single entity result");
            }
        }

        // do some small optimizations for the common 1 segment results...
        if (segmentsCount == 1) {
            return segments[0];
        }

        return new CompoundSelectDescriptor(segments);
    }

    protected SelectDescriptor<Object> getEntitySegment(
            QueryMetadata metadata,
            EntityResultSegment segmentDescriptor,
            int position) {

        ClassDescriptor classDescriptor;
        EntityResultSegment segmentMetadata;

        List<Object> segmentDesriptors = metadata.getResultSetMapping();
        if (segmentDesriptors != null) {
            segmentMetadata = (EntityResultSegment) segmentDesriptors.get(position);
            classDescriptor = segmentMetadata.getClassDescriptor();
        }
        else {
            segmentMetadata = null;
            classDescriptor = metadata.getClassDescriptor();
        }

        // no ObjEntity and Java class at the root of the query...
        if (classDescriptor == null) {
            DbEntity dbEntity = metadata.getDbEntity();
            if (dbEntity == null) {
                throw new CayenneRuntimeException("Invalid entity segment in position "
                        + position
                        + ", no root DbEntity specified");
            }

            throw new UnsupportedOperationException("TODO: DbEntity based queries");
        }

        if (classDescriptor.getEntityInheritanceTree() != null
                || classDescriptor.getSuperclassDescriptor() != null) {
            return new EntityTreeSegmentBuilder(metadata, extendedTypes, classDescriptor)
                    .buildSegment();
        }
        else {
            return new EntitySegmentBuilder(metadata, extendedTypes, classDescriptor
                    .getEntity()).buildSegment();
        }
    }

    protected SelectDescriptor<Object> getScalarSegment(
            List<Object> segmentDescriptors,
            int position) {
        if (scalarSegmentBuilder == null) {
            scalarSegmentBuilder = new ScalarSegmentBuilder(
                    extendedTypes,
                    segmentDescriptors);
        }
        return scalarSegmentBuilder.getSegment(position);
    }
}
