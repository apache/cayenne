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

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Extended type factory that produces types for Cayenne internal value types that could potentially make it to the DB.
 * ExtendedTypes that produced by this factory just trying to throw user-friendly exception instead of unknown type one.
 *
 * @since 4.2
 */
public class InternalUnsupportedTypeFactory implements ExtendedTypeFactory {

    @Override
    public ExtendedType<?> getType(Class<?> objectClass) {
        if(Marker.class.isAssignableFrom(objectClass)) {
            return new ExtendedType<Marker>() {
                @Override
                public String getClassName() {
                    return objectClass.getName();
                }

                @Override
                public void setJdbcObject(PreparedStatement statement, Marker value, int pos, int type, int scale) {
                    throw new CayenneRuntimeException(value.errorMessage());
                }

                @Override
                public Marker materializeObject(ResultSet rs, int index, int type) {
                    // this normally shouldn't happen
                    throw new CayenneRuntimeException("Trying to materialize internal Cayenne value. Check your mapping or report an issue.");
                }

                @Override
                public Marker materializeObject(CallableStatement rs, int index, int type) {
                    // this normally shouldn't happen
                    throw new CayenneRuntimeException("Trying to materialize internal Cayenne value. Check your mapping or report an issue.");
                }

                @Override
                public String toString(Marker value) {
                    return "Internal marker of type " + objectClass.getSimpleName();
                }
            };
        }
        return null;
    }

    /**
     * Marker interface, that should be used by any internal value types, that could potentially get to the SQL
     */
    public interface Marker {
        /**
         *  Error message in case this object made it to the DB
         */
        default String errorMessage() {
            return "Trying to use internal type in the query.";
        }
    }
}
