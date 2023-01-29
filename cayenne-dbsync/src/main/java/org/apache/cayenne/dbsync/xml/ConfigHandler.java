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

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;


/**
 * @since 4.1
 */
class ConfigHandler extends NamespaceAwareNestedTagHandler {
    private static final String TRUE = "true";
    private ReverseEngineering configuration;
    private final DataChannelMetaData metaData;

    ConfigHandler(NamespaceAwareNestedTagHandler parentHandler, DataChannelMetaData metaData) {
        super(parentHandler);
        this.metaData = metaData;
        this.targetNamespace = DbImportExtension.NAMESPACE;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case DbImportTags.CONFIG_TAG:
            case DbImportTags.OLD_CONFIG_TAG:
                createConfig();
                return true;
        }
        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {
        if (namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case DbImportTags.CATALOG_TAG:
                    return new CatalogHandler(this, configuration);
                case DbImportTags.SCHEMA_TAG:
                    return new SchemaHandler(this, configuration);
                case DbImportTags.TABLE_TYPES_TAG:
                    return new TableTypesHandler(this, configuration);
                case DbImportTags.INCLUDE_TABLE_TAG:
                    return new IncludeTableHandler(this, configuration);
                case DbImportTags.EXCLUDE_TABLE_TAG:
                case DbImportTags.INCLUDE_COLUMN_TAG:
                case DbImportTags.EXCLUDE_COLUMN_TAG:
                case DbImportTags.INCLUDE_PROCEDURE_TAG:
                case DbImportTags.EXCLUDE_PROCEDURE_TAG:
                    return new PatternParamHandler(this, configuration);
            }
        }
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case DbImportTags.DEFAULT_PACKAGE_TAG:
                createDefaultPackage(data);
                break;
            case DbImportTags.FORCE_DATAMAP_CATALOG_TAG:
                createForceDatamapCatalog(data);
                break;
            case DbImportTags.FORCE_DATAMAP_SCHEMA_TAG:
                createForceDatamapSchema(data);
                break;
            case DbImportTags.MEANINGFUL_PK_TABLES_TAG:
                createMeaningfulPkTables(data);
                break;
            case DbImportTags.NAMING_STRATEGY_TAG:
                createNamingStrategy(data);
                break;
            case DbImportTags.SKIP_PK_LOADING_TAG:
                createSkipPkLoading(data);
                break;
            case DbImportTags.SKIP_RELATIONSHIPS_LOADING_TAG:
                createSkipRelationshipsLoading(data);
                break;
            case DbImportTags.STRIP_FROM_TABLE_NAMES_TAG:
                createStripFromTableNames(data);
                break;
            case DbImportTags.USE_JAVA7_TYPES_TAG:
                createUseJava7Types(data);
                break;
        }
        return true;
    }

    private void createUseJava7Types(String useJava7Types) {
        if (!useJava7Types.trim().isEmpty() && configuration != null) {
            configuration.setUseJava7Types(useJava7Types.equals(TRUE));
        }
    }

    private void createStripFromTableNames(String stripFromTableNames) {
        if (!stripFromTableNames.trim().isEmpty() && configuration != null) {
            configuration.setStripFromTableNames(stripFromTableNames);
        }
    }

    private void createSkipRelationshipsLoading(String skipRelationshipsLoading) {
        if (!skipRelationshipsLoading.trim().isEmpty() && configuration != null) {
            configuration.setSkipRelationshipsLoading(skipRelationshipsLoading.equals(TRUE));
        }
    }

    private void createSkipPkLoading(String skipPkLoading) {
        if (!skipPkLoading.trim().isEmpty() && configuration != null) {
            configuration.setSkipPrimaryKeyLoading(skipPkLoading.equals(TRUE));
        }
    }

    private void createNamingStrategy(String namingStrategy) {
        if (!namingStrategy.trim().isEmpty() && configuration != null) {
            configuration.setNamingStrategy(namingStrategy);
        }
    }

    private void createMeaningfulPkTables(String meaningfulPkTables) {
        if (!meaningfulPkTables.trim().isEmpty() && configuration != null) {
            configuration.setMeaningfulPkTables(meaningfulPkTables);
        }
    }

    private void createForceDatamapSchema(String forceDatamapSchema) {
        if (!forceDatamapSchema.trim().isEmpty() && configuration != null) {
            configuration.setForceDataMapSchema(forceDatamapSchema.equals(TRUE));
        }
    }

    private void createForceDatamapCatalog(String forceDatamapCatalog) {
        if (!forceDatamapCatalog.trim().isEmpty() && configuration != null) {
            configuration.setForceDataMapCatalog(forceDatamapCatalog.equals(TRUE));
        }
    }

    private void createDefaultPackage(String defaultPackage) {
        if (!defaultPackage.trim().isEmpty() && configuration != null) {
            configuration.setDefaultPackage(defaultPackage);
        }
    }

    private void createConfig() {
        configuration = new ReverseEngineering();
        loaderContext.addDataMapListener(dataMap ->
                ConfigHandler.this.metaData.add(dataMap, configuration));
    }
}
