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

package org.apache.cayenne.project.upgrade.handlers.v12;

import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class transforming "toDependentPK" flag to "fk" in db relationships
 *
 * @since 5.0-M2
 */
public class ToDepPkToFkUpdater {

    private static final Logger logger = LoggerFactory.getLogger(ToDepPkToFkUpdater.class);

    private static final String TO_DEP_PK_TAG = "toDependentPK";
    private static final String FK_TAG = "fk";
    private static final String DB_RELATIONSHIP_TAG = "db-relationship";
    private static final String DB_ENTITY_TAG = "db-entity";
    private static final String DB_ATTRIBUTE_TAG = "db-attribute";
    private static final String DB_ATTRIBUTE_PAIR_TAG = "db-attribute-pair";

    public void update(List<UpgradeUnit> units) {

        List<Element> dataMaps = new ArrayList<>(units.size());
        List<NodeList> combinedDbEntityList = new ArrayList<>();

        for (UpgradeUnit upgradeUnit : units) {
            Element dataMap = upgradeUnit.getDocument().getDocumentElement();
            dataMaps.add(dataMap);
            combinedDbEntityList.add(dataMap.getElementsByTagName(DB_ENTITY_TAG));
        }

        for (Element dataMap : dataMaps) {
            NodeList relationships = dataMap.getElementsByTagName(DB_RELATIONSHIP_TAG);

            for (int i = 0; i < relationships.getLength(); i++) {
                Node relationship = relationships.item(i);

                Element reverseRelationship = findReverseRelationship(relationships, relationship);

                if (reverseRelationship == null) {
                    reverseRelationship = findReverseInAllDatamaps(dataMaps, relationship);
                }

                if (!isToDepPK(relationship)
                        && !isToDepPK(reverseRelationship)
                        && !isFK(relationship)
                        && !isFK(reverseRelationship)) {
                    setFk((Element) relationship, reverseRelationship, combinedDbEntityList);
                }

                if (isToDepPK(relationship)) {
                    handleToDepPK(relationship, reverseRelationship, combinedDbEntityList);
                }
            }
        }
    }

    private Element findReverseInAllDatamaps(List<Element> dataMaps, Node relationship) {
        for (Element dataMap : dataMaps) {
            NodeList relationships = dataMap.getElementsByTagName(DB_RELATIONSHIP_TAG);
            Element reverseRelationship = findReverseRelationship(relationships, relationship);
            if (reverseRelationship != null) {
                return reverseRelationship;
            }
        }
        return null;
    }

    private void setFk(Element relationship, Element reverseRelationship, List<NodeList> combinedDbEntityList) {

        Node sourceEntity = getDbEntityByJoinDirection(combinedDbEntityList, relationship, "source");
        Node targetEntity = getDbEntityByJoinDirection(combinedDbEntityList, relationship, "target");

        NodeList pairNodes = relationship.getElementsByTagName(DB_ATTRIBUTE_PAIR_TAG);
        for (int i = 0; i < pairNodes.getLength(); i++) {
            if (sourceEntity != null && targetEntity != null) {
                Node currentNode = pairNodes.item(i);

                boolean sourceIsPK = isPrimaryKey(sourceEntity, currentNode, "source");
                boolean targetIsPK = isPrimaryKey(targetEntity, currentNode, "target");

                if (sourceIsPK != targetIsPK) {
                    handleDefinedFkCase(relationship, reverseRelationship, sourceIsPK);
                    return;
                } else {
                    handleUndefinedFkCase(relationship, reverseRelationship, pairNodes, i);
                }
            }
        }
    }

    private void handleUndefinedFkCase(Element relationship, Element reverseRelationship, NodeList pairNodes, int i) {
        //Check is this last join, which means there are no joins where fk direction is defined and this
        // relationship has both side.
        if (isLastJoinPair(pairNodes, i) && reverseRelationship != null) {
            relationship.setAttribute(FK_TAG, "true");
            logger.warn(String.format("Foreign key in relationships pair %s and %s was undefined. FK = true was set in the %s by default"
                    , relationship.getAttribute("name")
                    , reverseRelationship.getAttribute("name")
                    , relationship.getAttribute("name")));
        }
    }

    private void handleDefinedFkCase(Element relationship, Element reverseRelationship, boolean sourceIsPK) {
        if (sourceIsPK && reverseRelationship != null) {
            reverseRelationship.setAttribute(FK_TAG, "true");
        }
        if (!sourceIsPK) {
            relationship.setAttribute(FK_TAG, "true");
        }
    }

    private boolean isLastJoinPair(NodeList pairNodes, int i) {
        return i == pairNodes.getLength() - 1;
    }

