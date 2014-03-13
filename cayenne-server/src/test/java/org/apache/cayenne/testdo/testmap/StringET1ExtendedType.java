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
package org.apache.cayenne.testdo.testmap;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.cayenne.access.types.ExtendedType;

public class StringET1ExtendedType implements ExtendedType {

    @Override
    public String getClassName() {
        return StringET1.class.getName();
    }

    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        String string = rs.getString(index);
        return string != null ? new StringET1(string) : null;
    }

    @Override
    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        String string = rs.getString(index);
        return string != null ? new StringET1(string) : null;
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value instanceof StringET1) {
            statement.setString(pos, ((StringET1) value).getString());
        }
        else {
            statement.setNull(pos, type);
        }
    }
}
