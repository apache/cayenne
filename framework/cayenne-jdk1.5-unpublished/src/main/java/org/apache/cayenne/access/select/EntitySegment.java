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
import java.util.List;

import org.apache.cayenne.CayenneException;

/**
 * @since 3.0
 */
class EntitySegment implements SelectSegment {

    private List<? extends SelectColumn> columns;
    private EntityRowReader rowReader;

    EntitySegment(EntityRowReader rowReader, List<? extends SelectColumn> columns) {
        this.columns = columns;
        this.rowReader = rowReader;
    }

    public void setColumnOffset(int offset) {
        rowReader.setColumnOffset(offset);

    }

    public List<? extends SelectColumn> getColumns() {
        return columns;
    }

    public RowReader<Object> getRowReader(ResultSet resultSet) throws CayenneException {
        return rowReader;
    }
}
