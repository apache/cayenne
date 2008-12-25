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
 * Describes a mapping between JDBC ResultSet and object select result. In general terms,
 * SelectDescriptor creates a mapping between a tree (object result metadata, including
 * support for scalar/entity results as well as inheritance) and a 2D matrix, representing
 * metadata of the SELECT clause of a UNION select query.
 * 
 * @since 3.0
 */
public interface SelectDescriptor<T> {

    /**
     * Returns a {@link RowReader} for the result set..
     */
    RowReader<T> getRowReader(ResultSet resultSet) throws CayenneException;

    /**
     * Returns a list of result set columns.
     */
    List<? extends SelectColumn> getColumns();

}
