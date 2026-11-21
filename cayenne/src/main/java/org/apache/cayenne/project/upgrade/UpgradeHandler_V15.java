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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Upgrades projects to version 15.
 * <p>
 * The DOM is parsed without namespaces, with "xmlns" declarations as plain attributes. So the elements are created and
 * renamed without a namespace to inherit the one of their parent, and removing "xmlns" moves an element to it.
 *
 * @since 6.0
 */
public final class UpgradeHandler_V15 implements UpgradeHandler {

    private static final Pattern INFO_NAMESPACE = Pattern.compile("http://cayenne\\.apache\\.org/schema/\\d+/info");
    private static final Pattern DBIMPORT_NAMESPACE =
            Pattern.compile("http://cayenne\\.apache\\.org/schema/\\d+/dbimport");

    @Override
    public String getVersion() {
        return "15";
    }

    @Override
    public void upgradeProjectDOM(UpgradeContext context) {
        updateProjectSchema(context);
        convertValidationExtension(context);
        renameInspection(context);
    }

    @Override
    public void upgradeDataMapDOM(UpgradeContext context) {
        updateDataMapSchema(context);
        convertCgenExtension(context);
        convertDbImportExtension(context);
        convertInfoProperties(context);
        convertForeignKeys(context);
        dropSqlQueryDbRoots(context);
        dropEntityCallbacks(context);
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

    // removing the namespace declaration moves the tags to the DataMap schema
    private void convertCgenExtension(UpgradeContext upgradeUnit) {
        for (Element cgen : elements(upgradeUnit, "/dataMap/*[local-name()='cgen']")) {
            cgen.removeAttribute("xmlns");
        }
    }

    // the legacy "config" tag is renamed to "dbImport", or dropped if there's a "dbImport" already, as it was ignored
    private void convertDbImportExtension(UpgradeContext upgradeUnit) {
        String dbImportPath = "/dataMap/*[local-name()='dbImport']";
        boolean hasDbImport = !elements(upgradeUnit, dbImportPath).isEmpty();
        for (Element config : elements(upgradeUnit, "/dataMap/*[local-name()='config']")) {
            String namespace = namespaceURI(config);
            if (namespace == null || !DBIMPORT_NAMESPACE.matcher(namespace).matches()) {
                continue;
            }

            if (hasDbImport) {
                config.getParentNode().removeChild(config);
                upgradeUnit.recordChange("The legacy 'config' tag of the reverse engineering settings was removed, "
                        + "as the DataMap has a 'dbImport' tag that replaces it", List.of(), false);
            } else {
                upgradeUnit.getDocument().renameNode(config, null, "dbImport");
                hasDbImport = true;
            }
        }

        for (Element dbImport : elements(upgradeUnit, dbImportPath)) {
            dbImport.removeAttribute("xmlns");
        }
    }

    // "comment" properties become "comment" tags, the other properties are dropped
    private void convertInfoProperties(UpgradeContext upgradeUnit) {
        for (Element property : elements(upgradeUnit, "//*[local-name()='property']")) {
            if (!isInfoProperty(property)) {
                continue;
            }

            Element parent = (Element) property.getParentNode();
            String name = property.getAttribute("name");
            if ("comment".equals(name)) {
                String comment = property.getAttribute("value");
                if (!comment.isEmpty()) {
                    commentElement(parent).setTextContent(comment);
                }
            } else {
                upgradeUnit.recordChange("Property '%s' of %s was removed, as only comments are supported"
                        .formatted(name, describe(parent)), List.of(), false);
            }
            parent.removeChild(property);
        }

        removeInfoNamespaceDeclarations(upgradeUnit.getDocument());
    }

    // adds the "comment" as the first child if missing
    private Element commentElement(Element parent) {
        List<Element> children = childElements(parent);
        for (Element child : children) {
            if (localName(child).equals("comment")) {
                return child;
            }
        }

        Element comment = parent.getOwnerDocument().createElement("comment");
        parent.insertBefore(comment, children.isEmpty() ? null : children.get(0));
        return comment;
    }

    // e.g. "objEntity 'Artist'"
    private String describe(Element element) {
        String name = element.hasAttribute("name") ? element.getAttribute("name") : element.getAttribute("className");
        return name.isEmpty() ? localName(element) : "%s '%s'".formatted(localName(element), name);
    }

    private void removeInfoNamespaceDeclarations(Document document) {
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            NamedNodeMap attributes = element.getAttributes();
            List<String> declarations = new ArrayList<>();
            for (int j = 0; j < attributes.getLength(); j++) {
                Attr attribute = (Attr) attributes.item(j);
                if (attribute.getName().startsWith("xmlns:")
                        && INFO_NAMESPACE.matcher(attribute.getValue()).matches()) {
                    declarations.add(attribute.getName());
                }
            }
            declarations.forEach(element::removeAttribute);
        }
    }

