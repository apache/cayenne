/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;

/**
 * ProjectTraversal allows to traverse Cayenne project tree in a "depth-first" order
 * starting from an arbitrary level to its children.
 * <p>
 * <i>Current implementation is not very efficient and would actually first read the whole
 * tree, before returning the first element from the iterator.</i>
 * </p>
 */
public class ProjectTraversal {

    protected static final Comparator mapObjectComparator = new MapObjectComparator();
    protected static final Comparator dataMapComparator = new DataMapComparator();
    protected static final Comparator dataDomainComparator = new DataDomainComparator();
    protected static final Comparator dataNodeComparator = new DataNodeComparator();
    protected static final Comparator queryComparator = new QueryComparator();
    protected static final Comparator embaddableComparator = new EmbeddableComparator();

    protected ProjectTraversalHandler handler;
    protected boolean sort;

    public ProjectTraversal(ProjectTraversalHandler handler) {
        this(handler, false);
    }

    /**
     * Creates ProjectTraversal instance with a given handler and sort policy. If
     * <code>sort</code> is true, children of each node will be sorted using a predefined
     * Comparator for a given type of child nodes.
     */
    public ProjectTraversal(ProjectTraversalHandler handler, boolean sort) {
        this.handler = handler;
        this.sort = sort;
    }

    /**
     * Performs traversal starting from the root node. Root node can be of any type
     * supported in Cayenne projects (Configuration, DataMap, DataNode, etc...)
     */
    public void traverse(Object rootNode) {
        this.traverse(rootNode, new ProjectPath());
    }

    public void traverse(Object rootNode, ProjectPath path) {
        if (rootNode instanceof Project) {
            this.traverseProject((Project) rootNode, path);
        }
        else if (rootNode instanceof DataDomain) {
            this.traverseDomains(Collections.singletonList(rootNode).iterator(), path);
        }
        else if (rootNode instanceof DataMap) {
            this.traverseMaps(Collections.singletonList(rootNode).iterator(), path);
        }
        else if (rootNode instanceof Entity) {
            this.traverseEntities(Collections.singletonList(rootNode).iterator(), path);
        }
        else if (rootNode instanceof Embeddable) {
            this.traverseEmbeddable(Collections.singletonList(rootNode).iterator(), path);
        }
        else if (rootNode instanceof EmbeddableAttribute) {
            this.traverseEmbeddableAttributes(Collections.singletonList(rootNode).iterator(), path);
        }
        else if (rootNode instanceof Attribute) {
            this.traverseAttributes(Collections.singletonList(rootNode).iterator(), path);
        }
        else if (rootNode instanceof Relationship) {
            this.traverseRelationships(
                    Collections.singletonList(rootNode).iterator(),
                    path);
        }
        else if (rootNode instanceof DataNode) {
            this.traverseNodes(Collections.singletonList(rootNode).iterator(), path);
        }
        else {
            String nodeClass = (rootNode != null)
                    ? rootNode.getClass().getName()
                    : "(null)";
            throw new IllegalArgumentException("Unsupported root node: " + nodeClass);
        }
    }

    /**
     * Performs traversal starting from the Project and down to its children.
     */
    public void traverseProject(Project project, ProjectPath path) {
        ProjectPath projectPath = path.appendToPath(project);
        handler.projectNode(projectPath);

        if (handler.shouldReadChildren(project, path)) {
            Iterator it = project.getChildren().iterator();
            while (it.hasNext()) {
                this.traverse(it.next(), projectPath);
            }
        }
    }

    /**
     * Performs traversal starting from a list of domains.
     */
    public void traverseDomains(Iterator domains, ProjectPath path) {

        if (sort) {
            domains = Util.sortedIterator(domains, ProjectTraversal.dataDomainComparator);
        }

        while (domains.hasNext()) {
            DataDomain domain = (DataDomain) domains.next();
            ProjectPath domainPath = path.appendToPath(domain);
            handler.projectNode(domainPath);

            if (handler.shouldReadChildren(domain, path)) {
                this.traverseMaps(domain.getDataMaps().iterator(), domainPath);
                this.traverseNodes(domain.getDataNodes().iterator(), domainPath);
            }
        }
    }

    public void traverseNodes(Iterator nodes, ProjectPath path) {
        if (sort) {
            nodes = Util.sortedIterator(nodes, ProjectTraversal.dataNodeComparator);
        }

        while (nodes.hasNext()) {
            DataNode node = (DataNode) nodes.next();
            ProjectPath nodePath = path.appendToPath(node);
            handler.projectNode(nodePath);

            if (handler.shouldReadChildren(node, path)) {
                this.traverseMaps(node.getDataMaps().iterator(), nodePath);
            }
        }
    }

    public void traverseMaps(Iterator maps, ProjectPath path) {
        if (sort) {
            maps = Util.sortedIterator(maps, ProjectTraversal.dataMapComparator);
        }

        while (maps.hasNext()) {
            DataMap map = (DataMap) maps.next();
            ProjectPath mapPath = path.appendToPath(map);
            handler.projectNode(mapPath);

            if (handler.shouldReadChildren(map, path)) {
                this.traverseEntities(map.getObjEntities().iterator(), mapPath);
                this.traverseEmbeddable(map.getEmbeddables().iterator(), mapPath);
                this.traverseEntities(map.getDbEntities().iterator(), mapPath);                
                this.traverseProcedures(map.getProcedures().iterator(), mapPath);
                this.traverseQueries(map.getQueries().iterator(), mapPath);
            }
        }
    }

