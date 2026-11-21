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

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        // the "validation" extension moves to the project namespace
        List<Element> validation = elements(document, "/project/validation");
        assertEquals(1, validation.size());
        assertFalse(validation.get(0).hasAttribute("xmlns"));
        String xml = serialize(document);
        assertTrue(xml.contains("<validation>"), xml);
        assertFalse(xml.contains("/validation\""), xml);
        assertFalse(xml.contains("xmlns=\"\""), xml);

        // the dependent PK inspection is renamed, the rest is kept
        List<Element> excludes = elements(validation.get(0), "exclude");
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

        // the "cgen" extension moves to the DataMap namespace
        List<Element> cgens = elements(document, "/dataMap/cgen");
        assertEquals(1, cgens.size());
        assertFalse(cgens.get(0).hasAttribute("xmlns"));
        assertEquals(List.of("destDir"), childNames(cgens.get(0)));
        assertEquals("src/main/java", elements(cgens.get(0), "destDir").get(0).getTextContent());

        // the "dbimport" extension moves to the DataMap namespace, and its legacy "config" tag is dropped
        List<Element> dbImports = elements(document, "/dataMap/dbImport");
        assertEquals(1, dbImports.size());
        assertFalse(dbImports.get(0).hasAttribute("xmlns"));
        assertEquals(List.of("schema"), childNames(dbImports.get(0)));
        assertEquals("SCHEMA_01", elements(dbImports.get(0), "schema/name").get(0).getTextContent());

        // a "config" tag of another namespace is kept as is
        List<Element> configs = elements(document, "/dataMap/config");
        assertEquals(1, configs.size());
        assertEquals("http://example.org/config", configs.get(0).getAttribute("xmlns"));

        // "info" comments become the first child, whatever the namespace prefix, and the last one wins
        assertEquals("comment", childNames(root).get(0));
        assertEquals("a map & <more>", comment(root));
        Element artistTable = elements(document, "/dataMap/dbEntity[@name='ARTIST']").get(0);
        assertEquals(List.of("comment", "dbAttribute", "dbAttribute"), childNames(artistTable));
        assertEquals("artists", comment(artistTable));
        Element artistName = elements(artistTable, "dbAttribute[@name='ARTIST_NAME']").get(0);
        assertEquals(List.of("comment"), childNames(artistName));
        assertEquals("a name", comment(artistName));
        assertEquals("second", comment(elements(document, "/dataMap/dbEntity[@name='GALLERY']").get(0)));
        assertEquals("paintings", comment(elements(document, "/dataMap/dbEntity[@name='PAINTING']").get(0)));
        Element byEntity = elements(document, "/dataMap/sqlQuery[@name='byEntity']").get(0);
        assertEquals(List.of("comment", "sql"), childNames(byEntity));
        assertEquals("by entity", comment(byEntity));

        // an empty comment and the other "info" properties are dropped
        assertEquals(List.of("dbAttribute"),
                childNames(elements(document, "/dataMap/dbEntity[@name='PAINTING_INFO']").get(0)));
        assertFalse(root.hasAttribute("xmlns:info"));
        String xml = serialize(document);
        assertFalse(xml.contains("/info\""), xml);

        // a "property" of the DataMap namespace is kept
        Element artistExhibit = elements(document, "/dataMap/dbEntity[@name='ARTIST_EXHIBIT']").get(0);
        assertEquals(List.of("dbAttribute", "dbAttribute", "property"), childNames(artistExhibit));
        assertEquals(Map.of("name", "comment", "value", "not an info property"),
                attributes(elements(artistExhibit, "property").get(0)));
        assertEquals(1, elements(document, "//*[local-name()='property']").size());

        // the added and moved elements are in the DataMap namespace
        assertTrue(xml.contains("<comment>artists</comment>"), xml);
        assertTrue(xml.contains("<cgen>"), xml);
        assertFalse(xml.contains("/cgen\""), xml);
        assertTrue(xml.contains("<dbImport>"), xml);
        assertFalse(xml.contains("/dbimport\""), xml);
        assertFalse(xml.contains("xmlns=\"\""), xml);

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

        // the "dbEntity" and "procedure" roots of SQL queries are dropped, an "objEntity" root is kept
        assertEquals(Map.of("name", "byEntity", "root", "objEntity", "rootName", "Artist"), attributes(byEntity));
        assertEquals(Map.of("name", "byTable"), attributes(elements(document, "/dataMap/sqlQuery[@name='byTable']").get(0)));
        assertEquals(Map.of("name", "byProcedure"),
                attributes(elements(document, "/dataMap/sqlQuery[@name='byProcedure']").get(0)));

        // the mapped callbacks are removed, keeping the rest of the entity
        assertTrue(elements(document, "/dataMap/objEntity/*[@methodName]").isEmpty());
        assertEquals(List.of("objAttribute"), childNames(elements(document, "/dataMap/objEntity[@name='Artist']").get(0)));

        // only the removed callbacks are a lossy change
        List<UpgradeChange> changes = unit.getChanges();
        assertEquals(4, changes.size(), changes.toString());
        assertTrue(changes.get(0).message().contains("legacy 'config' tag"), changes.toString());
        assertTrue(changes.get(1).message().contains("'label' of objEntity 'Artist'"), changes.toString());
        assertTrue(changes.get(2).message().contains("'artist'"), changes.toString());
        assertTrue(changes.get(3).message().contains("'Artist'"), changes.toString());
        assertEquals(List.of("postAdd: onPostAdd", "prePersist: onPrePersist", "postLoad: onPostLoad"),
                changes.get(3).details());
        assertEquals(List.of(changes.get(3)), changes.stream().filter(UpgradeChange::affectsRuntime).toList());
    }

    private String comment(Element parent) {
        List<Element> comments = elements(parent, "comment");
        assertEquals(1, comments.size());
        return comments.get(0).getTextContent();
    }

    private String serialize(Document document) throws Exception {
        StringWriter out = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
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
