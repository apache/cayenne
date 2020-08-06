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

package org.apache.cayenne.unit.jira;

import org.apache.cayenne.access.types.ExtendedType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CAY_207StringType1 implements ExtendedType<CAY_207String1> {

    @Override
    public String getClassName() {
        return CAY_207String1.class.getName();
    }

    @Override
    public CAY_207String1 materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        return new CAY_207String1(rs.getString(index));
    }

    @Override
    public CAY_207String1 materializeObject(ResultSet rs, int index, int type) throws Exception {
        return new CAY_207String1(rs.getString(index));
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            CAY_207String1 value,
            int pos,
            int type,
            int scale) throws Exception {

        if (scale != -1) {
            statement.setObject(pos, value.string, type, scale);
        }
        else {
            statement.setObject(pos, value.string, type);
        }
    }

    @Override
    public String toString(CAY_207String1 value) {
        if (value == null) {
            return "NULL";
        }

        return value.toString();
    }
}