    public void traverseEmbeddable(Iterator embeddadles, ProjectPath path) {
        if (sort) {
            embeddadles = Util.sortedIterator(
                    embeddadles,
                    ProjectTraversal.embaddableComparator);
        }

        while (embeddadles.hasNext()) {
            Embeddable emd = (Embeddable) embeddadles.next();
            ProjectPath entPath = path.appendToPath(emd);
            handler.projectNode(entPath);

            if (handler.shouldReadChildren(emd, path)) {
                this.traverseEmbeddableAttributes(emd.getAttributes().iterator(), entPath);
            }
        }
    }

    /**
     * Performs recursive traversal of an Iterator of Cayenne Query objects.
     */
    public void traverseQueries(Iterator queries, ProjectPath path) {
        if (sort) {
            queries = Util.sortedIterator(queries, ProjectTraversal.queryComparator);
        }

        while (queries.hasNext()) {
            Query query = (Query) queries.next();
            ProjectPath queryPath = path.appendToPath(query);
            handler.projectNode(queryPath);
        }
    }

    /**
     * Performs recusrive traversal of an Iterator of Cayenne Procedure objects.
     */
    public void traverseProcedures(Iterator procedures, ProjectPath path) {
        if (sort) {
            procedures = Util.sortedIterator(
                    procedures,
                    ProjectTraversal.mapObjectComparator);
        }

        while (procedures.hasNext()) {
            Procedure procedure = (Procedure) procedures.next();
            ProjectPath procedurePath = path.appendToPath(procedure);
            handler.projectNode(procedurePath);

            if (handler.shouldReadChildren(procedure, path)) {
                this.traverseProcedureParameters(
                        procedure.getCallParameters().iterator(),
                        procedurePath);
            }
        }
    }

    public void traverseEntities(Iterator entities, ProjectPath path) {
        if (sort) {
            entities = Util
                    .sortedIterator(entities, ProjectTraversal.mapObjectComparator);
        }

        while (entities.hasNext()) {
            Entity ent = (Entity) entities.next();
            ProjectPath entPath = path.appendToPath(ent);
            handler.projectNode(entPath);

            if (handler.shouldReadChildren(ent, path)) {
                this.traverseAttributes(ent.getAttributes().iterator(), entPath);
                this.traverseRelationships(ent.getRelationships().iterator(), entPath);
            }
        }
    }

    public void traverseAttributes(Iterator attributes, ProjectPath path) {
        if (sort) {
            attributes = Util.sortedIterator(
                    attributes,
                    ProjectTraversal.mapObjectComparator);
        }

        while (attributes.hasNext()) {
            handler.projectNode(path.appendToPath(attributes.next()));
        }
    }
    
    public void traverseEmbeddableAttributes(Iterator emAttributes, ProjectPath path) {
        if (sort) {
            emAttributes = Util.sortedIterator(
                    emAttributes,
                    ProjectTraversal.mapObjectComparator);
        }

        while (emAttributes.hasNext()) {
            handler.projectNode(path.appendToPath(emAttributes.next()));
        }
    }

    public void traverseRelationships(Iterator relationships, ProjectPath path) {
        if (sort) {
            relationships = Util.sortedIterator(
                    relationships,
                    ProjectTraversal.mapObjectComparator);
        }

        while (relationships.hasNext()) {
            handler.projectNode(path.appendToPath(relationships.next()));
        }
    }

    public void traverseProcedureParameters(
            Iterator<? extends ProcedureParameter> parameters,
            ProjectPath path) {
        // Note: !! do not try to sort parameters - they are positional by definition

        while (parameters.hasNext()) {
            handler.projectNode(path.appendToPath(parameters.next()));
        }
    }

    static class QueryComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String name1 = ((Query) o1).getName();
            String name2 = ((Query) o2).getName();

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }
    }

    static class MapObjectComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String name1 = ((CayenneMapEntry) o1).getName();
            String name2 = ((CayenneMapEntry) o2).getName();

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }
    }

    static class EmbeddableComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String name1 = ((Embeddable) o1).getClassName();
            String name2 = ((Embeddable) o2).getClassName();

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }
    }

    static class DataMapComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String name1 = ((DataMap) o1).getName();
            String name2 = ((DataMap) o2).getName();

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }

    }

    static class DataDomainComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String name1 = ((DataDomain) o1).getName();
            String name2 = ((DataDomain) o2).getName();

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }

    }

    static class DataNodeComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            String name1 = ((DataNode) o1).getName();
            String name2 = ((DataNode) o2).getName();

            if (name1 == null) {
                return (name2 != null) ? -1 : 0;
            }
            else if (name2 == null) {
                return 1;
            }
            else {
                return name1.compareTo(name2);
            }
        }
    }
}
