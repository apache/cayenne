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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.resource.Resource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class DataMapHandler extends NamespaceAwareNestedTagHandler {

    static final String DATA_MAP_TAG = "dataMap";

    private final Resource configurationSource;
    private DataMap dataMap;

    /**
     * @param configurationSource the source of the DataMap being loaded, can be null
     */
    public DataMapHandler(NamespaceAwareNestedTagHandler parentHandler, Resource configurationSource) {
        super(parentHandler);
        this.configurationSource = configurationSource;
    }

    public DataMapHandler(LoaderContext loaderContext) {
        super(loaderContext);
        setTargetNamespace(DataMap.SCHEMA_XSD);
        this.configurationSource = null;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName,
                                     Attributes attributes) throws SAXException {
        return switch (localName) {
            case DATA_MAP_TAG -> {
                this.dataMap = new DataMap();
                dataMap.setConfigurationSource(configurationSource);
                dataMap.setDefaultLockType("optimistic".equals(attributes.getValue("defaultLockType"))
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
                dataMap.setDefaultPackage(attributes.getValue("defaultPackage"));
                dataMap.setDefaultCatalog(attributes.getValue("defaultCatalog"));
                dataMap.setDefaultSchema(attributes.getValue("defaultSchema"));
                dataMap.setDefaultSuperclass(attributes.getValue("defaultSuperclass"));
                dataMap.setQuotingSQLIdentifiers("true".equals(attributes.getValue("quoteSqlIdentifiers")));
                yield true;
            }
            default -> false;
        };
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace)) {
            return switch (localName) {
                case DbEntityHandler.DB_ENTITY_TAG -> new DbEntityHandler(this, dataMap);
                case ObjEntityHandler.OBJ_ENTITY_TAG -> new ObjEntityHandler(this, dataMap);
                case DbRelationshipHandler.DB_RELATIONSHIP_TAG -> new DbRelationshipHandler(this, dataMap);
                case ObjRelationshipHandler.OBJ_RELATIONSHIP_TAG -> new ObjRelationshipHandler(this, dataMap);
                case ProcedureHandler.PROCEDURE_TAG -> new ProcedureHandler(this, dataMap);
                case QueryDescriptorHandler.OBJECT_QUERY_TAG,
                     QueryDescriptorHandler.SQL_QUERY_TAG,
                     QueryDescriptorHandler.PROCEDURE_QUERY_TAG -> new QueryDescriptorHandler(this, dataMap);
                case EmbeddableHandler.EMBEDDABLE_TAG -> new EmbeddableHandler(this, dataMap);
                default -> super.createChildTagHandler(namespaceURI, localName, qName, attributes);
            };
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected void beforeScopeEnd() {
        loaderContext.dataMapLoaded(dataMap);
    }

    public DataMap getDataMap() {
        return dataMap;
    }
}
