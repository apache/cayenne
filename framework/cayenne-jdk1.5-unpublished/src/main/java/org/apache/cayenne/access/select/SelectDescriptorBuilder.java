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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLResultSetMetadata;

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

        int resultWidth;
        int[] entitySegments;
        int[] scalarSegments;

        // No metadata mapping means a single entity result...
        SQLResultSetMetadata resultSetMetadata = queryMetadata.getResultSetMapping();
        if (resultSetMetadata != null) {
            resultWidth = resultSetMetadata.getSegmentsCount();
            entitySegments = resultSetMetadata.getEntitySegments();
            scalarSegments = resultSetMetadata.getScalarSegments();
        }
        else {
            resultWidth = 1;
            entitySegments = new int[] {
                0
            };
            scalarSegments = new int[0];
        }

        if (resultWidth == 0) {
            throw new CayenneRuntimeException("Empty result descriptor");
        }

        // sanity check - paginated queries are only possible if there is an "id" of each
        // row. for now this means single entity queries...
        if (queryMetadata.getPageSize() > 0) {
            if (entitySegments.length != 1 || scalarSegments.length != 0) {
                throw new CayenneRuntimeException(
                        "Paginated queries are only supported for a single entity result");
            }
        }

        if (scalarSegments.length > 0) {
            scalarSegmentBuilder = createScalarSegmentBuilder(resultSetMetadata);
        }

        if (entitySegments.length > 0) {
            entitySegmentBuilder = createEntitySegmentBuilder(queryMetadata);
        }

        // do some small optimizations for the common 1 segment results...
        if (resultWidth == 1) {
            if (entitySegments.length > 0) {
                return entitySegmentBuilder.getSegment(0);
            }
            else {
                return scalarSegmentBuilder.getSegment(0);
            }
        }

        CompoundSelectDescriptor compoundDescriptor = new CompoundSelectDescriptor(
                resultWidth);

        for (int i : entitySegments) {
            compoundDescriptor.append(i, entitySegmentBuilder.getSegment(i));
        }

        for (int i : scalarSegments) {
            compoundDescriptor.append(i, scalarSegmentBuilder.getSegment(i));
        }

        return compoundDescriptor;
    }

    protected EntitySegmentBuilder createEntitySegmentBuilder(QueryMetadata queryMetadata) {
        return new EntitySegmentBuilder(extendedTypes, queryMetadata);
    }

    protected ScalarSegmentBuilder createScalarSegmentBuilder(SQLResultSetMetadata md) {
        return new ScalarSegmentBuilder(extendedTypes, md);
    }
}
