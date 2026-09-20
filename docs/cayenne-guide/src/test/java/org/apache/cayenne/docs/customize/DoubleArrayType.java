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
package org.apache.cayenne.docs.customize;

import org.apache.cayenne.access.types.ExtendedType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.stream.Collectors;

// tag::content[]
/**
 * Defines methods to read Java objects from JDBC ResultSets and write as parameters of
 * PreparedStatements.
 */
public class DoubleArrayType implements ExtendedType<Double[]> {

    private final String SEPARATOR = ",";

    /**
     * Returns a full name of Java class that this ExtendedType supports.
     */
    @Override
    public String getClassName() {
        return Double[].class.getCanonicalName();
    }

    /**
     * Initializes a single parameter of a PreparedStatement with object value.
     */
    @Override
    public void setJdbcObject(PreparedStatement statement, Double[] value,
                              int pos, int type, int scale) throws Exception {

        statement.setString(pos, toString(value));
    }

    /**
     * Reads an object from JDBC ResultSet column, converting it to class returned by
     * 'getClassName' method.
     *
     * @throws Exception if read error occurred, or an object can't be converted to a
     *                   target Java class.
     */
    @Override
    public Double[] materializeObject(ResultSet rs, int index, int type) throws Exception {
        return fromString(rs.getString(index));
    }

    /**
     * Reads an object from a stored procedure OUT parameter, converting it to class
     * returned by 'getClassName' method.
     *
     * @throws Exception if read error occurred, or an object can't be converted to a
     *                   target Java class.
     */
    @Override
    public Double[] materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return fromString(rs.getString(index));
    }

    /**
     * Converts the value to a String. Used for the database write, as well as by Cayenne for logging.
     */
    @Override
    public String toString(Double[] value) {
        return Arrays.stream(value).map(String::valueOf).collect(Collectors.joining(SEPARATOR));
    }

    private Double[] fromString(String string) {
        return Arrays.stream(string.split(SEPARATOR)).map(Double::valueOf).toArray(Double[]::new);
    }
}
// end::content[]
