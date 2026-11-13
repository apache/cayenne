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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.dbimport.ExcludeColumn;
import org.apache.cayenne.project.dbimport.ExcludeProcedure;
import org.apache.cayenne.project.dbimport.ExcludeTable;
import org.apache.cayenne.project.dbimport.FilterContainer;
import org.apache.cayenne.project.dbimport.IncludeColumn;
import org.apache.cayenne.project.dbimport.IncludeProcedure;
import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * Reads the "dbImport" tag of a DataMap. Also holds the tag names and the rules shared with the nested tag handlers.
 */
final class DbImportHandler extends NamespaceAwareNestedTagHandler {

    static final String DB_IMPORT_TAG = "dbImport";
    static final String NAME_TAG = "name";
    static final String INCLUDE_COLUMN_TAG = "includeColumn";
    static final String EXCLUDE_COLUMN_TAG = "excludeColumn";
    private static final String EXCLUDE_TABLE_TAG = "excludeTable";
    private static final String INCLUDE_PROCEDURE_TAG = "includeProcedure";
    private static final String EXCLUDE_PROCEDURE_TAG = "excludeProcedure";
    private static final String DEFAULT_PACKAGE_TAG = "defaultPackage";
    private static final String FORCE_DATA_MAP_CATALOG_TAG = "forceDataMapCatalog";
    private static final String FORCE_DATA_MAP_SCHEMA_TAG = "forceDataMapSchema";
    private static final String MEANINGFUL_PK_TABLES_TAG = "meaningfulPkTables";
    private static final String NAMING_STRATEGY_TAG = "namingStrategy";
    private static final String SKIP_PRIMARY_KEY_LOADING_TAG = "skipPrimaryKeyLoading";
    private static final String SKIP_RELATIONSHIPS_LOADING_TAG = "skipRelationshipsLoading";
    private static final String STRIP_FROM_TABLE_NAMES_TAG = "stripFromTableNames";
    private static final String USE_JAVA7_TYPES_TAG = "useJava7Types";

    private final DataMap dataMap;
    private ReverseEngineering reverseEngineering;

    DbImportHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap dataMap) {
        super(parentHandler);
        this.dataMap = dataMap;
    }

    /**
     * Returns true for the pattern tags shared by "dbImport", "catalog" and "schema".
     */
    static boolean isFilterTag(String tag) {
        return switch (tag) {
            case EXCLUDE_TABLE_TAG, INCLUDE_COLUMN_TAG, EXCLUDE_COLUMN_TAG, INCLUDE_PROCEDURE_TAG,
                 EXCLUDE_PROCEDURE_TAG -> true;
            default -> false;
        };
    }

    static void loadFilter(FilterContainer container, String tag, String pattern) {
        switch (tag) {
            case EXCLUDE_TABLE_TAG -> container.addExcludeTable(new ExcludeTable(pattern));
            case INCLUDE_COLUMN_TAG -> container.addIncludeColumn(new IncludeColumn(pattern));
            case EXCLUDE_COLUMN_TAG -> container.addExcludeColumn(new ExcludeColumn(pattern));
            case INCLUDE_PROCEDURE_TAG -> container.addIncludeProcedure(new IncludeProcedure(pattern));
            case EXCLUDE_PROCEDURE_TAG -> container.addExcludeProcedure(new ExcludeProcedure(pattern));
        }
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case DB_IMPORT_TAG -> {
                reverseEngineering = new ReverseEngineering();
                dataMap.setReverseEngineering(reverseEngineering);
                yield true;
            }
            case DEFAULT_PACKAGE_TAG, FORCE_DATA_MAP_CATALOG_TAG, FORCE_DATA_MAP_SCHEMA_TAG, MEANINGFUL_PK_TABLES_TAG,
                 NAMING_STRATEGY_TAG, SKIP_PRIMARY_KEY_LOADING_TAG, SKIP_RELATIONSHIPS_LOADING_TAG,
                 STRIP_FROM_TABLE_NAMES_TAG, USE_JAVA7_TYPES_TAG -> true;
            default -> isFilterTag(localName);
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        // blank values keep the defaults
        if (data.isBlank()) {
            return true;
        }

        switch (localName) {
            case DEFAULT_PACKAGE_TAG -> reverseEngineering.setDefaultPackage(data);
            case FORCE_DATA_MAP_CATALOG_TAG -> reverseEngineering.setForceDataMapCatalog("true".equals(data));
            case FORCE_DATA_MAP_SCHEMA_TAG -> reverseEngineering.setForceDataMapSchema("true".equals(data));
            case MEANINGFUL_PK_TABLES_TAG -> reverseEngineering.setMeaningfulPkTables(data);
            case NAMING_STRATEGY_TAG -> reverseEngineering.setNamingStrategy(data);
            case SKIP_PRIMARY_KEY_LOADING_TAG -> reverseEngineering.setSkipPrimaryKeyLoading("true".equals(data));
            case SKIP_RELATIONSHIPS_LOADING_TAG -> reverseEngineering.setSkipRelationshipsLoading("true".equals(data));
            case STRIP_FROM_TABLE_NAMES_TAG -> reverseEngineering.setStripFromTableNames(data);
            case USE_JAVA7_TYPES_TAG -> reverseEngineering.setUseJava7Types("true".equals(data));
            default -> loadFilter(reverseEngineering, localName, data);
        }
        return true;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace)) {
            return switch (localName) {
                case DbImportCatalogHandler.CATALOG_TAG -> new DbImportCatalogHandler(this, reverseEngineering);
                case DbImportSchemaHandler.SCHEMA_TAG -> new DbImportSchemaHandler(this, reverseEngineering);
                case DbImportIncludeTableHandler.INCLUDE_TABLE_TAG ->
                        new DbImportIncludeTableHandler(this, reverseEngineering);
                case DbImportTableTypesHandler.TABLE_TYPES_TAG ->
                        new DbImportTableTypesHandler(this, reverseEngineering);
                default -> super.createChildTagHandler(namespaceURI, localName, qName, attributes);
            };
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }
}
