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
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;

/**
 * A class used as a builder of SelectDescriptors of any complexity.
 * 
 * @since 3.0
 */
public class SelectDescriptorBuilder {

    private ExtendedTypeMap extendedTypes;

    private transient ScalarSegmentBuilder scalarSegmentBuilder;
    private transient EntitySegmentBuilder entitySegmentBuilder;

    public SelectDescriptorBuilder(ExtendedTypeMap extendedTypes) {
        this.extendedTypes = extendedTypes;
    }

    public SelectDescriptor<?> fromQueryMetadata(QueryMetadata queryMetadata) {

        List<Object> segmentDescriptors = queryMetadata.getResultSetMapping();
        int resultWidth = segmentDescriptors != null ? segmentDescriptors.size() : 1;

        if (resultWidth == 0) {
            throw new CayenneRuntimeException("Empty result descriptor");
        }

        int entitySegments = 0, scalarSegments = 0;
        SelectDescriptor<Object>[] segments = new SelectDescriptor[resultWidth];

        if (segmentDescriptors == null) {
            segments[0] = getEntitySegmentBuilder(queryMetadata).getSegment(0);
            entitySegments++;
        }
        else {
            for (int i = 0; i < resultWidth; i++) {

                Object segmentDescriptor = segmentDescriptors.get(i);

                if (segmentDescriptor instanceof EntityResultSegment) {
                    segments[i] = getEntitySegmentBuilder(queryMetadata).getSegment(i);
                    entitySegments++;
                }
                else {
                    segments[i] = getScalarSegmentBuilder(segmentDescriptors).getSegment(
                            i);
                    scalarSegments++;
                }
            }
        }

        // sanity check - paginated queries are only possible if there is an "id" of each
        // row. for now this means single entity queries...
        if (queryMetadata.getPageSize() > 0) {
            if (entitySegments != 1 || scalarSegments != 0) {
                throw new CayenneRuntimeException(
                        "Paginated queries are only supported for a single entity result");
            }
        }

        // do some small optimizations for the common 1 segment results...
        if (resultWidth == 1) {
            return segments[0];
        }

        return new CompoundSelectDescriptor(segments);
    }

    protected EntitySegmentBuilder getEntitySegmentBuilder(QueryMetadata queryMetadata) {
        if (entitySegmentBuilder == null) {
            entitySegmentBuilder = new EntitySegmentBuilder(extendedTypes, queryMetadata);
        }
        return entitySegmentBuilder;
    }

    protected ScalarSegmentBuilder getScalarSegmentBuilder(List<Object> segmentDescriptors) {
        if (scalarSegmentBuilder == null) {
            scalarSegmentBuilder = new ScalarSegmentBuilder(
                    extendedTypes,
                    segmentDescriptors);
        }
        return scalarSegmentBuilder;
    }
}