    private boolean isPrimaryKey(Node entity, Node node, String joinDirection) {
        String joinName = node.getAttributes().getNamedItem(joinDirection).getNodeValue();
        NodeList childNodes = entity.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if(!DB_ATTRIBUTE_TAG.equals(item.getNodeName())) {
                continue;
            }
            if (item.getAttributes() != null) {
                String name = item.getAttributes().getNamedItem("name").getNodeValue();
                if (name.equals(joinName)) {
                    Node isPrimaryKey = item.getAttributes().getNamedItem("isPrimaryKey");
                    if (isPrimaryKey != null) {
                        return isPrimaryKey.getNodeValue().equals("true");
                    }
                }
            }
        }
        return false;
    }

    private Node getDbEntityByJoinDirection(List<NodeList> combinedDbEntityList, Element relationship, String joinDirection) {
        NamedNodeMap relationshipAttrs = relationship.getAttributes();
        String searchedEntityName = relationshipAttrs.getNamedItem(joinDirection).getNodeValue();
        for (NodeList list : combinedDbEntityList) {
            for (int i = 0; i < list.getLength(); i++) {
                String entityName = list.item(i).getAttributes().getNamedItem("name").getNodeValue();
                if (searchedEntityName.equals(entityName)) {
                    return list.item(i);
                }
            }
        }
        return null;
    }

    private boolean isToDepPK(Node relationship) {
        if (relationship == null) {
            return false;
        }
        NamedNodeMap relationshipAttrs = relationship.getAttributes();
        Node toDependentPK = relationshipAttrs.getNamedItem(TO_DEP_PK_TAG);
        return toDependentPK != null && toDependentPK.getNodeValue().equalsIgnoreCase("true");
    }

    private boolean isFK(Node relationship) {
        if (relationship == null) {
            return false;
        }
        NamedNodeMap relationshipAttrs = relationship.getAttributes();
        Node fkNode = relationshipAttrs.getNamedItem(FK_TAG);
        return fkNode != null && fkNode.getNodeValue().equalsIgnoreCase("true");
    }

    private void handleToDepPK(Node relationship, Element reverseRelationship, List<NodeList> combinedDbEntityList) {
        relationship.getAttributes().removeNamedItem(TO_DEP_PK_TAG);
        if (reverseRelationship != null) {
            if (!isToDepPK(reverseRelationship)) {
                reverseRelationship.setAttribute(FK_TAG, "true");
            } else {
                reverseRelationship.getAttributes().removeNamedItem(TO_DEP_PK_TAG);
                setFk((Element) relationship, reverseRelationship, combinedDbEntityList);
            }
        }
    }

    private Element findReverseRelationship(NodeList dbRelationshipList, Node dbRelationshipNode) {
        Element dbRelationship = (Element) dbRelationshipNode;

        String sourceAttr = dbRelationship.getAttribute("source");
        String targetAttr = dbRelationship.getAttribute("target");

        List<DbAttrPair> pairs = getDbAttrPairs(dbRelationship);

        for (int j = 0; j < dbRelationshipList.getLength(); j++) {
            Node candidateDbRelationshipNode = dbRelationshipList.item(j);
            if (candidateDbRelationshipNode.getNodeType() == Node.ELEMENT_NODE) {
                Element candidateDbRelationship = (Element) candidateDbRelationshipNode;
                if (isCandidateSuitable(sourceAttr, targetAttr, pairs, candidateDbRelationship)) {
                    return candidateDbRelationship;
                }
            }
        }
        return null;
    }

    private boolean isCandidateSuitable(String sourceAttr, String targetAttr, List<DbAttrPair> pairs, Element candidateDbRelationship) {
        String candidateSourceAttr = candidateDbRelationship.getAttribute("source");
        String candidateTargetAttr = candidateDbRelationship.getAttribute("target");

        List<DbAttrPair> candidatePairs = getDbAttrPairs(candidateDbRelationship);

        return sourceAttr.equals(candidateTargetAttr)
                && targetAttr.equals(candidateSourceAttr)
                && pairs.size() == candidatePairs.size()
                && containAllReversed(pairs, candidatePairs);
    }

    private boolean containAllReversed(List<DbAttrPair> pairs, List<DbAttrPair> candidatePairs) {
        return pairs.stream()
                .allMatch(pair -> candidatePairs.stream().anyMatch(pair::isReverseFor));
    }

    private List<DbAttrPair> getDbAttrPairs(Element dbRelationship) {
        List<DbAttrPair> pairs = new ArrayList<>();
        NodeList attributes = dbRelationship.getElementsByTagName(DB_ATTRIBUTE_PAIR_TAG);
        for (int j = 0; j < attributes.getLength(); j++) {
            Node attributeNode = attributes.item(j);
            if (attributeNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) attributeNode;
                String pairSource = element.getAttribute("source");
                String pairTarget = element.getAttribute("target");
                pairs.add(new DbAttrPair(pairSource, pairTarget));
            }
        }
        return pairs;
    }

    private static class DbAttrPair {
        private final String source;
        private final String target;

        public DbAttrPair(String source, String target) {
            this.source = source;
            this.target = target;
        }

        private boolean isReverseFor(DbAttrPair pair) {
            return this.source.equals(pair.target) && this.target.equals(pair.source);
        }
    }
}