    private void convertValidationExtension(UpgradeContext upgradeUnit) {
        for (Element validation : elements(upgradeUnit, "/project/*[local-name()='validation']")) {
            validation.removeAttribute("xmlns");
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
     * Removes the callbacks mapped on "objEntity" ("postAdd", "prePersist", etc.), that are no longer supported. The
     * user has to implement the callback interfaces instead, so the change is recorded as affecting the runtime, and a
     * project with mapped callbacks is not upgraded in memory.
     */
    private void dropEntityCallbacks(UpgradeContext upgradeUnit) {
        Set<String> callbackTags = Set.of("postAdd", "prePersist", "postPersist", "preUpdate", "postUpdate",
                "preRemove", "postRemove", "postLoad");

        for (Element entity : elements(upgradeUnit, "/dataMap/*[local-name()='objEntity']")) {
            List<String> callbacks = new ArrayList<>();
            for (Element child : childElements(entity)) {
                if (callbackTags.contains(localName(child))) {
                    callbacks.add(localName(child) + ": " + child.getAttribute("methodName"));
                    entity.removeChild(child);
                }
            }

            if (!callbacks.isEmpty()) {
                upgradeUnit.recordChange(("Callbacks mapped on ObjEntity '%s' were removed. To keep them "
                        + "working, implement the callback interfaces from 'org.apache.cayenne.lifecycle' in the "
                        + "Persistent class, e.g. PostAddAware")
                        .formatted(entity.getAttribute("name")), callbacks, true);
            }
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

        Element reverse = document.createElement("dbRelationship");
        reverse.setAttribute("name", reverseName(target, source, relationships));
        reverse.setAttribute("source", source);
        reverse.setAttribute("target", target);
        for (Element join : joins(relationship)) {
            Element reverseJoin = document.createElement("dbAttributePair");
            reverseJoin.setAttribute("source", join.getAttribute("target"));
            reverseJoin.setAttribute("target", join.getAttribute("source"));
            reverse.appendChild(reverseJoin);
        }
        relationship.getParentNode().insertBefore(reverse, relationship.getNextSibling());

        upgradeUnit.recordChange(("DbRelationship '%s' from '%s' to '%s' was added as the reverse of '%s', to mark "
                + "the side that holds the foreign key").formatted(reverse.getAttribute("name"), source, target,
                relationship.getAttribute("name")), List.of(), false);
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

    /**
     * Returns whether an element is a property of the "info" extension. Also used by {@link UpgradeHandler_V14}.
     */
    static boolean isInfoProperty(Element element) {
        String name = element.getNodeName();
        String namespace = namespaceURI(element);
        return name.substring(name.indexOf(':') + 1).equals("property")
                && namespace != null && INFO_NAMESPACE.matcher(namespace).matches();
    }

    // resolves the namespace from the "xmlns" attributes of the element and its ancestors
    private static String namespaceURI(Element element) {
        String name = element.getNodeName();
        int colon = name.indexOf(':');
        String declaration = colon > 0 ? "xmlns:" + name.substring(0, colon) : "xmlns";
        for (Node node = element; node instanceof Element e; node = e.getParentNode()) {
            if (e.hasAttribute(declaration)) {
                return e.getAttribute(declaration);
            }
        }
        return null;
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
