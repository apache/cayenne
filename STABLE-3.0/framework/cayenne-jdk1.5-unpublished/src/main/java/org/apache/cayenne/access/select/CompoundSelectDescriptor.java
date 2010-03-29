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
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneException;

/**
 * @since 3.0
 */
class CompoundSelectDescriptor implements SelectDescriptor<Object[]> {

    private SelectDescriptor<Object>[] segments;
    private CompoundRowReader rowReader;
    private List<SelectColumn> columns;

    CompoundSelectDescriptor(SelectDescriptor<Object>[] segments) {
        this.segments = segments;
    }

    public List<SelectColumn> getColumns() {
        return columns;
    }

    public RowReader<Object[]> getRowReader(ResultSet resultSet) throws CayenneException {
        if (rowReader == null) {
            willReadResultSet(resultSet);
        }

        return rowReader;
    }

    private void willReadResultSet(ResultSet resultSet) throws CayenneException {

        this.columns = new ArrayList<SelectColumn>();
        this.rowReader = new CompoundRowReader(segments.length);

        // finish descriptor initialization
        for (int i = 0; i < segments.length; i++) {

            int offset = columns.size();
            columns.addAll(segments[i].getColumns());

            RowReader<Object> rowReader = segments[i].getRowReader(resultSet);
            rowReader.setColumnOffset(offset);
            this.rowReader.addRowReader(i, rowReader);
        }
    }
}
