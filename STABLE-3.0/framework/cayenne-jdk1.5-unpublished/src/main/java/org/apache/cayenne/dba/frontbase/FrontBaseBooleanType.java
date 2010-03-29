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

package org.apache.cayenne.dba.frontbase;

import java.sql.PreparedStatement;
import java.sql.Types;

import org.apache.cayenne.access.types.BooleanType;

/**
 * Overrides default BooleanType behavior to bind BOOLEAN type to PreparedStatements via
 * "setObject", as binding via "setBoolean" only works for BIT.
 * 
 * @since 1.2
 */
class FrontBaseBooleanType extends BooleanType {

    @Override
    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision) throws Exception {

        if (val == null) {
            st.setNull(pos, type);
        }
        else if (type == Types.BIT) {
            boolean flag = Boolean.TRUE.equals(val);
            st.setBoolean(pos, flag);
        }
        else {
            st.setObject(pos, val, type);
        }
    }
}
