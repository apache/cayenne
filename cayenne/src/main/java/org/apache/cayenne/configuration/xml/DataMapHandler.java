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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class DataMapHandler extends NamespaceAwareNestedTagHandler {

    /* This constants must be in sync with dataMap.xsd schema */
    private static final String DATA_MAP_TAG         = "dataMap";
    private static final String DB_ENTITY_TAG        = "dbEntity";
    private static final String OBJ_ENTITY_TAG       = "objEntity";
    private static final String DB_RELATIONSHIP_TAG  = "dbRelationship";
    private static final String OBJ_RELATIONSHIP_TAG = "objRelationship";
    private static final String EMBEDDABLE_TAG       = "embeddable";
    private static final String PROCEDURE_TAG        = "procedure";
    private static final String OBJECT_QUERY_TAG     = "objectQuery";
    private static final String SQL_QUERY_TAG        = "sqlQuery";
    private static final String PROCEDURE_QUERY_TAG  = "procedureQuery";

    public static final String TRUE = "true";

    private DataMap dataMap;

    public DataMapHandler(NamespaceAwareNestedTagHandler parentHandler) {
        super(parentHandler);
    }

    public DataMapHandler(LoaderContext loaderContext) {
        super(loaderContext);
        setTargetNamespace(DataMap.SCHEMA_XSD);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName,
                                     Attributes attributes) throws SAXException {
        return switch (localName) {
            case DATA_MAP_TAG -> {
                this.dataMap = new DataMap();
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
                case DB_ENTITY_TAG -> new DbEntityHandler(this, dataMap);
                case OBJ_ENTITY_TAG -> new ObjEntityHandler(this, dataMap);
                case DB_RELATIONSHIP_TAG -> new DbRelationshipHandler(this, dataMap);
                case OBJ_RELATIONSHIP_TAG -> new ObjRelationshipHandler(this, dataMap);
                case PROCEDURE_TAG -> new ProcedureHandler(this, dataMap);
                case OBJECT_QUERY_TAG, SQL_QUERY_TAG, PROCEDURE_QUERY_TAG -> new QueryDescriptorHandler(this, dataMap);
                case EMBEDDABLE_TAG -> new EmbeddableHandler(this, dataMap);
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
