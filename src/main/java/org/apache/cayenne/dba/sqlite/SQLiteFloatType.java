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

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class SQLiteFloatType extends AbstractType {

    public String getClassName() {
        return Float.class.getName();
    }

    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        // the driver throws an NPE on 'getFloat' if the value is null, so must read it as
        // an object.
        Number n = (Number) rs.getObject(index);
        return (n == null) ? null : new Float(n.floatValue());
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        // the driver throws an NPE on 'getFloat' if the value is null, so must read it as
        // an object.
        Number n = (Number) rs.getObject(index);
        return (n == null) ? null : new Float(n.floatValue());
    }
}
