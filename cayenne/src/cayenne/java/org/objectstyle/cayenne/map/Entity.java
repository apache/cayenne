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
package org.objectstyle.cayenne.map;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.CayenneMap;

/**
 * An Entity is an abstract descriptor for an entity mapping concept.
 * Entity can represent either a descriptor of database table or
 * a persistent object.
 *
 * @author Andrei Adamchik
 */
public abstract class Entity extends MapObject {
    public static final String PATH_SEPARATOR = ".";

    // ====================================================
    // Attributes
    // ====================================================
    protected CayenneMap attributes = new CayenneMap(this);
    //  read-through reference for public access
    protected SortedMap attributesMapRef = Collections.unmodifiableSortedMap(attributes);
    //  read-through reference for public access
    protected Collection attributesRef =
        Collections.unmodifiableCollection(attributes.values());

    // ====================================================
    // Relationships
    // ====================================================
    protected CayenneMap relationships = new CayenneMap(this);
    //  read-through reference for public access
    protected SortedMap relationshipsMapRef =
        Collections.unmodifiableSortedMap(relationships);
    //  read-through reference for public access
    protected Collection relationshipsRef =
        Collections.unmodifiableCollection(relationships.values());

    /**
     * @return parent DataMap of this entity.
     */
    public DataMap getDataMap() {
        return (DataMap) getParent();
    }

    /**
     * Sets parent DataMap of this entity.
     */
    public void setDataMap(DataMap dataMap) {
        this.setParent(dataMap);
    }

    /**
     * Returns a named query associated with this entity.
     * 
     * @since 1.1 Return type is changed to Query from SelectQuery.
     * @deprecated Since 1.1 Queries are stored at the DataMap level.
     */
    public Query getQuery(String queryName) {
        return getDataMap().getQuery(getName() + ":" + queryName);
    }

    /**
     * Creates a named association of a SelectQuery with this entity. Throws
     * IllegalArgumentException if query root can not be resolved to this
     * entity.
     * 
     * @deprecated Since 1.1 Queries are stored at the DataMap level.
     */
    public void addQuery(String queryName, Query query) {
        query.setName(getName() + ":" + queryName);
        getDataMap().addQuery(query);
    }

    /**
     * Removes a named query from this Entity.
     * 
     * @deprecated Since 1.1 Queries are stored at the DataMap level.
     */
    public void removeQuery(String queryName) {
        getDataMap().removeQuery(getName() + ":" + queryName);
    }

    /**
     * @deprecated Since 1.1 Queries are stored at the DataMap level.
     */
    public void clearQueries() {
        // TODO: for backwards compatibility
        // we must scan all queries that start with this entity
        // name and only clean those.
        getDataMap().clearQueries();
    }

    /**
     * Helper method that checks that a Query belongs to this entity by
     * validating query root object.
     *
     * @throws IllegalArgumentException if query does not belong to this entity.
     * @deprecated Unused since 1.1
     */
    protected abstract void validateQueryRoot(Query query)
        throws IllegalArgumentException;

    /**
     * Returns attribute with name <code>attrName</code>.
     * Will return null if no attribute with this name exists.
     */
    public Attribute getAttribute(String attrName) {
        return (Attribute) attributes.get(attrName);
    }

    /**
     * Adds new attribute to the entity. If attribute has no name,
     * IllegalArgumentException is thrown.
     *
     * Also sets <code>attr</code>'s entity to be this entity.
     */
    public void addAttribute(Attribute attr) {
        if (attr.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed attribute.");
        }

        attributes.put(attr.getName(), attr);
    }

