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

package org.apache.cayenne.dba.sybase;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;

/** 
 * DbAdapter implementation for <a href="http://www.sybase.com">Sybase RDBMS</a>.
 *
 * @author Andrus Adamchik
 */
public class SybaseAdapter extends JdbcAdapter {

    /**
     * Returns word "go".
     * 
     * @since 1.0.4
     */
    public String getBatchTerminator() {
        return "go";
    }
    
    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // create specially configured ByteArrayType handler
        map.registerType(new ByteArrayType(true, false));

        // address Sybase driver inability to handle java.lang.Short and java.lang.Byte
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
    }

    /** 
     * Creates and returns a primary key generator. 
     * Overrides superclass implementation to return an
     * instance of SybasePkGenerator.
     */
    protected PkGenerator createPkGenerator() {
        return new SybasePkGenerator();
    }

    public void bindParameter(
        PreparedStatement statement,
        Object object,
        int pos,
        int sqlType,
        int precision)
        throws SQLException, Exception {

        // Sybase driver doesn't like CLOBs and BLOBs as parameters
        if (object == null) {
            if (sqlType == Types.CLOB) {
                sqlType = Types.VARCHAR;
            }
            else if (sqlType == Types.BLOB) {
                sqlType = Types.VARBINARY;
            }
        }

        super.bindParameter(statement, object, pos, sqlType, precision);
    }
}
