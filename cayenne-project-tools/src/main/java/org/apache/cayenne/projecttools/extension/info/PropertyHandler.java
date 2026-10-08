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

package org.apache.cayenne.projecttools.extension.info;

import org.apache.cayenne.project.ProjectNode;
import org.apache.cayenne.project.xml.DataMapHandler;
import org.apache.cayenne.project.xml.DbEntityHandler;
import org.apache.cayenne.project.xml.DbRelationshipHandler;
import org.apache.cayenne.project.xml.EmbeddableHandler;
import org.apache.cayenne.project.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.project.xml.ObjEntityHandler;
import org.apache.cayenne.project.xml.ObjRelationshipHandler;
import org.apache.cayenne.project.xml.ProcedureHandler;
import org.apache.cayenne.project.xml.ProjectMetaData;
import org.apache.cayenne.project.xml.QueryDescriptorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * @since 4.1
 */
class PropertyHandler extends NamespaceAwareNestedTagHandler {

    static final String PROPERTY_TAG = "property";

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyHandler.class);

    private final ProjectMetaData metaData;

    PropertyHandler(NamespaceAwareNestedTagHandler parentHandler, ProjectMetaData metaData) {
        super(parentHandler);
        setTargetNamespace(InfoExtension.NAMESPACE);
        this.metaData = metaData;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case PROPERTY_TAG:
                ProjectNode parentObject = getParentObject();
                String name = attributes.getValue("name");
                if(parentObject != null) {
                    ObjectInfo info = metaData.get(parentObject, ObjectInfo.class);
                    if(info == null) {
                        info = new ObjectInfo();
                        metaData.add(parentObject, info);
                    }
                    String oldValue = info.put(name, attributes.getValue("value"));
                    if(oldValue != null) {
                        LOGGER.warn("Duplicated property {} for object {}", name, parentObject);
                    }
                }
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    private ProjectNode getParentObject() {
        return switch (parentHandler) {
            case DataMapHandler handler -> handler.getDataMap();
            case DbEntityHandler handler -> handler.getEntity();
            case ObjEntityHandler handler -> handler.getEntity();
            case EmbeddableHandler handler -> handler.getEmbeddable();
            case QueryDescriptorHandler handler -> handler.getQueryDescriptor();
            case ProcedureHandler handler -> handler.getProcedure();
            case DbRelationshipHandler handler -> handler.getDbRelationship();
            case ObjRelationshipHandler handler -> handler.getObjRelationship();
            // a property of an attribute is nested in the attribute tag, which is read by the entity handler
            case NamespaceAwareNestedTagHandler handler -> getLastAttribute(handler.getParentHandler());
            default -> null;
        };
    }

    private ProjectNode getLastAttribute(ContentHandler entityHandler) {
        return switch (entityHandler) {
            case DbEntityHandler handler -> handler.getLastAttribute();
            case ObjEntityHandler handler -> handler.getLastAttribute();
            default -> null;
        };
    }
}
