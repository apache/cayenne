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
package org.apache.cayenne.dba.sqlite;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Date;

import org.apache.cayenne.access.types.UtilDateType;

/**
 * Implements special date handling for SQLite. See
 * http://www.zentus.com/sqlitejdbc/usage.html for details.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
// TODO: andrus 10/10/2007 - most of this is bogus... see http://www.zentus.com/sqlitejdbc/usage.html 
// for how dates should be handled (without relying on the driver).
class SQLiteDateType extends UtilDateType {

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {

        Date val = null;

        switch (type) {
            case Types.TIMESTAMP:
                val = rs.getTimestamp(index);
                break;
            case Types.DATE:
                val = rs.getDate(index);
                break;
            case Types.TIME:
                val = rs.getTime(index);
                break;
            default:
                // here instead of failing like the super does, simply attempt to convert
                // to timestamp (note that 'getObject' may return a String... SQLite is
                // not very robust in type conversions.
                val = rs.getTimestamp(index);
                break;
        }

        return (rs.wasNull()) ? null : new Date(val.getTime());
    }

    public Object materializeObject(CallableStatement cs, int index, int type)
            throws Exception {
        Object val = null;

        switch (type) {
            case Types.TIMESTAMP:
                val = cs.getTimestamp(index);
                break;
            case Types.DATE:
                val = cs.getDate(index);
                break;
            case Types.TIME:
                val = cs.getTime(index);
                break;
            default:
                // here instead of failing like the super does, simply attempt to convert
                // to timestamp (note that 'getObject' may return a String... SQLite is
                // not very robust in type conversions.
                val = cs.getTimestamp(index);
                break;
        }

        return (cs.wasNull()) ? null : new java.util.Date(((java.util.Date) val)
                .getTime());
    }
}
