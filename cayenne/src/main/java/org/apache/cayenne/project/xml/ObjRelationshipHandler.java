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
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class ObjRelationshipHandler extends NamespaceAwareNestedTagHandler {

    public static final String OBJ_RELATIONSHIP_TAG = "objRelationship";

    private DataMap map;

    private ObjRelationship objRelationship;

    public ObjRelationshipHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case OBJ_RELATIONSHIP_TAG:
                addObjRelationship(attributes);
                return true;
        }

        return false;
    }

    private void addObjRelationship(Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (null == name) {
            throw new SAXException("ObjRelationshipHandler::addObjRelationship() - unable to parse target.");
        }

        String sourceName = attributes.getValue("source");
        if (sourceName == null) {
            throw new SAXException("ObjRelationshipHandler::addObjRelationship() - unable to parse source.");
        }

        ObjEntity source = map.getObjEntity(sourceName);
        if (source == null) {
            throw new SAXException("ObjRelationshipHandler::addObjRelationship() - unable to find source " + sourceName);
        }

        objRelationship = new ObjRelationship(name);
        objRelationship.setSourceEntity(source);
        objRelationship.setTargetEntityName(attributes.getValue("target"));
        String deleteRule = attributes.getValue("deleteRule");
        if (deleteRule != null) {
            objRelationship.setDeleteRule(DeleteRule.valueOf(deleteRule.toUpperCase()));
        }
        objRelationship.setUsedForLocking(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("lock")));
        objRelationship.setDeferredDbRelationshipPath((attributes.getValue("dbRelationshipPath")));
        objRelationship.setCollectionType(attributes.getValue("collectionType"));
        objRelationship.setMapKey(attributes.getValue("mapKey"));
        source.addRelationship(objRelationship);
    }

    public ObjRelationship getObjRelationship() {
        return objRelationship;
    }
}
