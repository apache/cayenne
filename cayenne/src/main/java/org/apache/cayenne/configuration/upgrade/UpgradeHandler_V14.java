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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.QLSelectPrinter;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrades projects to version 14.
 *
 * @since 5.0
 */
public final class UpgradeHandler_V14 implements UpgradeHandler {

    @Override
    public String getVersion() {
        return "14";
    }

    @Override
    public void processProjectDom(UpgradeContext upgradeUnit) {
        updateDomainSchemaAndVersion(upgradeUnit);
        updateDomainExtensionSchema(upgradeUnit, VALIDATION);
    }

    @Override
    public void processDataMapDom(UpgradeContext upgradeUnit) {
        updateDataMapSchemaAndVersion(upgradeUnit);
        updateExtensionSchema(upgradeUnit, CGEN);
        updateExtensionSchema(upgradeUnit, DB_IMPORT);
        updateInfoSchema(upgradeUnit);
        convertSelectQueries(upgradeUnit);
    }

    private void convertSelectQueries(UpgradeContext upgradeUnit) {
        String path = "/data-map/*[local-name()='query'][@type='SelectQuery']";
        for (Element query : elements(upgradeUnit, path)) {
            String name = query.getAttribute("name");
            String rootEntity = rootEntity(upgradeUnit, query);

            if (rootEntity == null) {
                query.getParentNode().removeChild(query);
                upgradeUnit.recordChange("""
                        Query '%s' was removed from the DataMap, as it has no root ObjEntity. Starting with project \
                        version 14, a select query is stored as a String that starts with its root entity"""
                        .formatted(name), true);
                continue;
            }

            String select = QLSelectPrinter.print(select(name, rootEntity, query));

            query.removeAttribute("root");
            query.removeAttribute("root-name");
            for (Element child : childElements(query)) {
                if (!localName(child).equals("property") || isQueryStringProperty(child)) {
                    query.removeChild(child);
                }
            }

            Element selectElement = upgradeUnit.getDocument().createElementNS(query.getNamespaceURI(), "select");
            selectElement.appendChild(upgradeUnit.getDocument().createCDATASection(select));
            query.appendChild(selectElement);
        }
    }

    /**
     * Returns the name of the query root ObjEntity, or null if the query root is not an ObjEntity.
     */
    private String rootEntity(UpgradeContext upgradeUnit, Element query) {
        String rootType = query.getAttribute("root");
        String rootName = query.getAttribute("root-name");
        if (rootName.isEmpty()) {
            return null;
        }

        return switch (rootType) {
            case "obj-entity" -> rootName;

            // a root defined as a Java class is resolved to an ObjEntity in the same DataMap
            case "java-class" -> {
                String entityPath = "/data-map/*[local-name()='obj-entity'][@className='" + rootName + "']";
                List<Element> entities = elements(upgradeUnit, entityPath);
                yield entities.isEmpty() ? null : entities.get(0).getAttribute("name");
            }
            default -> null;
        };
    }

    private ObjectSelect<?> select(String name, String rootEntity, Element query) {
        ObjectSelect<?> select = ObjectSelect.query(Object.class).entityName(rootEntity);

        for (Element child : childElements(query)) {
            String text = child.getTextContent().trim();
            switch (localName(child)) {
                case "qualifier" -> {
                    if (!text.isEmpty()) {
                        select.where(parseExpression(name, text));
                    }
                }
                case "ordering" -> {
                    boolean descending = "true".equalsIgnoreCase(child.getAttribute("descending"));
                    boolean ignoreCase = "true".equalsIgnoreCase(child.getAttribute("ignore-case"));
                    SortOrder order = descending
                            ? (ignoreCase ? SortOrder.DESCENDING_INSENSITIVE : SortOrder.DESCENDING)
                            : (ignoreCase ? SortOrder.ASCENDING_INSENSITIVE : SortOrder.ASCENDING);
                    select.orderBy(new Ordering(parseExpression(name, text), order));
                }
                case "prefetch" -> {
                    if (!text.isEmpty()) {
                        select.prefetch(text, prefetchSemantics(child.getAttribute("type")));
                    }
                }
                case "property" -> {
                    String value = child.getAttribute("value");
                    switch (child.getAttribute("name")) {
                        case "cayenne.GenericSelectQuery.fetchLimit" -> select.limit(Integer.parseInt(value));
                        case "cayenne.GenericSelectQuery.fetchOffset" -> select.offset(Integer.parseInt(value));
                        case "cayenne.SelectQuery.distinct" -> {
                            if (Boolean.parseBoolean(value)) {
                                select.distinct();
                            }
                        }
                        default -> {
                            // not a part of the query String
                        }
                    }
                }
                default -> {
                    // not a part of a select query
                }
            }
        }

        return select;
    }

    private Expression parseExpression(String queryName, String expression) {
        try {
            return ExpressionFactory.exp(expression);
        } catch (Exception e) {
            throw new ConfigurationException("Query '%s' has an invalid expression: %s", e, queryName, expression);
        }
    }

    private boolean isQueryStringProperty(Element property) {
        return switch (property.getAttribute("name")) {
            case "cayenne.GenericSelectQuery.fetchLimit",
                 "cayenne.GenericSelectQuery.fetchOffset",
                 "cayenne.SelectQuery.distinct" -> true;
            default -> false;
        };
    }

    private int prefetchSemantics(String type) {
        return switch (type) {
            case "joint" -> PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS;
            case "disjoint" -> PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS;
            case "disjointById" -> PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS;
            default -> PrefetchTreeNode.UNDEFINED_SEMANTICS;
        };
    }

    /**
     * Returns the name of the element without a prefix, whether the document was parsed with or without namespaces.
     */
    private static String localName(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
    }

    private List<Element> childElements(Element parent) {
        NodeList children = parent.getChildNodes();
        List<Element> elements = new ArrayList<>(children.getLength());
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element element) {
                elements.add(element);
            }
        }
        return elements;
    }

    private List<Element> elements(UpgradeContext upgradeUnit, String path) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate(path, upgradeUnit.getDocument(), XPathConstants.NODESET);
        } catch (Exception e) {
            return List.of();
        }

        List<Element> elements = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        return elements;
    }
}
