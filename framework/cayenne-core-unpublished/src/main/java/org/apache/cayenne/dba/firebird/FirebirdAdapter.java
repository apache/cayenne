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

package org.apache.cayenne.dba.firebird;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * DbAdapter implementation for <a href="http://www.firebirdsql.org">FirebirdSQL
 * RDBMS </a>. Sample connection settings to use with Firebird are shown
 * below:
 * 
 * <pre>
 *      firebird.cayenne.adapter = org.apache.cayenne.dba.firebird.FirebirdAdapter
 *      firebird.jdbc.username = test
 *      firebird.jdbc.password = secret
 *      firebird.jdbc.url = jdbc:firebirdsql:localhost:/home/firebird/test.fdb  
 *      firebird.jdbc.driver = org.firebirdsql.jdbc.FBDriver
 * </pre>
 */
public class FirebirdAdapter extends JdbcAdapter {

    public FirebirdAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories);
	    setSupportsBatchUpdates(true);
    }
    
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        // handling internaly binary types as blobs or clobs generates exceptions
        // Blob.length() and Clob.length() methods are optional (http://docs.oracle.com/javase/7/docs/api/java/sql/Clob.html#length())
        // and firebird driver doesn't support them.
        map.registerType(new ByteArrayType(true, false));
        map.registerType(new CharType(true, false));
        
    }
    
    public FirebirdMergerFactory mergerFactory() {
        return new FirebirdMergerFactory();
    }
    
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        String[] types = externalTypesForJdbcType(column.getType());
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null ? ((DbEntity) column.getEntity()).getFullyQualifiedName()
                    : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '" + entityName + "." + column.getName()
                    + "': " + column.getType());
        }

        String type = types[0];
        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ').append(type);

        // append size and precision (if applicable)s
        if (TypesMapping.supportsLength(column.getType())) {
            int len = column.getMaxLength();

            int scale = (TypesMapping.isDecimal(column.getType()) && column.getType() != Types.FLOAT) ? column
                    .getScale() : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                sqlBuffer.append('(').append(len);

                if (scale >= 0) {
                    sqlBuffer.append(", ").append(scale);
                }

                sqlBuffer.append(')');
            }
        }

        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : "");
    }
    
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        QualifierTranslator translator = new FirebirdQualifierTranslator(queryAssembler);
        return translator;
    }


}
