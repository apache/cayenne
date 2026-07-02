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
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.util.Util;

import java.sql.ResultSet;

/**
 * @since 3.0
 */
class ScalarRowReader<T> implements RowReader<T> {

    private final ExtendedType<T> reader;
    private final int index;
    private final int type;

    public ScalarRowReader(ExtendedType<T> reader, int index, int type) {
        this.reader = reader;
        this.index = index;
        this.type = type;
    }

    @Override
    public T readRow(ResultSet resultSet) {
        try {
            return reader.materializeObject(resultSet, index, type);
        } catch (CayenneRuntimeException cex) {
            // rethrow unmodified
            throw cex;
        } catch (Exception otherex) {
            throw new CayenneRuntimeException("Exception materializing column.", Util.unwindException(otherex));
        }
    }
}
