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

package org.apache.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Defines methods to read Java objects from JDBC ResultSets and write as parameters of
 * PreparedStatements.
 */
public interface ExtendedType {

    /**
     * Returns a full name of Java class that this ExtendedType supports.
     */
    String getClassName();

    /**
     * Initializes a single parameter of a PreparedStatement with object value.
     */
    void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int scale) throws Exception;

    /**
     * Reads an object from JDBC ResultSet column, converting it to class returned by
     * 'getClassName' method.
     * 
     * @throws Exception if read error occurred, or an object can't be converted to a
     *             target Java class.
     */
    Object materializeObject(ResultSet rs, int index, int type) throws Exception;

    /**
     * Reads an object from a stored procedure OUT parameter, converting it to class
     * returned by 'getClassName' method.
     * 
     * @throws Exception if read error ocurred, or an object can't be converted to a
     *             target Java class.
     */
    Object materializeObject(CallableStatement rs, int index, int type) throws Exception;
}
