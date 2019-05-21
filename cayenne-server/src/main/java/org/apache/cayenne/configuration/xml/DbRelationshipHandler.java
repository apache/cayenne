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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class DbRelationshipHandler extends NamespaceAwareNestedTagHandler {

    private static final String DB_RELATIONSHIP_TAG = "db-relationship";
    public static final String DB_ATTRIBUTE_PAIR_TAG = "db-attribute-pair";

    private DataMap map;

    private DbRelationship dbRelationship;

    public DbRelationshipHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {

        switch (localName) {
            case DB_RELATIONSHIP_TAG:
                createRelationship(attributes);
                return true;

            case DB_ATTRIBUTE_PAIR_TAG:
                createDbAttributePair(attributes);
                return true;
        }

        return false;
    }

    private void createRelationship(Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (name == null) {
            throw new SAXException("DbRelationshipHandler::createRelationship() - missing \"name\" attribute.");
        }

        String sourceName = attributes.getValue("source");
        if (sourceName == null) {
            throw new SAXException("DbRelationshipHandler::createRelationship() - null source entity");
        }

        DbEntity source = map.getDbEntity(sourceName);
        if (source == null) {
            return;
        }

        dbRelationship = new DbRelationship(name);
        dbRelationship.setSourceEntity(source);
        dbRelationship.setTargetEntityName(attributes.getValue("target"));
        dbRelationship.setToMany(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("toMany")));
        dbRelationship.setToDependentPK(DataMapHandler.TRUE.equalsIgnoreCase(attributes.getValue("toDependentPK")));

        source.addRelationship(dbRelationship);
    }

    private void createDbAttributePair(Attributes attributes) {
        DbJoin join = new DbJoin(dbRelationship);
        join.setSourceName(attributes.getValue("source"));
        join.setTargetName(attributes.getValue("target"));
        dbRelationship.addJoin(join);
    }

    public DbRelationship getDbRelationship() {
        return dbRelationship;
    }
}
