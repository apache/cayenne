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
package org.apache.cayenne.configuration.upgrade;

import org.apache.cayenne.configuration.Project;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class UpgradeHandler_V14Test extends BaseUpgradeHandlerTest {

    @Override
    UpgradeHandler newHandler() {
        return new UpgradeHandler_V14();
    }

    @Test
    public void projectDomUpgrade() throws Exception {
        Document document = processProjectDom("v14/cayenne-project1.xml");

        Element root = document.getDocumentElement();
        assertFalse(root.hasAttribute("project-version"));
        assertEquals("project", root.getNodeName());
        assertEquals("http://cayenne.apache.org/schema/14/project", root.getAttribute("xmlns"));
        assertNoHyphenatedNames(document);

        // the properties become attributes, unless they have the default value or are unknown
        assertTrue(elements(document, "/project/*[local-name()='property']").isEmpty());
        assertEquals("false", root.getAttribute("validateOnCommit"));
        assertFalse(root.hasAttribute("sharedCache"));

        List<Element> validation = elements(document, "/project/*[local-name()='validation']");
        assertEquals(1, validation.size());
        assertEquals("http://cayenne.apache.org/schema/14/validation", validation.get(0).getAttribute("xmlns"));

        // the EJBQL inspection no longer exists, the data channel inspection is renamed, the rest is kept
        List<Element> excludes = elements(validation.get(0), "*[local-name()='exclude']");
        assertEquals(List.of("PROJECT_NO_NAME", "SQL_TEMPLATE_NO_ROOT"),
                excludes.stream().map(Element::getTextContent).toList());
    }

    @Test
    public void dataMapDomUpgrade() throws Exception {
        String resource = "v14/map1.map.xml";
        UpgradeContext unit = new UpgradeContext(new URLResource(getClass().getResource(resource)),
                documentFromResource(resource));
        handler.processDataMapDom(unit);
        Document document = unit.getDocument();

        Element root = document.getDocumentElement();
        assertFalse(root.hasAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/14/dataMap", root.getAttribute("xmlns"));

        // all tags and attributes are renamed to camelCase
        assertEquals("dataMap", root.getNodeName());
        assertNoHyphenatedNames(document);
        assertEquals(2, elements(document, "/dataMap/dbEntity/dbAttribute").size());
        assertEquals(1, elements(document, "/dataMap/objEntity/objAttribute[@dbAttributePath='ARTIST_NAME']").size());

        // the DataMap properties become attributes, unknown properties are dropped, the "info" properties are kept
        assertTrue(elements(document, "/dataMap/*[name()='property']").isEmpty());
        assertEquals("org.apache.cayenne.x", root.getAttribute("defaultPackage"));
        assertEquals("optimistic", root.getAttribute("defaultLockType"));
        assertEquals("true", root.getAttribute("quoteSqlIdentifiers"));
        assertFalse(root.hasAttribute("clientSupported"));
        assertFalse(root.hasAttribute("defaultSchema"));
        assertEquals(1, elements(document, "/dataMap/*[name()='info:property'][@value='a map']").size());

        // the boolean attributes of "dbAttribute" lose the "is" prefix
        assertEquals(1, elements(document, "/dataMap/dbEntity/dbAttribute[@primaryKey='true'][@mandatory='true']").size());
        assertTrue(elements(document, "//*[@isPrimaryKey or @isMandatory or @isGenerated]").isEmpty());

        // the delete rules are in lowercase
        assertEquals(1, elements(document, "/dataMap/objRelationship[@deleteRule='nullify']").size());

        List<Element> cgen = elements(document, "/dataMap/*[local-name()='cgen']");
        assertEquals(1, cgen.size());
        assertEquals("http://cayenne.apache.org/schema/14/cgen", cgen.get(0).getAttribute("xmlns"));

        List<Element> infoProperties = elements(document, "//*[local-name()='property']").stream()
                .filter(p -> p.hasAttribute("xmlns:info"))
                .toList();
        assertEquals(2, infoProperties.size());
        infoProperties.forEach(p ->
                assertEquals("http://cayenne.apache.org/schema/14/info", p.getAttribute("xmlns:info")));

        // the rootless query and the EJBQL queries are removed with a notification each, the rest are converted
        // to the elements specific to the query type
        assertTrue(elements(document, "/dataMap/*[local-name()='query']").isEmpty());
        List<Element> queries = elements(document, "/dataMap/*[@name][contains(local-name(), 'Query')]");
        assertEquals(List.of("AllClauses", "ClassRoot", "RootOnly", "Template", "Procedure"),
                queries.stream().map(q -> q.getAttribute("name")).toList());
        assertEquals(List.of("objectQuery", "objectQuery", "objectQuery", "sqlQuery", "procedureQuery"),
                queries.stream().map(Element::getNodeName).toList());
        queries.forEach(q -> assertFalse(q.hasAttribute("type"), q.getAttribute("name")));
        queries.forEach(q -> assertEquals(root.getNamespaceURI(), q.getNamespaceURI(), q.getAttribute("name")));
        List<String> notifications = unit.getChangeNotifications();
        assertEquals(5, notifications.size(), notifications.toString());
        assertTrue(notifications.get(0).contains("'NoRoot'"), notifications.toString());
        assertTrue(notifications.get(1).contains("'Ejbql1'"), notifications.toString());
        assertTrue(notifications.get(2).contains("'Ejbql2'"), notifications.toString());
        assertTrue(notifications.get(3).contains("fetch limit of 10") && notifications.get(3).contains("'Template'"),
                notifications.toString());
        assertTrue(notifications.get(4).contains("'UnknownType'"), notifications.toString());
        assertEquals(notifications, unit.getChangesAffectingRuntime());

        Element allClauses = queries.get(0);
        assertFalse(allClauses.hasAttribute("root"));
        assertFalse(allClauses.hasAttribute("rootName"));
        // the properties become attributes and a cache group element, limit, offset and distinct go to the query
        // String
        assertEquals(List.of("ql", "cacheGroup"), childNames(allClauses));
        assertEquals(Map.of("name", "AllClauses", "cacheStrategy", "SHARED_CACHE", "pageSize", "5"),
                attributes(allClauses));
        assertEquals("g1", cacheGroup(allClauses));
        assertEquals("select distinct self from Artist where artistName like $name "
                        + "order by artistName desc insensitive, dateOfBirth limit 10 offset 20 "
                        + "prefetch paintings joint, paintings.gallery disjoint, paintings.exhibits disjointById, "
                        + "groups",
                select(allClauses));

        Element classRoot = queries.get(1);
        assertFalse(classRoot.hasAttribute("root"));
        assertEquals(List.of("ql"), childNames(classRoot));
        assertEquals("from Artist where artistName = \"a\"", select(classRoot));

        Element rootOnly = queries.get(2);
        assertEquals(List.of("ql"), childNames(rootOnly));
        assertEquals("from Artist", select(rootOnly));

        // an unknown property and the one of another query type are dropped. So is the limit, with a notification
        Element template = queries.get(3);
        assertEquals("dataMap", template.getAttribute("root"));
        assertEquals(List.of("sql", "sql", "cacheGroup", "prefetch"), childNames(template));
        assertEquals("g2 & <g3>", cacheGroup(template));
        assertEquals(Map.of("name", "Template", "root", "dataMap", "rootName", "map1", "dataRows", "true",
                "columnNameCapitalization", "LOWER"), attributes(template));

        Element procedure = queries.get(4);
        assertTrue(childNames(procedure).isEmpty());
        assertEquals(Map.of("name", "Procedure", "root", "procedure", "rootName", "p1", "resultEntity", "Artist",
                "cacheStrategy", "LOCAL_CACHE", "fetchLimit", "3", "fetchOffset", "4",
                "columnNameCapitalization", "UPPER"), attributes(procedure));

        assertTrue(queries.stream().allMatch(q -> elements(q, "*[local-name()='property']").isEmpty()));
    }

    @Test
    public void modelUpgrade() {
        Project project = mock(Project.class);
        handler.processModel(project);
        verifyNoInteractions(project);
    }

    private void assertNoHyphenatedNames(Document document) {
        assertTrue(elements(document, "//*[contains(name(), '-') or @*[contains(name(), '-')]]").isEmpty());
    }

    private String select(Element query) {
        List<Element> select = elements(query, "*[local-name()='ql']");
        assertEquals(1, select.size());
        assertEquals(query.getNamespaceURI(), select.get(0).getNamespaceURI());

        // the query String must be written as CDATA
        Node cdata = select.get(0).getFirstChild();
        assertEquals(Node.CDATA_SECTION_NODE, cdata.getNodeType());
        return cdata.getNodeValue();
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

    private String cacheGroup(Element query) {
        List<Element> cacheGroup = elements(query, "*[local-name()='cacheGroup']");
        assertEquals(1, cacheGroup.size());
        assertEquals(query.getNamespaceURI(), cacheGroup.get(0).getNamespaceURI());

        // the cache group must be written as CDATA
        Node cdata = cacheGroup.get(0).getFirstChild();
        assertEquals(Node.CDATA_SECTION_NODE, cdata.getNodeType());
        return cdata.getNodeValue();
    }

    private Map<String, String> attributes(Element element) {
        Map<String, String> attributes = new HashMap<>();
        NamedNodeMap nodes = element.getAttributes();
        for (int i = 0; i < nodes.getLength(); i++) {
            attributes.put(nodes.item(i).getNodeName(), nodes.item(i).getNodeValue());
        }
        return attributes;
    }

    private List<Element> elements(Node context, String path) throws RuntimeException {
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
