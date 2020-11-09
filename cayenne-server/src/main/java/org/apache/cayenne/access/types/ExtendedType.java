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

package org.apache.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import org.apache.cayenne.access.sqlbuilder.sqltree.ChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * Defines methods to read Java objects from JDBC ResultSets and write as parameters of PreparedStatements.
 */
public interface ExtendedType<T> {


    /**
     * Defines trimming constant for toString method that helps to limit logging of large values.
     */
    int TRIM_VALUES_THRESHOLD = 30;

    /**
     * Returns a full name of Java class that this ExtendedType supports.
     */
    String getClassName();

    /**
     * Initializes a single parameter of a PreparedStatement with object value.
     */
    void setJdbcObject(
            PreparedStatement statement,
            T value,
            int pos,
            int type,
            int scale) throws Exception;

    /**
     * Reads an object from JDBC ResultSet column, converting it to class returned by
     * 'getClassName' method.
     *
     * @throws Exception if read error occurred, or an object can't be converted to a
     *                   target Java class.
     */
    T materializeObject(ResultSet rs, int index, int type) throws Exception;

    /**
     * Reads an object from a stored procedure OUT parameter, converting it to class
     * returned by 'getClassName' method.
     *
     * @throws Exception if read error occurred, or an object can't be converted to a
     *                   target Java class.
     */
    T materializeObject(CallableStatement rs, int index, int type) throws Exception;

    /**
     * Converts value of the supported type to a human-readable String representation.
     *
     * @param value a value to convert to String.
     * @since 4.0
     */
    String toString(T value);

}
