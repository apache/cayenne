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

package org.apache.cayenne.map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.XMLSerializable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * An Entity is an abstract descriptor for an entity mapping concept. Entity can represent
 * either a descriptor of database table or a persistent object.
 * 
 */
public abstract class Entity<E extends Entity<E, A, R>, A extends Attribute<E, A, R>, R extends Relationship<E, A, R>>
        implements CayenneMapEntry, XMLSerializable, Serializable {

    public static final String PATH_SEPARATOR = ".";

    /**
     * A prefix or a suffix that can be used in a path component to indicate that an OUTER
     * JOIN should be used when resolving the expression.
     * 
     * @since 3.0
     */
    public static final String OUTER_JOIN_INDICATOR = "+";

    protected String name;
    protected DataMap dataMap;

    protected final Map<String, A> attributes = new LinkedHashMap<>();
    protected final Map<String, R> relationships = new LinkedHashMap<>();

    /**
     * Creates an unnamed Entity.
     */
    public Entity() {
        this(null);
    }

    /**
     * Creates a named Entity.
     */
    public Entity(String name) {
        setName(name);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }

    /**
     * Returns entity name. Name is a unique identifier of the entity within its DataMap.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getParent() {
        return getDataMap();
    }

    public void setParent(Object parent) {
        if (parent != null && !(parent instanceof DataMap)) {
            throw new IllegalArgumentException("Expected null or DataMap, got: " + parent);
        }

        setDataMap((DataMap) parent);
    }

    /**
     * @return parent DataMap of this entity.
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * Sets parent DataMap of this entity.
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns attribute with name <code>attributeName</code> or null if no attribute
     * with this name exists.
     */
    public A getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    /**
     * Adds new attribute to the entity, setting its parent entity to be this object. If
     * attribute has no name, IllegalArgumentException is thrown.
     */
    public void addAttribute(A attribute) {
        if (attribute.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed attribute.");
        }

        // block overrides

        // TODO: change method signature to return replaced attribute and make sure the Modeler handles it...
        A existingAttribute = attributes.get(attribute.getName());
        if (existingAttribute != null) {
            if (existingAttribute == attribute) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override attribute '" + attribute.getName() + "'");
            }
        }

        // Check that there aren't any relationships with the same name as the given
        // attribute.
        Object existingRelationship = relationships.get(attribute.getName());
        if (existingRelationship != null) {
            throw new IllegalArgumentException(
                    "Attribute name conflict with existing relationship '" + attribute.getName() + "'");
        }

        attributes.put(attribute.getName(), attribute);
        attribute.setEntity(this);
    }

    /**
     * Removes an attribute named <code>attrName</code>.
     */
    public void removeAttribute(String attrName) {
        attributes.remove(attrName);
    }

    /**
     *
     * @since 4.0
     */
    public void updateAttribute(A attribute) {
        removeAttribute(attribute.getName());
        addAttribute(attribute);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    /**
     * Returns relationship with name <code>relName</code>. Will return null if no
     * relationship with this name exists in the entity.
     */
    public R getRelationship(String relName) {
        return relationships.get(relName);
    }

    /**
     * Adds new relationship to the entity.
     */
    public void addRelationship(R relationship) {
        if (relationship.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed relationship.");
        }

        // block overrides

        // TODO: change method signature to return replaced attribute and make sure the
        // Modeler handles it...
        Object existingRelationship = relationships.get(relationship.getName());
        if (existingRelationship != null) {
            if (existingRelationship == relationship) {
                return;
            } else {
                throw new IllegalArgumentException(
                        "An attempt to override relationship '" + relationship.getName() + "'");
            }
        }

        // Check that there aren't any attributes with the same name as the given
        // relationship.
        Object existingAttribute = attributes.get(relationship.getName());
        if (existingAttribute != null) {
            throw new IllegalArgumentException(
                    "Relationship name conflict with existing attribute '" + relationship.getName() + "'");
        }

        relationships.put(relationship.getName(), relationship);
        @SuppressWarnings("unchecked")
        E sourceEntity = (E) this;
        relationship.setSourceEntity(sourceEntity);
    }

    /**
     *  Removes a relationship named <code>attrName</code>.
     */
    public void removeRelationship(String relName) {
        relationships.remove(relName);
    }

    public void clearRelationships() {
        relationships.clear();
    }

    /**
     * Returns an unmodifiable map of relationships sorted by name.
     */
    public Map<String, R> getRelationshipMap() {
        // create a new instance ... earlier attempts to cache it in the entity caused
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableMap(relationships);
    }

    /**
     * Returns a relationship that has a specified entity as a target. If there is more
     * than one relationship for the same target, it is unpredictable which one will be
     * returned.
     * 
     * @since 1.1
     */
    public R getAnyRelationship(E targetEntity) {
        if (getRelationships().isEmpty()) {
            return null;
        }

        for (R r : getRelationships()) {
            if (r.getTargetEntity() == targetEntity) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable collection of Relationships that exist in this entity.
     */
    public Collection<R> getRelationships() {
        // create a new instance ... earlier attempts to cache it in the entity caused
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableCollection(relationships.values());
    }

    /**
     * Returns an unmodifiable sorted map of entity attributes.
     */
    public Map<String, A> getAttributeMap() {
        // create a new instance ... earlier attempts to cache it in the entity caused
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Returns an unmodifiable collection of entity attributes.
     */
    public Collection<A> getAttributes() {
        // create a new instance ... earlier attempts to cache it in the entity caused
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableCollection(attributes.values());
    }

    /**
     * Translates Expression rooted in this entity to an analogous expression rooted in
     * related entity.
     * 
     * @since 1.1
     */
    public Expression translateToRelatedEntity(Expression expression, String relationshipPath) {
        return translateToRelatedEntity(expression, CayennePath.of(relationshipPath));
    }

    /**
     * Transforms Expression rooted in this entity to an analogous expression
     * rooted in related entity.
     *
     * @since 5.0
     */
    public abstract Expression translateToRelatedEntity(Expression expression, CayennePath relationshipPath);

    /**
     * Convenience method returning the last component in the path iterator. If the last
     * component is an alias, it is fully resolved down to the last ObjRelationship.
     * 
     * @since 3.0
     */
    public PathComponent<A, R> lastPathComponent(
            Expression path,
            Map<String, String> aliasMap) {

        for (PathComponent<A, R> component : resolvePath(path, aliasMap)) {
            if (component.isLast()) {
                // resolve aliases if needed
                return lastPathComponent(component);
            }
        }

        return null;
    }

    private PathComponent<A, R> lastPathComponent(PathComponent<A, R> component) {
        
        if (!component.isAlias()) {
            return component;
        }

        for (PathComponent<A, R> subcomponent : component.getAliasedPath()) {
            if (subcomponent.isLast()) {
                return lastPathComponent(subcomponent);
            }
        }

        throw new IllegalStateException("Invalid last path component: " + component.getName());
    }

    /**
     * Returns an Iterable over the path components with elements represented as
     * {@link PathComponent} instances, encapsulating a relationship, an attribute or a
     * subpath alias. An optional "aliasMap" parameter is used to resolve subpaths from
     * aliases.
     * <p>
     * This method is lazy: if path is invalid and can not be resolved from this entity,
     * this method will still return an Iterator, but an attempt to read the first invalid
     * path component will result in ExpressionException.
     * </p>
     * 
     * @since 3.0
     */
    public abstract Iterable<PathComponent<A, R>> resolvePath(
            Expression pathExp,
            Map<String, String> aliasMap);

    /**
     * Processes expression <code>pathExp</code> and returns an Iterator of path
     * components that contains a sequence of Attributes and Relationships. Note that if
     * path is invalid and can not be resolved from this entity, this method will still
     * return an Iterator, but an attempt to read the first invalid path component will
     * result in ExpressionException.
     */
    public abstract Iterator<CayenneMapEntry> resolvePathComponents(Expression pathExp) throws ExpressionException;

    /**
     * Returns an Iterator over the path components that contains a sequence of Attributes
     * and Relationships. Note that if path is invalid and can not be resolved from this
     * entity, this method will still return an Iterator, but an attempt to read the first
     * invalid path component will result in ExpressionException.
     */
    public Iterator<CayenneMapEntry> resolvePathComponents(String path) throws ExpressionException {
        return new PathIterator(CayennePath.of(path));
    }

    public Iterator<CayenneMapEntry> resolvePathComponents(CayennePath path) throws ExpressionException {
        return new PathIterator(path);
    }

    /**
     * An iterator resolving mapping components represented by the path string.
     * This entity is assumed to be the root of the path.
     */
    final class PathIterator implements Iterator<CayenneMapEntry> {
        private final CayennePath path;
        private final Iterator<CayennePathSegment> iterator;
        private Entity<E, A, R> currentEntity;

        PathIterator(CayennePath path) {
            this.currentEntity = Entity.this;
            this.path = path;
            this.iterator = path.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public CayenneMapEntry next() {
            CayennePathSegment nextSegment = iterator.next();
            // see if this is an attribute
            A attr = currentEntity.getAttribute(nextSegment.value());
            if (attr != null) {
                // do a sanity check...
                if (iterator.hasNext()) {
                    throw new ExpressionException("Attribute must be the last component of the path: '%s'.",
                            path, null, nextSegment.value());
                }
                return attr;
            }

            R rel = currentEntity.getRelationship(nextSegment.value());
            if (rel != null) {
                currentEntity = rel.getTargetEntity();
                if (currentEntity != null || !iterator.hasNext()) { //otherwise an exception will be thrown
                    return rel;
                }
            }
            
            String entityName = (currentEntity != null) ? currentEntity.getName() : "(?)";
            throw new ExpressionException("Can't resolve path component: [%s.%s].", path, null, entityName, nextSegment.value());
        }
    }

    final MappingNamespace getNonNullNamespace() {
        MappingNamespace parent = getDataMap();
        if (parent == null) {
            throw new CayenneRuntimeException("Entity '%s' has no parent MappingNamespace (such as DataMap)", getName());
        }

        return parent;
    }
}
