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
package org.apache.cayenne.access.jdbc;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

/**
 * @since 3.2
 */
public class DefaultRowReaderFactory implements RowReaderFactory {

    @Override
    public RowReader<?> createRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata) {

        List<Object> rsMapping = queryMetadata.getResultSetMapping();
        if (rsMapping == null) {
            return createFullRowReader(descriptor, queryMetadata);
        }

        int resultWidth = rsMapping.size();
        if (resultWidth == 0) {
            throw new CayenneRuntimeException("Empty result descriptor");
        } else if (resultWidth == 1) {

            Object segment = rsMapping.get(0);

            if (segment instanceof EntityResultSegment) {
                return createEntityRowReader(descriptor, queryMetadata, (EntityResultSegment) segment);
            } else {
                return new ScalarRowReader<Object>(descriptor, (ScalarResultSegment) segment);
            }
        } else {
            CompoundRowReader reader = new CompoundRowReader(resultWidth);

            for (int i = 0; i < resultWidth; i++) {
                Object segment = rsMapping.get(i);

                if (segment instanceof EntityResultSegment) {
                    reader.addRowReader(i,
                            createEntityRowReader(descriptor, queryMetadata, (EntityResultSegment) segment));
                } else {
                    reader.addRowReader(i, new ScalarRowReader<Object>(descriptor, (ScalarResultSegment) segment));
                }
            }

            return reader;
        }
    }

    private RowReader<?> createEntityRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
            EntityResultSegment resultMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader<Object>(descriptor, queryMetadata);
        } else if (resultMetadata.getClassDescriptor() != null && resultMetadata.getClassDescriptor().hasSubclasses()) {
            return new InheritanceAwareEntityRowReader(descriptor, resultMetadata);
        } else {
            return new EntityRowReader(descriptor, resultMetadata);
        }
    }

    private RowReader<?> createFullRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata) {

        if (queryMetadata.getPageSize() > 0) {
            return new IdRowReader<Object>(descriptor, queryMetadata);
        } else if (queryMetadata.getClassDescriptor() != null && queryMetadata.getClassDescriptor().hasSubclasses()) {
            return new InheritanceAwareRowReader(descriptor, queryMetadata);
        } else {
            return new FullRowReader(descriptor, queryMetadata);
        }
    }

}
