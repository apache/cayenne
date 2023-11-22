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

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * @since 4.1
 */
public class ObjEntityHandler extends NamespaceAwareNestedTagHandler {

    private static final Logger logger = LoggerFactory.getLogger(ObjEntityHandler.class);

    private static final String OBJ_ENTITY_TAG = "obj-entity";
    private static final String OBJ_ATTRIBUTE_TAG = "obj-attribute";
    private static final String OBJ_ATTRIBUTE_OVERRIDE_TAG = "attribute-override";
    private static final String EMBEDDED_ATTRIBUTE_TAG = "embedded-attribute";
    private static final String QUALIFIER_TAG = "qualifier";

    // lifecycle listeners and callbacks related
    private static final String POST_ADD_TAG = "post-add";
    private static final String PRE_PERSIST_TAG = "pre-persist";
    private static final String POST_PERSIST_TAG = "post-persist";
    private static final String PRE_UPDATE_TAG = "pre-update";
    private static final String POST_UPDATE_TAG = "post-update";
    private static final String PRE_REMOVE_TAG = "pre-remove";
    private static final String POST_REMOVE_TAG = "post-remove";
    private static final String POST_LOAD_TAG = "post-load";

    private DataMap map;

    private ObjEntity entity;

    private ObjAttribute lastAttribute;

    public ObjEntityHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case OBJ_ENTITY_TAG:
                createObjEntity(attributes);
                return true;

            case OBJ_ATTRIBUTE_TAG:
                createObjAttribute(attributes);
                return true;

            case OBJ_ATTRIBUTE_OVERRIDE_TAG:
                processStartAttributeOverride(attributes);
                return true;

            case QUALIFIER_TAG:
                return true;

            case POST_ADD_TAG:
            case PRE_PERSIST_TAG:
            case POST_PERSIST_TAG:
            case PRE_UPDATE_TAG:
            case POST_UPDATE_TAG:
            case PRE_REMOVE_TAG:
            case POST_REMOVE_TAG:
            case POST_LOAD_TAG:
                createCallback(localName, attributes);
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        if(namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case EMBEDDED_ATTRIBUTE_TAG:
                    return new EmbeddableAttributeHandler(this, entity);
            }
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
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

    private void createObjEntity(Attributes attributes) {
        entity = new ObjEntity(attributes.getValue("name"));
        entity.setClassName(attributes.getValue("className"));
        entity.setAbstract(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("abstract")));
        entity.setReadOnly(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("readOnly")));
        if ("optimistic".equals(attributes.getValue("", "lock-type"))) {
            entity.setDeclaredLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        }

        String superEntityName = attributes.getValue("superEntityName");
        if (superEntityName != null) {
            entity.setSuperEntityName(superEntityName);
        } else {
            entity.setSuperClassName(attributes.getValue("superClassName"));
        }
        entity.setDbEntityName(attributes.getValue("dbEntityName"));

        map.addObjEntity(entity);
    }

    private void createObjAttribute(Attributes attributes) {
        String dbPath = attributes.getValue("db-attribute-path");
        if (dbPath == null) {
            dbPath = attributes.getValue("db-attribute-name");
        }

        lastAttribute = new ObjAttribute(attributes.getValue("name"));
        lastAttribute.setType(attributes.getValue("type"));
        lastAttribute.setUsedForLocking(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("lock")));
        lastAttribute.setLazy(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("lazy")));
        lastAttribute.setDbAttributePath(dbPath);
        entity.addAttribute(lastAttribute);
    }

    private void processStartAttributeOverride(Attributes attributes) {
        entity.addAttributeOverride(attributes.getValue("name"),
                attributes.getValue("db-attribute-path"));
    }

    private CallbackDescriptor getCallbackDescriptor(String type) {
        if (entity == null) {
            return null;
        }

        switch (type) {
            case POST_ADD_TAG:
                return entity.getCallbackMap().getPostAdd();
            case PRE_PERSIST_TAG:
                return entity.getCallbackMap().getPrePersist();
            case POST_PERSIST_TAG:
                return entity.getCallbackMap().getPostPersist();
            case PRE_UPDATE_TAG:
                return entity.getCallbackMap().getPreUpdate();
            case POST_UPDATE_TAG:
                return entity.getCallbackMap().getPostUpdate();
            case PRE_REMOVE_TAG:
                return entity.getCallbackMap().getPreRemove();
            case POST_REMOVE_TAG:
                return entity.getCallbackMap().getPostRemove();
            case POST_LOAD_TAG:
                return entity.getCallbackMap().getPostLoad();
        }

        return null;
    }

    private void createCallback(String type, Attributes attributes) {
        String methodName = attributes.getValue("method-name");
        CallbackDescriptor descriptor = getCallbackDescriptor(type);
        if(descriptor != null) {
            descriptor.addCallbackMethod(methodName);
        }
    }

    private void createQualifier(String qualifier) {
        if (isBlank(qualifier)) {
            return;
        }

        if (entity != null) {
            try {
                entity.setDeclaredQualifier(ExpressionFactory.exp(qualifier));
            } catch (ExpressionException ex) {
                logger.warn("Unable to parse entity " + entity.getName() + " qualifier", ex);
            }
        }
    }

    public ObjEntity getEntity() {
        return entity;
    }

    public ObjAttribute getLastAttribute() {
        return lastAttribute;
    }
}
