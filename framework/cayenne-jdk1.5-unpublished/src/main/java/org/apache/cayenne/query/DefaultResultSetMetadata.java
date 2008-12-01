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
package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A SQLResultSetMetadata implementation based on {@link SQLResult}.
 * 
 * @since 3.0
 */
class DefaultResultSetMetadata implements SQLResultSetMetadata {

    private List<Object> segments;
    private int[] scalarSegments;
    private int[] entitySegments;

    DefaultResultSetMetadata(SQLResult result, EntityResolver resolver) {
        List<Object> descriptors = result.getComponents();

        this.segments = new ArrayList<Object>(descriptors.size());

        int[] scalarSegments = new int[descriptors.size()];
        int[] entitySegments = new int[descriptors.size()];

        int ss = 0, es = 0;
        int offset = 0;
        for (int i = 0; i < descriptors.size(); i++) {

            Object descriptor = descriptors.get(i);
            if (descriptor instanceof String) {
                segments
                        .add(new DefaultScalarResultMetadata((String) descriptor, offset));
                offset = offset + 1;
                scalarSegments[ss++] = i;
            }
            else if (descriptor instanceof EntityResult) {
                EntityResult entityResult = (EntityResult) descriptor;
                Map<String, String> fields = entityResult.getDbFields(resolver);

                String entityName = entityResult.getEntityName();
                if (entityName == null) {
                    entityName = resolver
                            .lookupObjEntity(entityResult.getEntityClass())
                            .getName();
                }

                ClassDescriptor classDescriptor = resolver.getClassDescriptor(entityName);
                segments.add(new DefaultEntityResultMetadata(
                        classDescriptor,
                        fields,
                        offset));
                offset = offset + fields.size();
                entitySegments[es++] = i;
            }
            else {
                throw new IllegalArgumentException(
                        "Unsupported result set descriptor type: " + descriptor);
            }
        }

        this.scalarSegments = trim(scalarSegments, ss);
        this.entitySegments = trim(entitySegments, es);
    }

    private int[] trim(int[] array, int size) {
        int[] trimmed = new int[size];
        System.arraycopy(array, 0, trimmed, 0, size);
        return trimmed;
    }

    public EntityResultMetadata getEntitySegment(int position) {
        Object result = segments.get(position);
        if (result instanceof EntityResultMetadata) {
            return (EntityResultMetadata) result;
        }

        throw new IllegalArgumentException("Segment at position "
                + position
                + " is not an entity segment");
    }

    public int[] getEntitySegments() {
        return entitySegments;
    }

    public ScalarResultMetadata getScalarSegment(int position) {

        Object result = segments.get(position);
        if (result instanceof ScalarResultMetadata) {
            return (ScalarResultMetadata) result;
        }

        throw new IllegalArgumentException("Segment at position "
                + position
                + " is not a scalar segment");
    }

    public int[] getScalarSegments() {
        return scalarSegments;
    }

    public int getSegmentsCount() {
        return segments.size();
    }
}
