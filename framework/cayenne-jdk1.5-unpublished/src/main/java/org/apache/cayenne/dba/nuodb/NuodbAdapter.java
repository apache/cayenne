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

package org.apache.cayenne.dba.nuodb;

import java.sql.PreparedStatement;
import java.sql.Types;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.BooleanType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * @since 3.0
 */
public class NuodbAdapter extends JdbcAdapter {

    public NuodbAdapter() {
        setSupportsGeneratedKeys(true);
    }
    
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // configure boolean type to work with numeric columns
        map.registerType(new NuodbCharType());
        
        // configure boolean type to work with numeric columns
        map.registerType(new NuodbBooleanType());
    }
    
    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new NuodbActionBuilder(this, node.getEntityResolver()));
    }
    
    /**
     * Appends SQL for column creation to CREATE TABLE buffer.
     * 
     * @since 1.2
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        super.createTableAppendColumn(sqlBuffer, column);
        
        if (column.isPrimaryKey()) {
        	sqlBuffer.append(" generated always as identity primary key");
        }
    }

    /**
     * @since 1.2
     */
    @Override
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {
    }
    
    final class NuodbCharType extends CharType {
        
        NuodbCharType() {
            super(true, true);
        }
        
        @Override
        public void setJdbcObject(
                PreparedStatement st,
                Object value,
                int pos,
                int type,
                int scale) throws Exception {
            
            if (value == null) {
                st.setNull(pos, type);
            }
            else if (type == Types.CLOB) {
                st.setString(pos, (String) value);
            }
            else if (type == Types.BIGINT) {
                st.setLong(pos, Long.parseLong(value.toString()));
            }
            else if (type == Types.INTEGER) {
                st.setInt(pos, Integer.parseInt(value.toString()));
            }
            else if (scale != -1) {
                st.setObject(pos, value, type, scale);
            }
            else {
                st.setObject(pos, value, type);
            }
        }
    }
    
    final class NuodbBooleanType extends BooleanType {

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
            else if (type == Types.BIT || type == Types.BOOLEAN) {
                boolean flag = Boolean.TRUE.equals(val);
                st.setBoolean(pos, flag);
            }
            else if (type == Types.INTEGER) {
                int intVal = Boolean.TRUE.equals(val) ? 1 : 0;
                st.setInt(pos, intVal);
            }
            else {
                st.setObject(pos, val, type);
            }
        }
    }
}
