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

import org.apache.cayenne.exp.path.CayennePath;
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

    public static final String OBJ_RELATIONSHIP_TAG = "obj-relationship";

    @Deprecated
    public static final String DB_RELATIONSHIP_REF_TAG = "db-relationship-ref";

    private DataMap map;

    private ObjRelationship objRelationship;

    public ObjRelationshipHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case OBJ_RELATIONSHIP_TAG:
                addObjRelationship(attributes);
                return true;

            case DB_RELATIONSHIP_REF_TAG:
                addDbRelationshipRef(attributes);
                return true;
        }

        return false;
    }

    /**
     * <db-relationship-ref> tag deprecated
     */
    @Deprecated
    private void addDbRelationshipRef(Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (name == null) {
            throw new SAXException("ObjRelationshipHandler::addDbRelationshipRef() - null DbRelationship name for "
                    + objRelationship.getName());
        }

        CayennePath path = objRelationship.getDbRelationshipPath();
        objRelationship.setDbRelationshipPath(path.dot(name));
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
        objRelationship.setDeleteRule(DeleteRule.deleteRuleForName(attributes.getValue("deleteRule")));
        objRelationship.setUsedForLocking(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("lock")));
        objRelationship.setDeferredDbRelationshipPath((attributes.getValue("db-relationship-path")));
        objRelationship.setCollectionType(attributes.getValue("collection-type"));
        objRelationship.setMapKey(attributes.getValue("map-key"));
        source.addRelationship(objRelationship);
    }

    public ObjRelationship getObjRelationship() {
        return objRelationship;
    }
}
