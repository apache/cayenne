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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.resource.ResourceLocator;

import java.util.List;

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

    private static final String NCHAR_SUFFIX = " CHARACTER SET UNICODE_FSS";

    public FirebirdAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                resourceLocator);
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
            String entityName = column.getEntity() != null ? column.getEntity().getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '" + entityName + "." + column.getName()
                    + "': " + column.getType());
        }

        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ');

        String type = types[0];
        String length = sizeAndPrecision(this, column);

        int suffixIndex = type.indexOf(NCHAR_SUFFIX);
        if (!length.isEmpty() && suffixIndex > 0) {
            sqlBuffer.append(type.substring(0, suffixIndex)).append(length).append(NCHAR_SUFFIX);
        } else {
            sqlBuffer.append(type).append(" ").append(length);
        }

        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : "");
    }
    
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new FirebirdQualifierTranslator(queryAssembler);
    }


}
