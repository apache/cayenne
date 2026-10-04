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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.ExpressionException;
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

    static final String DB_ENTITY_TAG = "dbEntity";
    private static final String DB_ATTRIBUTE_TAG = "dbAttribute";
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
        return switch (localName) {
            case DB_ENTITY_TAG -> {
                createDbEntity(attributes);
                yield true;
            }
            case DB_ATTRIBUTE_TAG -> {
                createDbAttribute(attributes);
                yield true;
            }
            case QUALIFIER_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (QUALIFIER_TAG.equals(localName)) {
            createQualifier(data);
        }
        return true;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        if (DbKeyGeneratorHandler.DB_KEY_GENERATOR_TAG.equals(localName)) {
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

        String scale = attributes.getValue("scale");
        if (scale != null) {
            lastAttribute.setScale(Integer.parseInt(scale));
        }

        lastAttribute.setPrimaryKey("true".equals(attributes.getValue("primaryKey")));
        lastAttribute.setMandatory("true".equals(attributes.getValue("mandatory")));
        lastAttribute.setGenerated("true".equals(attributes.getValue("generated")));
    }

    private void createQualifier(String qualifier) {
        if (isBlank(qualifier)) {
            return;
        }

        if (entity != null) {
            try {
                entity.setQualifier(ExpressionFactory.exp(qualifier));
            } catch (ExpressionException ex) {
                throw new ConfigurationException("Invalid qualifier of DbEntity '%s': %s", ex, entity.getName(),
                        qualifier);
            }
        }
    }

    public DbEntity getEntity() {
        return entity;
    }

    public DbAttribute getLastAttribute() {
        return lastAttribute;
    }
}
