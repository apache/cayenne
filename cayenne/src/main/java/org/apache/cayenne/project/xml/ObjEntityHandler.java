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
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * @since 4.1
 */
public class ObjEntityHandler extends NamespaceAwareNestedTagHandler {

    static final String OBJ_ENTITY_TAG = "objEntity";
    private static final String OBJ_ATTRIBUTE_TAG = "objAttribute";
    private static final String OBJ_ATTRIBUTE_OVERRIDE_TAG = "attributeOverride";
    private static final String QUALIFIER_TAG = "qualifier";

    // lifecycle listeners and callbacks related
    private static final String POST_ADD_TAG = "postAdd";
    private static final String PRE_PERSIST_TAG = "prePersist";
    private static final String POST_PERSIST_TAG = "postPersist";
    private static final String PRE_UPDATE_TAG = "preUpdate";
    private static final String POST_UPDATE_TAG = "postUpdate";
    private static final String PRE_REMOVE_TAG = "preRemove";
    private static final String POST_REMOVE_TAG = "postRemove";
    private static final String POST_LOAD_TAG = "postLoad";

    private DataMap map;

    private ObjEntity entity;

    private ObjAttribute lastAttribute;

    public ObjEntityHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case OBJ_ENTITY_TAG -> {
                createObjEntity(attributes);
                yield true;
            }
            case OBJ_ATTRIBUTE_TAG -> {
                createObjAttribute(attributes);
                yield true;
            }
            case OBJ_ATTRIBUTE_OVERRIDE_TAG -> {
                processStartAttributeOverride(attributes);
                yield true;
            }
            case QUALIFIER_TAG -> true;
            case POST_ADD_TAG, PRE_PERSIST_TAG, POST_PERSIST_TAG, PRE_UPDATE_TAG, POST_UPDATE_TAG, PRE_REMOVE_TAG,
                 POST_REMOVE_TAG, POST_LOAD_TAG -> {
                createCallback(localName, attributes);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        if (namespaceURI.equals(targetNamespace)
                && EmbeddableAttributeHandler.EMBEDDED_ATTRIBUTE_TAG.equals(localName)) {
            return new EmbeddableAttributeHandler(this, entity);
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (QUALIFIER_TAG.equals(localName)) {
            createQualifier(data);
        }
        return true;
    }

    private void createObjEntity(Attributes attributes) {
        entity = new ObjEntity(attributes.getValue("name"));
        entity.setClassName(attributes.getValue("className"));
        entity.setAbstract("true".equals(attributes.getValue("abstract")));
        entity.setReadOnly("true".equals(attributes.getValue("readOnly")));
        if ("optimistic".equals(attributes.getValue("lockType"))) {
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
        lastAttribute = new ObjAttribute(attributes.getValue("name"));
        lastAttribute.setType(attributes.getValue("type"));
        lastAttribute.setUsedForLocking("true".equals(attributes.getValue("lock")));
        lastAttribute.setLazy("true".equals(attributes.getValue("lazy")));
        lastAttribute.setDbAttributePath(attributes.getValue("dbAttributePath"));
        entity.addAttribute(lastAttribute);
    }

    private void processStartAttributeOverride(Attributes attributes) {
        entity.addAttributeOverride(attributes.getValue("name"),
                attributes.getValue("dbAttributePath"));
    }

    private CallbackDescriptor getCallbackDescriptor(String type) {
        if (entity == null) {
            return null;
        }

        return switch (type) {
            case POST_ADD_TAG -> entity.getCallbackMap().getPostAdd();
            case PRE_PERSIST_TAG -> entity.getCallbackMap().getPrePersist();
            case POST_PERSIST_TAG -> entity.getCallbackMap().getPostPersist();
            case PRE_UPDATE_TAG -> entity.getCallbackMap().getPreUpdate();
            case POST_UPDATE_TAG -> entity.getCallbackMap().getPostUpdate();
            case PRE_REMOVE_TAG -> entity.getCallbackMap().getPreRemove();
            case POST_REMOVE_TAG -> entity.getCallbackMap().getPostRemove();
            case POST_LOAD_TAG -> entity.getCallbackMap().getPostLoad();
            default -> null;
        };
    }

    private void createCallback(String type, Attributes attributes) {
        String methodName = attributes.getValue("methodName");
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
                throw new ConfigurationException("Invalid qualifier of ObjEntity '%s': %s", ex, entity.getName(),
                        qualifier);
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
