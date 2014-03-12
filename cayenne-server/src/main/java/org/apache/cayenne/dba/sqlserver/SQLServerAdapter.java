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

package org.apache.cayenne.dba.sqlserver;

import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * Cayenne DbAdapter implementation for <a href="http://www.microsoft.com/sql/"Microsoft
 * SQL Server </a> engine. </p> <h3>Microsoft Driver Settings</h3>
 * <p>
 * Sample connection settings to use with MS SQL Server are shown below:
 * 
 * <pre>
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:sqlserver://192.168.0.65;databaseName=cayenne;SelectMethod=cursor
 *       sqlserver.jdbc.driver = com.microsoft.sqlserver.jdbc.SQLServerDriver
 * </pre>
 * <p>
 * <i>Note on case-sensitive LIKE: if your application requires case-sensitive LIKE
 * support, ask your DBA to configure the database to use a case-senstitive collation (one
 * with "CS" in symbolic collation name instead of "CI", e.g.
 * "SQL_Latin1_general_CP1_CS_AS"). </i>
 * </p>
 * <h3>jTDS Driver Settings</h3>
 * <p>
 * jTDS is an open source driver that can be downloaded from <a href=
 * "http://jtds.sourceforge.net">http://jtds.sourceforge.net </a>. It supports both
 * SQLServer and Sybase. Sample SQLServer settings are the following:
 * </p>
 * 
 * <pre>
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:jtds:sqlserver://192.168.0.65/cayenne
 *       sqlserver.jdbc.driver = net.sourceforge.jtds.jdbc.Driver
 * </pre>
 * 
 * @since 1.1
 */
public class SQLServerAdapter extends SybaseAdapter {

    public static final String TRIM_FUNCTION = "RTRIM";

    public SQLServerAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ResourceLocator resourceLocator) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories, resourceLocator);

        // TODO: i wonder if Sybase supports generated keys...
        // in this case we need to move this to the super.
        this.setSupportsGeneratedKeys(true);
        this.setSupportsBatchUpdates(true);
    }

    /**
     * Uses SQLServerActionBuilder to create the right action.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new SQLServerActionBuilder(this, node.getEntityResolver(), node
                .getRowReaderFactory()));
    }

    /**
     * Returns a trimming translator.
     */
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        QualifierTranslator translator = new SQLServerTrimmingQualifierTranslator(
                queryAssembler,
                SQLServerAdapter.TRIM_FUNCTION);
        translator.setCaseInsensitive(caseInsensitiveCollations);
        return translator;
    }

    /**
     * Overrides super implementation to correctly set up identity columns.
     * 
     * @since 1.2
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        super.createTableAppendColumn(sqlBuffer, column);

        if (column.isGenerated()) {
            // current limitation - we don't allow to set identity parameters...
            sqlBuffer.append(" IDENTITY (1, 1)");
        }
    }

    @Override
    public MergerFactory mergerFactory() {
        return new SQLServerMergerFactory();
    }

}
