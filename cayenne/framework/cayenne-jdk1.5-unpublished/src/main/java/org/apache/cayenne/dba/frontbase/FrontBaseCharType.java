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

import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.util.MemoryClob;

/**
 * A char type that uses a real clob for insertion.
 * 
 * @since 1.2
 */
// actually this is the way CLOBs must be handled by default, but there are still some
// issues with other adapters, so we can't move this to a superclass yet.
class FrontBaseCharType extends CharType {

    FrontBaseCharType() {
        super(false, true);
    }

    @Override
    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision) throws Exception {

        if (type == Types.CLOB) {
            st.setClob(pos, writeClob((String) val));
        }
        else {
            super.setJdbcObject(st, val, pos, type, precision);
        }
    }

    Clob writeClob(String string) {
        return string != null ? new MemoryClob(string) : null;
    }
}
