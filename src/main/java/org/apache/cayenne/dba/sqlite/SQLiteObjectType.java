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

import org.apache.cayenne.access.types.AbstractType;
import org.apache.cayenne.access.types.DefaultType;

/**
 * This ExtendedType is used by SQLite as often the types of columns of the result sets
 * can't be determined on the fly, and {@link DefaultType} for Object class throws on
 * NULLs.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class SQLiteObjectType extends AbstractType {

    public String getClassName() {
        return Object.class.getName();
    }

    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        return rs.getObject(index);
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        return rs.getObject(index);
    }

}
