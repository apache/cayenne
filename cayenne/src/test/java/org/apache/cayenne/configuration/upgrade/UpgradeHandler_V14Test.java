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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

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
        assertEquals("14", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/14/domain", root.getAttribute("xmlns"));

        List<Element> validation = elements(document, "/domain/*[local-name()='validation']");
        assertEquals(1, validation.size());
        assertEquals("http://cayenne.apache.org/schema/14/validation", validation.get(0).getAttribute("xmlns"));

        // the EJBQL inspection no longer exists, the other exclusions are kept
        List<Element> excludes = elements(validation.get(0), "*[local-name()='exclude']");
        assertEquals(List.of("DATA_CHANNEL_NO_NAME", "SQL_TEMPLATE_NO_ROOT"),
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
        assertEquals("14", root.getAttribute("project-version"));
        assertEquals("http://cayenne.apache.org/schema/14/modelMap", root.getAttribute("xmlns"));

        List<Element> cgen = elements(document, "/data-map/*[local-name()='cgen']");
        assertEquals(1, cgen.size());
        assertEquals("http://cayenne.apache.org/schema/14/cgen", cgen.get(0).getAttribute("xmlns"));

        List<Element> infoProperties = elements(document, "//*[local-name()='property']").stream()
                .filter(p -> p.hasAttribute("xmlns:info"))
                .toList();
        assertEquals(1, infoProperties.size());
        assertEquals("http://cayenne.apache.org/schema/14/info", infoProperties.get(0).getAttribute("xmlns:info"));

        // the rootless query and the EJBQL queries are removed with a notification each, the rest are converted
        List<Element> queries = elements(document, "/data-map/*[local-name()='query']");
        assertEquals(List.of("AllClauses", "ClassRoot", "RootOnly", "Template"),
                queries.stream().map(q -> q.getAttribute("name")).toList());
        List<String> notifications = unit.getChangeNotifications();
        assertEquals(3, notifications.size(), notifications.toString());
        assertTrue(notifications.get(0).contains("'NoRoot'"), notifications.toString());
        assertTrue(notifications.get(1).contains("'Ejbql1'"), notifications.toString());
        assertTrue(notifications.get(2).contains("'Ejbql2'"), notifications.toString());
        assertEquals(notifications, unit.getChangesAffectingRuntime());

        Element allClauses = queries.get(0);
        assertFalse(allClauses.hasAttribute("root"));
        assertFalse(allClauses.hasAttribute("root-name"));
        assertEquals(List.of("property", "property", "property", "select"), childNames(allClauses));
        assertEquals(List.of("cayenne.GenericSelectQuery.cacheStrategy", "cayenne.GenericSelectQuery.cacheGroups",
                "cayenne.GenericSelectQuery.pageSize"), propertyNames(allClauses));
        assertEquals("select distinct self from Artist where artistName like $name "
                        + "order by artistName desc insensitive, dateOfBirth limit 10 offset 20 "
                        + "prefetch paintings joint, paintings.gallery disjoint, paintings.exhibits disjointById, "
                        + "groups",
                select(allClauses));

        Element classRoot = queries.get(1);
        assertFalse(classRoot.hasAttribute("root"));
        assertEquals(List.of("select"), childNames(classRoot));
        assertEquals("from Artist where artistName = \"a\"", select(classRoot));

        Element rootOnly = queries.get(2);
        assertEquals(List.of("select"), childNames(rootOnly));
        assertEquals("from Artist", select(rootOnly));

        // other query types are untouched
        Element template = queries.get(3);
        assertEquals("data-map", template.getAttribute("root"));
        assertEquals(List.of("property", "sql", "prefetch"), childNames(template));
    }

    @Test
    public void modelUpgrade() {
        DataChannelDescriptor descriptor = mock(DataChannelDescriptor.class);
        handler.processModel(descriptor);
        verifyNoInteractions(descriptor);
    }

    private String select(Element query) {
        List<Element> select = elements(query, "*[local-name()='select']");
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

    private List<String> propertyNames(Element query) {
        return elements(query, "*[local-name()='property']").stream().map(p -> p.getAttribute("name")).toList();
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
