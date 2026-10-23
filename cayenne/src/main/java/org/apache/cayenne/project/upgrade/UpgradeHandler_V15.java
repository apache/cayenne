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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Upgrades projects to version 15
 *
 * @since 6.0
 */
public final class UpgradeHandler_V15 implements UpgradeHandler {

    @Override
    public String getVersion() {
        return "15";
    }

    @Override
    public void upgradeProjectDOM(UpgradeContext context) {
        updateProjectSchema(context);
        upgradeExtensionSchema(context, "/project", "validation");
        renameInspection(context);
    }

    @Override
    public void upgradeDataMapDOM(UpgradeContext context) {
        updateDataMapSchema(context);
        upgradeExtensionSchema(context, "/dataMap", "cgen");
        upgradeExtensionSchema(context, "/dataMap", "dbImport");
        upgradeInfoSchema(context);
        convertForeignKeys(context);
        dropSqlQueryDbRoots(context);
    }

    private void updateProjectSchema(UpgradeContext upgradeUnit) {
        Element project = upgradeUnit.getDocument().getDocumentElement();
        project.setAttribute("xmlns", "http://cayenne.apache.org/schema/15/project");
        project.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/15/project "
                + "https://cayenne.apache.org/schema/15/project.xsd");
    }

    private void updateDataMapSchema(UpgradeContext upgradeUnit) {
        Element dataMap = upgradeUnit.getDocument().getDocumentElement();
        dataMap.setAttribute("xmlns", "http://cayenne.apache.org/schema/15/dataMap");
        dataMap.setAttribute("xsi:schemaLocation", "http://cayenne.apache.org/schema/15/dataMap "
                + "https://cayenne.apache.org/schema/15/dataMap.xsd");
    }

    private void upgradeExtensionSchema(UpgradeContext upgradeUnit, String root, String extension) {
        for (Element element : elements(upgradeUnit, root + "/*[local-name()='" + extension + "']")) {
            element.setAttribute("xmlns", "http://cayenne.apache.org/schema/15/" + extension.toLowerCase());
        }
    }

    private void upgradeInfoSchema(UpgradeContext upgradeUnit) {
        for (Element element : elements(upgradeUnit, "//*[local-name()='property']")) {
            if (element.hasAttribute("xmlns:info")) {
                element.setAttribute("xmlns:info", "http://cayenne.apache.org/schema/15/info");
            }
        }
    }

    /**
     * Renames the "DB_RELATIONSHIP_GENERATED_WITH_DEPENDENT_PK" inspection to
     * "DB_RELATIONSHIP_FOREIGN_KEY_IN_GENERATED_PK".
     */
    private void renameInspection(UpgradeContext upgradeUnit) {
        String path = "/project/*[local-name()='validation']/*[local-name()='exclude']"
                + "[normalize-space(text())='DB_RELATIONSHIP_GENERATED_WITH_DEPENDENT_PK']";
        for (Element exclude : elements(upgradeUnit, path)) {
            exclude.setTextContent("DB_RELATIONSHIP_FOREIGN_KEY_IN_GENERATED_PK");
        }
    }

    /**
     * Removes the "dbEntity" and "procedure" roots of "sqlQuery", that only routed the query to the DataNode of its
     * DataMap. A SQL query root is now an ObjEntity or the DataMap, and a query without a root is run on the DataNode
     * of its DataMap.
     */
    private void dropSqlQueryDbRoots(UpgradeContext upgradeUnit) {
        String path = "/dataMap/*[local-name()='sqlQuery'][@root='dbEntity' or @root='procedure']";
        for (Element query : elements(upgradeUnit, path)) {
            query.removeAttribute("root");
            query.removeAttribute("rootName");
        }
    }

    /**
     * Replaces the "toDependentPK" flag of "dbRelationship", that marked the master side of a relationship joined
     * PK to PK, with the "foreignKey" flag on the side whose join columns hold the FK: the reverse of a
     * "toDependentPK" relationship, and otherwise every to-one. A to-one not marked "toDependentPK" held the FK
     * whatever columns it joined, including a FK to a unique non-PK column. The reverse of a "toDependentPK"
     * relationship is created when it is missing, as it is the only place to keep the flag.
     */
    private void convertForeignKeys(UpgradeContext upgradeUnit) {
        List<Element> relationships = elements(upgradeUnit, "/dataMap/*[local-name()='dbRelationship']");
        for (Element relationship : new ArrayList<>(relationships)) {
            if ("true".equals(relationship.getAttribute("toDependentPK"))) {
                relationship.removeAttribute("toDependentPK");
                Element reverse = reverseRelationship(relationship, relationships);
                if (reverse == null) {
                    reverse = createReverseRelationship(upgradeUnit, relationship, relationships);
                    relationships.add(reverse);
                }
                reverse.setAttribute("foreignKey", "true");
            } else if (!"true".equals(relationship.getAttribute("toMany"))) {
                relationship.setAttribute("foreignKey", "true");
            }
        }
    }

    private Element reverseRelationship(Element relationship, List<Element> relationships) {
        Set<String> reverseJoins = joinPairs(relationship, true);
        for (Element candidate : relationships) {
            if (candidate.getAttribute("source").equals(relationship.getAttribute("target"))
                    && candidate.getAttribute("target").equals(relationship.getAttribute("source"))
                    && joinPairs(candidate, false).equals(reverseJoins)) {
                return candidate;
            }
        }
        return null;
    }

    // the FK side references exactly one master row, so the created reverse is always to-one
    private Element createReverseRelationship(
            UpgradeContext upgradeUnit, Element relationship, List<Element> relationships) {

        Document document = upgradeUnit.getDocument();
        String source = relationship.getAttribute("target");
        String target = relationship.getAttribute("source");

        Element reverse = document.createElementNS(relationship.getNamespaceURI(), "dbRelationship");
        reverse.setAttribute("name", reverseName(target, source, relationships));
        reverse.setAttribute("source", source);
        reverse.setAttribute("target", target);
        for (Element join : joins(relationship)) {
            Element reverseJoin = document.createElementNS(join.getNamespaceURI(), "dbAttributePair");
            reverseJoin.setAttribute("source", join.getAttribute("target"));
            reverseJoin.setAttribute("target", join.getAttribute("source"));
            reverse.appendChild(reverseJoin);
        }
        relationship.getParentNode().insertBefore(reverse, relationship.getNextSibling());

        upgradeUnit.recordChange(("DbRelationship '%s' from '%s' to '%s' was added as the reverse of '%s', to mark "
                + "the side that holds the foreign key").formatted(reverse.getAttribute("name"), source, target,
                relationship.getAttribute("name")), false);
        return reverse;
    }

    // the target name in lowerCamelCase, e.g. "ARTIST_GROUP" becomes "artistGroup", made unique within the source
    private String reverseName(String target, String source, List<Element> relationships) {
        StringBuilder base = new StringBuilder(target.length());
        boolean upper = false;
        for (char c : target.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                base.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            }
        }

        Set<String> names = new HashSet<>();
        for (Element relationship : relationships) {
            if (relationship.getAttribute("source").equals(source)) {
                names.add(relationship.getAttribute("name"));
            }
        }
        String name = base.toString();
        for (int i = 1; names.contains(name); i++) {
            name = base + String.valueOf(i);
        }
        return name;
    }

    private List<Element> joins(Element relationship) {
        List<Element> joins = new ArrayList<>();
        for (Element child : childElements(relationship)) {
            if (localName(child).equals("dbAttributePair")) {
                joins.add(child);
            }
        }
        return joins;
    }

    // the joins as "source->target" strings, flipped for the reverse direction
    private Set<String> joinPairs(Element relationship, boolean reversed) {
        Set<String> pairs = new HashSet<>();
        for (Element join : joins(relationship)) {
            String source = join.getAttribute("source");
            String target = join.getAttribute("target");
            pairs.add(reversed ? target + "->" + source : source + "->" + target);
        }
        return pairs;
    }

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
