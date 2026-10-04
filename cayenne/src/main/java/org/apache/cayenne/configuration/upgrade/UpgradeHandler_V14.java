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
import org.apache.cayenne.ql.QLSelectPrinter;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Upgrades projects to version 14
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
        updateProjectSchema(upgradeUnit);
        updateDomainExtensionSchema(upgradeUnit, VALIDATION);
        removeEjbqlInspection(upgradeUnit);
        renameProjectInspection(upgradeUnit);
        removeVersionAttribute(upgradeUnit);
        convertNamesToCamelCase(upgradeUnit);
        renameProjectRoot(upgradeUnit);
    }

    @Override
    public void processDataMapDom(UpgradeContext upgradeUnit) {
        updateDataMapSchema(upgradeUnit);
        updateExtensionSchema(upgradeUnit, CGEN);
        updateExtensionSchema(upgradeUnit, DB_IMPORT);
        updateInfoSchema(upgradeUnit);
        convertSelectQueries(upgradeUnit);
        removeEjbqlQueries(upgradeUnit);
        convertQueryTags(upgradeUnit);
        convertQueryRoots(upgradeUnit);
        renameDbAttributeFlags(upgradeUnit);
        convertDeleteRules(upgradeUnit);
        removeVersionAttribute(upgradeUnit);
        convertNamesToCamelCase(upgradeUnit);
    }

    /**
     * Switches the project to the version 14 schema, that is called "project" instead of "domain".
     */
    private void updateProjectSchema(UpgradeContext upgradeUnit) {
        Element project = upgradeUnit.getDocument().getDocumentElement();
        project.setAttribute("xmlns", "http://cayenne.apache.org/schema/14/project");
        project.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        project.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/14/project "
                + "https://cayenne.apache.org/schema/14/project.xsd");
    }

    /**
     * Renames the root tag of the project from "domain" to "project".
     */
    private void renameProjectRoot(UpgradeContext upgradeUnit) {
        Element root = upgradeUnit.getDocument().getDocumentElement();
        upgradeUnit.getDocument().renameNode(root, root.getNamespaceURI(), "project");
    }

    /**
     * Switches the DataMap to the version 14 schema, that is called "dataMap" instead of "modelMap".
     */
    private void updateDataMapSchema(UpgradeContext upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        dataMap.setAttribute("xmlns", "http://cayenne.apache.org/schema/14/dataMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/14/dataMap "
                + "https://cayenne.apache.org/schema/14/dataMap.xsd");
    }

    /**
     * Drops the "is" prefix of the boolean attributes of "db-attribute", e.g. "isMandatory" becomes "mandatory".
     */
    private void renameDbAttributeFlags(UpgradeContext upgradeUnit) {
        String path = "/data-map/*[local-name()='db-entity']/*[local-name()='db-attribute']";
        for (Element attribute : elements(upgradeUnit, path)) {
            renameAttribute(attribute, "isMandatory", "mandatory");
            renameAttribute(attribute, "isPrimaryKey", "primaryKey");
            renameAttribute(attribute, "isGenerated", "generated");
        }
    }

    private void renameAttribute(Element element, String name, String newName) {
        if (element.hasAttribute(name)) {
            element.setAttribute(newName, element.getAttribute(name));
            element.removeAttribute(name);
        }
    }

    /**
     * Converts the delete rules of "obj-relationship" to lowercase, e.g. "Nullify" becomes "nullify".
     */
    private void convertDeleteRules(UpgradeContext upgradeUnit) {
        String path = "/data-map/*[local-name()='obj-relationship'][@deleteRule]";
        for (Element relationship : elements(upgradeUnit, path)) {
            relationship.setAttribute("deleteRule", relationship.getAttribute("deleteRule").toLowerCase());
        }
    }

    /**
     * Removes the project version attribute, as the version is already a part of the schema namespace.
     */
    private void removeVersionAttribute(UpgradeContext upgradeUnit) {
        upgradeUnit.getDocument().getDocumentElement().removeAttribute("project-version");
    }

    /**
     * Renames the hyphenated tags and attributes to camelCase, e.g. "db-entity" becomes "dbEntity". The extensions,
     * that have their own namespaces, are left alone.
     */
    private void convertNamesToCamelCase(UpgradeContext upgradeUnit) {
        Element root = upgradeUnit.getDocument().getDocumentElement();
        convertNamesToCamelCase(upgradeUnit.getDocument(), root, root.getNamespaceURI());
    }

    private void convertNamesToCamelCase(Document document, Element element, String namespace) {
        if (!Objects.equals(namespace, element.getNamespaceURI())) {
            return;
        }

        for (Element child : childElements(element)) {
            convertNamesToCamelCase(document, child, namespace);
        }

        NamedNodeMap attributes = element.getAttributes();
        List<Node> hyphenated = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.item(i).getNodeName();
            if (name.contains("-") && !name.contains(":")) {
                hyphenated.add(attributes.item(i));
            }
        }
        for (Node attribute : hyphenated) {
            document.renameNode(attribute, null, camelCase(attribute.getNodeName()));
        }

        if (element.getNodeName().contains("-")) {
            document.renameNode(element, element.getNamespaceURI(), camelCase(element.getNodeName()));
        }
    }

    private static String camelCase(String name) {
        StringBuilder camelCase = new StringBuilder(name.length());
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '-') {
                upper = true;
            } else {
                camelCase.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return camelCase.toString();
    }

    /**
     * Converts the hyphenated types of the query roots to camelCase, e.g. "obj-entity" becomes "objEntity".
     */
    private void convertQueryRoots(UpgradeContext upgradeUnit) {
        String path = "/data-map/*[local-name()='sql-query' or local-name()='procedure-query'][@root]";
        for (Element query : elements(upgradeUnit, path)) {
            query.setAttribute("root", camelCase(query.getAttribute("root")));
        }
    }

    private void removeEjbqlInspection(UpgradeContext upgradeUnit) {
        String path = "/domain/*[local-name()='validation']/*[local-name()='exclude']"
                + "[normalize-space(text())='EJBQL_QUERY_INVALID_SYNTAX']";
        for (Element exclude : elements(upgradeUnit, path)) {
            exclude.getParentNode().removeChild(exclude);
        }
    }

    /**
     * Renames the "DATA_CHANNEL_NO_NAME" inspection to "PROJECT_NO_NAME".
     */
    private void renameProjectInspection(UpgradeContext upgradeUnit) {
        String path = "/domain/*[local-name()='validation']/*[local-name()='exclude']"
                + "[normalize-space(text())='DATA_CHANNEL_NO_NAME']";
        for (Element exclude : elements(upgradeUnit, path)) {
            exclude.setTextContent("PROJECT_NO_NAME");
        }
    }

    private void removeEjbqlQueries(UpgradeContext upgradeUnit) {
        String path = "/data-map/*[local-name()='query'][@type='EJBQLQuery']";
        for (Element query : elements(upgradeUnit, path)) {
            query.getParentNode().removeChild(query);
            upgradeUnit.recordChange("""
                    Query '%s' was removed from the DataMap, as EJBQL queries are no longer supported. \
                    Replace it with an ObjectSelect or ColumnSelect parsed from a String"""
                    .formatted(query.getAttribute("name")), true);
        }
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

            Element selectElement = upgradeUnit.getDocument().createElementNS(query.getNamespaceURI(), "ql");
            selectElement.appendChild(upgradeUnit.getDocument().createCDATASection(select));
            query.appendChild(selectElement);
        }
    }

    /**
     * Replaces each generic "query" element with an element specific to the query type, and its generic "property"
     * elements with the attributes specific to each property, and a "cache-group" element. The properties that do not
     * apply to the query type are dropped.
     */
    private void convertQueryTags(UpgradeContext upgradeUnit) {
        for (Element query : elements(upgradeUnit, "/data-map/*[local-name()='query']")) {
            String name = query.getAttribute("name");
            String queryTag = switch (query.getAttribute("type")) {
                case "SelectQuery" -> "object-query";
                case "SQLTemplate" -> "sql-query";
                case "ProcedureQuery" -> "procedure-query";
                default -> null;
            };

            if (queryTag == null) {
                query.getParentNode().removeChild(query);
                upgradeUnit.recordChange("Query '%s' was removed from the DataMap, as its type is not supported"
                        .formatted(name), true);
                continue;
            }

            query.removeAttribute("type");
            Element typedQuery = (Element) upgradeUnit.getDocument()
                    .renameNode(query, query.getNamespaceURI(), queryTag);

            String cacheGroup = "";
            for (Element child : childElements(typedQuery)) {
                if (localName(child).equals("property")) {
                    String attribute = queryPropertyAttribute(queryTag, child.getAttribute("name"));
                    String value = child.getAttribute("value").trim();
                    recordSqlQueryLimitRemoval(upgradeUnit, name, queryTag, child.getAttribute("name"), value);
                    if (attribute != null && !value.isEmpty()) {
                        typedQuery.setAttribute(attribute,
                                attribute.equals("column-name-capitalization") ? value.toUpperCase() : value);
                    }
                    if (child.getAttribute("name").equals("cayenne.GenericSelectQuery.cacheGroups")) {
                        // the old property could hold a comma-separated list of groups, of which only the first
                        // non-empty one was ever used
                        for (String group : value.split(",")) {
                            if (!group.isBlank()) {
                                cacheGroup = group.trim();
                                break;
                            }
                        }
                    }
                    typedQuery.removeChild(child);
                }
            }

            // the cache group is an element that follows "ql" or "sql"
            if (!cacheGroup.isEmpty()) {
                Element next = null;
                for (Element child : childElements(typedQuery)) {
                    if (!localName(child).equals("ql") && !localName(child).equals("sql")) {
                        next = child;
                        break;
                    }
                }

                Element element = upgradeUnit.getDocument()
                        .createElementNS(typedQuery.getNamespaceURI(), "cache-group");
                element.appendChild(upgradeUnit.getDocument().createCDATASection(cacheGroup));
                typedQuery.insertBefore(element, next);
            }
        }
    }

    private void recordSqlQueryLimitRemoval(
            UpgradeContext upgradeUnit, String queryName, String queryTag, String property, String value) {

        if (!queryTag.equals("sql-query") || value.isEmpty() || value.equals("0")) {
            return;
        }

        String label = switch (property) {
            case "cayenne.GenericSelectQuery.fetchLimit" -> "fetch limit";
            case "cayenne.GenericSelectQuery.fetchOffset" -> "fetch offset";
            default -> null;
        };

        if (label != null) {
            upgradeUnit.recordChange("""
                    The %s of %s was removed from the SQL query '%s', as it is no longer supported in the DataMap. \
                    Make it a part of the query SQL""".formatted(label, value, queryName), true);
        }
    }

    /**
     * Returns the name of the attribute that replaces a property of a given type of query, or null if the property
     * does not apply to such query.
     */
    private String queryPropertyAttribute(String queryTag, String property) {
        return switch (property) {
            case "cayenne.GenericSelectQuery.cacheStrategy" -> "cache-strategy";
            case "cayenne.GenericSelectQuery.fetchingDataRows" -> "data-rows";
            case "cayenne.GenericSelectQuery.pageSize" -> "page-size";
            case "cayenne.GenericSelectQuery.statementFetchSize" -> "statement-fetch-size";

            // the limit and offset of an object query are a part of the query String, and those of a SQL query must
            // be a part of SQL
            case "cayenne.GenericSelectQuery.fetchLimit" -> queryTag.equals("procedure-query") ? "fetch-limit" : null;
            case "cayenne.GenericSelectQuery.fetchOffset" -> queryTag.equals("procedure-query") ? "fetch-offset" : null;

            case "cayenne.SQLTemplate.columnNameCapitalization" ->
                    queryTag.equals("sql-query") ? "column-name-capitalization" : null;
            case "cayenne.ProcedureQuery.columnNameCapitalization" ->
                    queryTag.equals("procedure-query") ? "column-name-capitalization" : null;
            default -> null;
        };
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