    /** Removes an attribute named <code>attrName</code>.*/
    public void removeAttribute(String attrName) {
        attributes.remove(attrName);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    /**
     * Returns relationship with name <code>relName</code>.
     * Will return null if no relationship with this name
     * exists in the entity.
     */
    public Relationship getRelationship(String relName) {
        return (Relationship) relationships.get(relName);
    }

    /** Adds new relationship to the entity. */
    public void addRelationship(Relationship rel) {
        relationships.put(rel.getName(), rel);
    }

    /** Removes a relationship named <code>attrName</code>.*/
    public void removeRelationship(String relName) {
        relationships.remove(relName);
    }

    public void clearRelationships() {
        relationships.clear();
    }

    /**
     * Returns a map of relationships sorted by name.
     */
    public SortedMap getRelationshipMap() {
        return relationshipsMapRef;
    }

    /**
     * Returns a relationship that has a specified entity as a target.
     * If there is more than one relationship for the same target,
     * it is unpredictable which one will be returned.
     * 
     * @since 1.1
     */
    public Relationship getAnyRelationship(Entity targetEntity) {
        Collection relationships = getRelationships();
        if (relationships.isEmpty()) {
            return null;
        }

        Iterator it = relationships.iterator();
        while (it.hasNext()) {
            Relationship r = (Relationship) it.next();
            if (r.getTargetEntity() == targetEntity) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns a collection of Relationships that exist in this entity.
     */
    public Collection getRelationships() {
        return relationshipsRef;
    }

    /**
     * Returns entity attributes as an unmodifiable map. Since 1.1 returns
     * a SortedMap.
     */
    public SortedMap getAttributeMap() {
        return attributesMapRef;
    }

    /**
     * Returns entity attributes.
     */
    public Collection getAttributes() {
        return attributesRef;
    }

    /**
     * Translates Expression rooted in this entity to an analogous expression 
     * rooted in related entity.
     * 
     * @since 1.1
     */
    public abstract Expression translateToRelatedEntity(
        Expression expression,
        String relationshipPath);

    /**
     * Convenience method returning the last component in the path iterator.
     * 
     * @since 1.1
     * @see #resolvePathComponents(Expression)
     */
    public Object lastPathComponent(Expression pathExp) {
        Object last = null;
        Iterator it = resolvePathComponents(pathExp);
        while (it.hasNext()) {
            last = it.next();
        }

        return last;
    }

    /**
     * Processes expression <code>pathExp</code> and returns an Iterator
     * of path components that contains a sequence of Attributes and Relationships.
     * Note that if path is invalid and can not be resolved from this entity,
     * this method will still return an Iterator, but an attempt to read the first
     * invalid path component will result in ExpressionException.
     */
    public abstract Iterator resolvePathComponents(Expression pathExp)
        throws ExpressionException;

    /**
     * Returns an Iterator over the path components that contains a sequence of 
     * Attributes and Relationships. Note that if path is invalid and can not be 
     * resolved from this entity, this method will still return an Iterator, 
     * but an attempt to read the first invalid path component will result in 
     * ExpressionException.
     */
    public Iterator resolvePathComponents(String path) throws ExpressionException {
        return new PathIterator(path);
    }

    // An iterator resolving mapping components represented by the path string.
    // This entity is assumed to be the root of the path.
    final class PathIterator implements Iterator {

        private StringTokenizer toks;
        private Entity currentEnt;
        private String path;

        PathIterator(String path) {
            super();
            this.currentEnt = Entity.this;
            this.toks = new StringTokenizer(path, PATH_SEPARATOR);
            this.path = path;
        }

        public boolean hasNext() {
            return toks.hasMoreTokens();
        }

        public Object next() {
            String pathComp = toks.nextToken();

            // see if this is an attribute
            Attribute attr = currentEnt.getAttribute(pathComp);
            if (attr != null) {
                // do a sanity check...
                if (toks.hasMoreTokens()) {
                    throw new ExpressionException(
                        "Attribute must be the last component of the path: '"
                            + pathComp
                            + "'.",
                        path,
                        null);
                }

                return attr;
            }

            Relationship rel = currentEnt.getRelationship(pathComp);
            if (rel != null) {
                currentEnt = rel.getTargetEntity();
                return rel;
            }

            // build error message
            StringBuffer buf = new StringBuffer();
            buf
                .append("Can't resolve path component: [")
                .append(currentEnt.getName())
                .append('.')
                .append(pathComp)
                .append("].");
            throw new ExpressionException(buf.toString(), path, null);
        }

        public void remove() {
            throw new UnsupportedOperationException("'remove' operation is not supported.");
        }
    }

    final MappingNamespace getNonNullNamespace() {
        MappingNamespace parent = (MappingNamespace) getParent();
        if (parent == null) {
            throw new CayenneRuntimeException(
                "Entity '"
                    + getName()
                    + "' has no parent MappingNamespace (such as DataMap)");
        }

        return parent;
    }
}
