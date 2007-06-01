/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.MapObject;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * ProjectTraversal allows to traverse Cayenne project tree in a
 * "depth-first" order starting from an arbitrary level to its children. 
 * 
 * <p><i>Current implementation is not very efficient
 * and would actually first read the whole tree, before returning 
 * the first element from the iterator.</i></p>
 * 
 * @author Andrei Adamchik
 */
public class ProjectTraversal {

    protected static final Comparator mapObjectComparator = new MapObjectComparator();
    protected static final Comparator dataMapComparator = new DataMapComparator();
    protected static final Comparator dataDomainComparator = new DataDomainComparator();
    protected static final Comparator dataNodeComparator = new DataNodeComparator();
    protected static final Comparator queryComparator = new QueryComparator();

    protected ProjectTraversalHandler handler;
    protected boolean sort;

    public ProjectTraversal(ProjectTraversalHandler handler) {
        this(handler, false);
    }

    /**
     * Creates ProjectTraversal instance with a given handler and 
     * sort policy. If <code>sort</code> is true, children of each
     * node will be sorted using a predefined Comparator for a given 
     * type of child nodes.
     */
    public ProjectTraversal(ProjectTraversalHandler handler, boolean sort) {
        this.handler = handler;
        this.sort = sort;
    }

    /**
     * Performs traversal starting from the root node. Root node can be
     * of any type supported in Cayenne projects (Configuration, DataMap, DataNode, etc...)
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
            String nodeClass =
                (rootNode != null) ? rootNode.getClass().getName() : "(null)";
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
                this.traverseEntities(map.getDbEntities().iterator(), mapPath);
                this.traverseProcedures(map.getProcedures().iterator(), mapPath);
                this.traverseQueries(map.getQueries().iterator(), mapPath);
            }
        }
    }

    /**
     * Performs recusrive traversal of an Iterator of Cayenne Query objects.
     */
    public void traverseQueries(Iterator queries, ProjectPath path) {
        if (sort) {
            queries =
                Util.sortedIterator(queries, ProjectTraversal.queryComparator);
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
            procedures =
                Util.sortedIterator(procedures, ProjectTraversal.mapObjectComparator);
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
            entities =
                Util.sortedIterator(entities, ProjectTraversal.mapObjectComparator);
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
            attributes =
                Util.sortedIterator(attributes, ProjectTraversal.mapObjectComparator);
        }

        while (attributes.hasNext()) {
            handler.projectNode(path.appendToPath(attributes.next()));
        }
    }

    public void traverseRelationships(Iterator relationships, ProjectPath path) {
        if (sort) {
            relationships =
                Util.sortedIterator(relationships, ProjectTraversal.mapObjectComparator);
        }

        while (relationships.hasNext()) {
            handler.projectNode(path.appendToPath(relationships.next()));
        }
    }

    public void traverseProcedureParameters(Iterator parameters, ProjectPath path) {
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
            String name1 = ((MapObject) o1).getName();
            String name2 = ((MapObject) o2).getName();

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
