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

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * @since 4.1
 */
public class DbEntityHandler extends NamespaceAwareNestedTagHandler {

    private static final String DB_ENTITY_TAG = "db-entity";
    private static final String DB_ATTRIBUTE_TAG = "db-attribute";
    private static final String DB_KEY_GENERATOR_TAG = "db-key-generator";
    private static final String QUALIFIER_TAG = "qualifier";

    private DataMap dataMap;
    private DbEntity entity;
    private DbAttribute lastAttribute;

    public DbEntityHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap dataMap) {
        super(parentHandler);
        this.dataMap = dataMap;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case DB_ENTITY_TAG:
                createDbEntity(attributes);
                return true;

            case DB_ATTRIBUTE_TAG:
                createDbAttribute(attributes);
                return true;

            case QUALIFIER_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case QUALIFIER_TAG:
                createQualifier(data);
                break;
        }
        return true;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        switch (localName) {
            case DB_KEY_GENERATOR_TAG:
                return new DbKeyGeneratorHandler(this, entity);
        }
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    private void createDbEntity(Attributes attributes) {
        String name = attributes.getValue("name");
        entity = new DbEntity(name);
        entity.setSchema(attributes.getValue("schema"));
        entity.setCatalog(attributes.getValue("catalog"));
        dataMap.addDbEntity(entity);
    }

    private void createDbAttribute(Attributes attributes) {
        String name = attributes.getValue("name");
        String type = attributes.getValue("type");

        lastAttribute = new DbAttribute(name);
        lastAttribute.setType(TypesMapping.getSqlTypeByName(type));
        entity.addAttribute(lastAttribute);

        String length = attributes.getValue("length");
        if (length != null) {
            lastAttribute.setMaxLength(Integer.parseInt(length));
        }

        String precision = attributes.getValue("attributePrecision");
        if (precision != null) {
            lastAttribute.setAttributePrecision(Integer.parseInt(precision));
        }

        // this is an obsolete 1.2 'precision' attribute that really meant 'scale'
        String pseudoPrecision = attributes.getValue("precision");
        if (pseudoPrecision != null) {
            lastAttribute.setScale(Integer.parseInt(pseudoPrecision));
        }

        String scale = attributes.getValue("scale");
        if (scale != null) {
            lastAttribute.setScale(Integer.parseInt(scale));
        }

        lastAttribute.setPrimaryKey(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("isPrimaryKey")));
        lastAttribute.setMandatory(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("isMandatory")));
        lastAttribute.setGenerated(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("isGenerated")));
    }

    private void createQualifier(String qualifier) {
        if (isBlank(qualifier)) {
            return;
        }

        // qualifier can belong to ObjEntity, DbEntity or a query
        if (entity != null) {
            entity.setQualifier(ExpressionFactory.exp(qualifier));
        }
    }

    public DbEntity getEntity() {
        return entity;
    }

    public DbAttribute getLastAttribute() {
        return lastAttribute;
    }
}
