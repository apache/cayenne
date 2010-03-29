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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.CayenneException;

/**
 * Defines API of an iterator over the records returned as a result of SelectQuery
 * execution. Usually a ResultIterator is supported by an open java.sql.ResultSet,
 * therefore most of the methods would throw checked exceptions. ResultIterators must be
 * explicitly closed when the user is done working with them.
 * <p>
 * Result "rows", depending on the query, may be represented as scalar values, DataRows,
 * or Object[] arrays containing a mix of scalars and DataRows.
 */
public interface ResultIterator {

    /**
     * Returns all yet unread rows from ResultSet without closing it.
     * 
     * @since 3.0
     */
    List<?> allRows() throws CayenneException;

    /**
     * Returns true if there is at least one more record that can be read from the
     * iterator.
     */
    boolean hasNextRow() throws CayenneException;

    /**
     * Returns the next result row that is, depending on the query, may be a scalar value,
     * a DataRow, or an Object[] array containing a mix of scalars and DataRows.
     * 
     * @since 3.0
     */
    Object nextRow() throws CayenneException;

    /**
     * @since 3.0
     */
    void skipRow() throws CayenneException;

    /**
     * Closes ResultIterator and associated ResultSet. This method must be called
     * explicitly when the user is finished processing the records. Otherwise unused
     * database resources will not be released properly.
     */
    void close() throws CayenneException;
}
