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

import java.sql.ResultSet;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class ScalarRowReader<T> implements RowReader<T> {

    private ExtendedType converter;
    private int index;
    private int type;

    ScalarRowReader(RowDescriptor descriptor, ScalarResultSegment segmentMetadata) {

        int scalarIndex = segmentMetadata.getColumnOffset();
        this.converter = descriptor.getConverters()[scalarIndex];
        this.type = descriptor.getColumns()[scalarIndex].getJdbcType();
        this.index = scalarIndex + 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T readRow(ResultSet resultSet) {
        try {
            return (T) converter.materializeObject(resultSet, index, type);
        } catch (CayenneRuntimeException cex) {
            // rethrow unmodified
            throw cex;
        } catch (Exception otherex) {
            throw new CayenneRuntimeException("Exception materializing column.", Util.unwindException(otherex));
        }
    }
}
