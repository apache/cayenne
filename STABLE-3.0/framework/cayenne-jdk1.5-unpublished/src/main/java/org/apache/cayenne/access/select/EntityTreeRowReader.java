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

import java.sql.ResultSet;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.exp.Expression;

/**
 * Inheritance-aware entity RowReader.
 * 
 * @since 3.0
 */
class EntityTreeRowReader implements RowReader<Object> {

    private RowReader<?> discriminatorReader;
    private Expression[] entityQualifiers;
    private RowReader<Object>[] entityReaders;

    EntityTreeRowReader(RowReader<?> discriminatorReader, Expression[] entityQualifiers,
            RowReader<Object>[] entityReaders) {

        this.discriminatorReader = discriminatorReader;
        this.entityQualifiers = entityQualifiers;
        this.entityReaders = entityReaders;
    }

    public void setColumnOffset(int offset) {
        throw new UnsupportedOperationException("TODO");
    }

    public Object readRow(ResultSet resultSet) throws CayenneException {

        Map<String, Object> discriminator = (Map<String, Object>) discriminatorReader
                .readRow(resultSet);

        // read qualifiers list in reverse order to ensure that for each superclass,
        // subclass qualifiers are run first... This way we can even support empty
        // qualifier superclasses.
        int len = entityQualifiers.length;
        for (int i = len - 1; i >= 0; i--) {

            // TODO: andrus, 12/25/2008 - Expression in-memory evaluation for each row in
            // a ResultSet will be a very slow operation. This procedure should be
            // optimized somehow...
            if (entityQualifiers[i].match(discriminator)) {
                return entityReaders[i].readRow(resultSet);
            }
        }

        throw new CayenneException(
                "Row discriminator did not match any entities in the inheritance hierarchy: "
                        + discriminator);
    }
}
