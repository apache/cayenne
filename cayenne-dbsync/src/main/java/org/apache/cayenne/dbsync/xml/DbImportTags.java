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

package org.apache.cayenne.dbsync.xml;

public class DbImportTags {

    public static final String OLD_CONFIG_TAG = "config";
    public static final String CONFIG_TAG = "dbImport";

    public static final String CATALOG_TAG = "catalog";
    public static final String SCHEMA_TAG = "schema";
    public static final String TABLE_TYPES_TAG = "tableTypes";
    public static final String DEFAULT_PACKAGE_TAG = "defaultPackage";
    public static final String FORCE_DATAMAP_CATALOG_TAG = "forceDataMapCatalog";
    public static final String FORCE_DATAMAP_SCHEMA_TAG = "forceDataMapSchema";
    public static final String MEANINGFUL_PK_TABLES_TAG = "meaningfulPkTables";
    public static final String NAMING_STRATEGY_TAG = "namingStrategy";
    public static final String SKIP_PK_LOADING_TAG = "skipPrimaryKeyLoading";
    public static final String SKIP_RELATIONSHIPS_LOADING_TAG = "skipRelationshipsLoading";
    public static final String STRIP_FROM_TABLE_NAMES_TAG = "stripFromTableNames";
    public static final String USE_JAVA7_TYPES_TAG = "useJava7Types";
    public static final String INCLUDE_TABLE_TAG = "includeTable";
    public static final String EXCLUDE_TABLE_TAG = "excludeTable";
    public static final String INCLUDE_COLUMN_TAG = "includeColumn";
    public static final String EXCLUDE_COLUMN_TAG = "excludeColumn";
    public static final String INCLUDE_PROCEDURE_TAG = "includeProcedure";
    public static final String EXCLUDE_PROCEDURE_TAG = "excludeProcedure";

    public static final String NAME_TAG = "name";

}
