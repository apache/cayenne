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
package org.apache.cayenne.project.upgrade;

import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpgradeHandler_V15Test extends BaseUpgradeHandlerTest {

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V15();
    }

    @Test
    public void projectDomUpgrade() throws Exception {
        Document document = upgradeProjectDOM("v15/cayenne-project1.xml");

        Element root = document.getDocumentElement();
        assertEquals("project", root.getNodeName());
        assertEquals("http://cayenne.apache.org/schema/15/project", root.getAttribute("xmlns"));
        assertEquals("http://cayenne.apache.org/schema/15/project https://cayenne.apache.org/schema/15/project.xsd",
                root.getAttribute("xsi:schemaLocation"));
        assertEquals("false", root.getAttribute("validateOnCommit"));

        List<Element> validation = elements(document, "/project/*[local-name()='validation']");
        assertEquals(1, validation.size());
        assertEquals("http://cayenne.apache.org/schema/15/validation", validation.get(0).getAttribute("xmlns"));

        // the dependent PK inspection is renamed, the rest is kept
        List<Element> excludes = elements(validation.get(0), "*[local-name()='exclude']");
        assertEquals(List.of("PROJECT_NO_NAME", "DB_RELATIONSHIP_FOREIGN_KEY_IN_GENERATED_PK", "SQL_TEMPLATE_NO_ROOT"),
                excludes.stream().map(Element::getTextContent).toList());
    }

    @Test
    public void dataMapDomUpgrade() throws Exception {
        UpgradeContext unit = new UpgradeContext(new URLResource(getClass().getResource("v15/map1.map.xml")),
                documentFromResource("v15/map1.map.xml"));
        handler.upgradeDataMapDOM(unit);
        Document document = unit.getDocument();

        Element root = document.getDocumentElement();
        assertEquals("dataMap", root.getNodeName());
        assertEquals("http://cayenne.apache.org/schema/15/dataMap", root.getAttribute("xmlns"));
        assertEquals("http://cayenne.apache.org/schema/15/dataMap https://cayenne.apache.org/schema/15/dataMap.xsd",
                root.getAttribute("xsi:schemaLocation"));
        assertEquals("org.apache.cayenne.x", root.getAttribute("defaultPackage"));

        // the extensions switch to the new schema version
        assertEquals("http://cayenne.apache.org/schema/15/cgen",
                elements(document, "/dataMap/cgen").get(0).getAttribute("xmlns"));
        assertEquals("http://cayenne.apache.org/schema/15/dbimport",
                elements(document, "/dataMap/dbImport").get(0).getAttribute("xmlns"));
        assertEquals("http://cayenne.apache.org/schema/15/info",
                elements(document, "/dataMap/dbEntity/*[name()='info:property']").get(0).getAttribute("xmlns:info"));

        // "toDependentPK" becomes "foreignKey" on the reverse side, every other to-one holds the FK, including one
        // joined to a non-PK column or to an unknown entity, and a to-many doesn't
        assertTrue(elements(document, "//*[@toDependentPK]").isEmpty());
        assertEquals(List.of("artist", "toArtist", "toGallery", "toOther", "painting"),
                elements(document, "/dataMap/dbRelationship[@foreignKey='true']").stream()
                        .map(r -> r.getAttribute("name")).toList());
        assertTrue(elements(document, "/dataMap/dbRelationship[@name='toPaintingInfo'][@foreignKey]").isEmpty());
        assertTrue(elements(document, "/dataMap/dbRelationship[@name='paintingArray'][@foreignKey]").isEmpty());
        assertTrue(elements(document, "/dataMap/dbRelationship[@name='artistExhibitArray'][@foreignKey]").isEmpty());

        // the missing reverse of a "toDependentPK" relationship is created to hold the flag, named after its target
        List<Element> artist = elements(document, "/dataMap/dbRelationship[@source='ARTIST_EXHIBIT']");
        assertEquals(1, artist.size());
        assertEquals(Map.of("name", "artist", "source", "ARTIST_EXHIBIT", "target", "ARTIST", "foreignKey", "true"),
                attributes(artist.get(0)));
        assertEquals(List.of("dbAttributePair"), childNames(artist.get(0)));
        assertEquals(Map.of("source", "ARTIST_ID", "target", "ARTIST_ID"),
                attributes(elements(artist.get(0), "*").get(0)));
        assertEquals(root.getNamespaceURI(), artist.get(0).getNamespaceURI());
        assertEquals("artistExhibitArray", ((Element) artist.get(0).getPreviousSibling()).getAttribute("name"));

        // the added reverse relationship is a lossless change
        List<String> notifications = unit.getChangeNotifications();
        assertEquals(1, notifications.size(), notifications.toString());
        assertTrue(notifications.get(0).contains("'artist'"), notifications.toString());
        assertTrue(unit.getChangesAffectingRuntime().isEmpty());
    }

    private List<String> childNames(Element parent) {
        List<String> names = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                names.add(child.getNodeName());
            }
        }
        return names;
    }

    private Map<String, String> attributes(Element element) {
        Map<String, String> attributes = new HashMap<>();
        NamedNodeMap nodes = element.getAttributes();
        for (int i = 0; i < nodes.getLength(); i++) {
            attributes.put(nodes.item(i).getNodeName(), nodes.item(i).getNodeValue());
        }
        return attributes;
    }

    private List<Element> elements(Node context, String path) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate(path, context, XPathConstants.NODESET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<Element> elements = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
