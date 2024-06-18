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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * Describes an association between two Java classes mapped as source and target
 * ObjEntity. Maps to a path of DbRelationships.
 */
public class ObjRelationship extends Relationship<ObjEntity, ObjAttribute, ObjRelationship> implements ConfigurationNode {

    /**
     * Denotes a default type of to-many relationship collection which is a Java
     * List.
     * 
     * @since 3.0
     */
    public static final String DEFAULT_COLLECTION_TYPE = "java.util.List";

    boolean readOnly;

    protected int deleteRule = DeleteRule.NO_ACTION;
    protected boolean usedForLocking;

    protected List<DbRelationship> dbRelationships = new ArrayList<>(2);

    /**
     * Db-relationships path that is set but not yet parsed (turned into
     * List&lt;DbRelationship&gt;) Used during map loading
     */
    volatile CayennePath deferredPath;

    /**
     * Stores the type of collection mapped by a to-many relationship. Null for
     * to-one relationships.
     * 
     * @since 3.0
     */
    protected String collectionType;

    /**
     * Stores a property name of a target entity used to create a relationship
     * map. Only has effect if collectionType property is set to
     * "java.util.Map".
     * 
     * @since 3.0
     */
    protected String mapKey;

    public ObjRelationship() {
        this(null);
    }

    public ObjRelationship(String name) {
        super(name);
    }

    @Override
    public ObjEntity getSourceEntity() {
        return super.getSourceEntity();
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitObjRelationship(this);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor<?> delegate) {
        ObjEntity source = getSourceEntity();
        if (source == null) {
            return;
        }

        encoder.start("obj-relationship")
                .attribute("name", getName())
                .attribute("source", source.getName());

        // looking up a target entity ensures that bogus names are not saved...
        // whether this is good or bad is debatable, as users may want to point to
        // non-existent entities on purpose.
        ObjEntity target = getTargetEntity();
        if (target != null) {
            encoder.attribute("target", target.getName());
        }

        if (getCollectionType() != null && !DEFAULT_COLLECTION_TYPE.equals(getCollectionType())) {
            encoder.attribute("collection-type", getCollectionType());
        }

        encoder.attribute("lock", isUsedForLocking())
                .attribute("map-key", getMapKey());

        String deleteRule = DeleteRule.deleteRuleName(getDeleteRule());
        if (deleteRule != null && getDeleteRule() != DeleteRule.NO_ACTION) {
            encoder.attribute("deleteRule", deleteRule);
        }

        // quietly get rid of invalid path... this is not the best way of doing
        // things, but it is consistent across map package
        encoder.attribute("db-relationship-path", getValidRelationshipPath());

        delegate.visitObjRelationship(this);
        encoder.end();
    }

    /**
     * Returns a target ObjEntity of this relationship. Entity is looked up in
     * the parent DataMap using "targetEntityName".
     */
    @Override
    public ObjEntity getTargetEntity() {
        String targetName = getTargetEntityName();
        if (targetName == null) {
            return null;
        }

        return getNonNullNamespace().getObjEntity(targetName);
    }

    /**
     * Returns the name of a complimentary relationship going in the opposite
     * direction or null if it doesn't exist.
     * 
     * @since 1.2
     */
    public String getReverseRelationshipName() {
        ObjRelationship reverse = getReverseRelationship();
        return (reverse != null) ? reverse.getName() : null;
    }

    /**
     * Returns a "complimentary" ObjRelationship going in the opposite
     * direction. Returns null if no such relationship is found.
     */
    @Override
    public ObjRelationship getReverseRelationship() {

        // reverse the list
        List<DbRelationship> relationships = getDbRelationships();
        List<DbRelationship> reversed = new ArrayList<>(relationships.size());

        for (DbRelationship relationship : relationships) {
            DbRelationship reverse = relationship.getReverseRelationship();
            if (reverse == null) {
                return null;
            }

            reversed.add(0, reverse);
        }

        ObjEntity target = this.getTargetEntity();
        if (target == null) {
            return null;
        }

        ObjEntity source = getSourceEntity();

        for (ObjRelationship relationship : target.getRelationships()) {
            ObjEntity maybeSameSource = relationship.getTargetEntity();
            if (maybeSameSource != source && !source.isSubentityOf(maybeSameSource)) {
                continue;
            }

            List<?> otherRels = relationship.getDbRelationships();
            if (reversed.size() != otherRels.size()) {
                continue;
            }

            int len = reversed.size();
            boolean relsMatch = true;
            for (int i = 0; i < len; i++) {
                if (otherRels.get(i) != reversed.get(i)) {
                    relsMatch = false;
                    break;
                }
            }

            if (relsMatch) {
                return relationship;
            }
        }

        return null;
    }

    /**
     * Creates a complimentary reverse relationship from target entity to the
     * source entity. A new relationship is created regardless of whether one
     * already exists. Returned relationship is not attached to the source
     * entity and has no name. Throws a {@link CayenneRuntimeException} if
     * reverse DbRelationship is not mapped.
     * 
     * @since 3.0
     */
    public ObjRelationship createReverseRelationship() {
        ObjRelationship reverse = new ObjRelationship();
        reverse.setSourceEntity(getTargetEntity());
        reverse.setTargetEntityName(getSourceEntity().getName());
        reverse.setDbRelationshipPath(getReverseDbRelationshipPath());
        return reverse;
    }

    /**
     * Returns an immutable list of underlying DbRelationships.
     */
    public List<DbRelationship> getDbRelationships() {
        refreshFromDeferredPath();
        return Collections.unmodifiableList(dbRelationships);
    }

    /**
     * Appends a DbRelationship to the existing list of DbRelationships.
     */
    public void addDbRelationship(DbRelationship dbRel) {
        refreshFromDeferredPath();
        if (dbRel.getName() == null) {
            throw new IllegalArgumentException("DbRelationship has no name");
        }

        // Adding a second is creating a flattened relationship.
        // Ensure that the new relationship properly continues
        // on the flattened path
        int numDbRelationships = dbRelationships.size();
        if (numDbRelationships > 0) {
            DbRelationship lastRel = dbRelationships.get(numDbRelationships - 1);
            if (!lastRel.getTargetEntityName().equals(dbRel.getSourceEntity().getName())) {
                throw new CayenneRuntimeException("Error adding db relationship %s to ObjRelationship %s"
                        + " because the source of the newly added relationship"
                        + " is not the target of the previous relationship in the chain.", dbRel, this);
            }
        }

        dbRelationships.add(dbRel);

        this.recalculateToManyValue();
    }

    /**
     * Removes the relationship <code>dbRel</code> from the list of
     * relationships.
     */
    public void removeDbRelationship(DbRelationship dbRel) {
        refreshFromDeferredPath();
        if (dbRelationships.remove(dbRel)) {
            this.recalculateToManyValue();
        }
    }

    public void clearDbRelationships() {
        deferredPath = null;
        this.dbRelationships.clear();
        this.readOnly = false;
        this.toMany = false;
    }

    /**
     * Returns a boolean indicating whether the presence of a non-null source
     * key(s) will not guarantee a presence of a target record. PK..FK
     * relationships are all optional, but there are other more subtle cases,
     * such as PK..PK, etc.
     * 
     * @since 3.0
     */
    public boolean isOptional() {
        if (isToMany() || isFlattened()) {
            return true;
        }

        // entities with qualifiers may result in filtering even existing target rows,
        // so such relationships are optional
        if (isQualifiedEntity(getTargetEntity())) {
            return true;
        }

        DbRelationship dbRelationship = getDbRelationships().get(0);

        // to-one mandatory relationships are either from non-PK or to master pk
        if (dbRelationship.isToPK()) {
            if (!dbRelationship.isFromPK()) {
                return false;
            }
            DbRelationship reverseRelationship = dbRelationship.getReverseRelationship();
            return !reverseRelationship.isToDependentPK();
        }

        return true;
    }

    /**
     * Returns true if the relationship is non-optional and target has no
     * subclasses.
     * 
     * @since 3.0
     */
    public boolean isSourceDefiningTargetPrecenseAndType(EntityResolver entityResolver) {

        if (isOptional()) {
            return false;
        }

        EntityInheritanceTree inheritanceTree = entityResolver.getInheritanceTree(getTargetEntityName());
        return inheritanceTree == null || inheritanceTree.getChildren().isEmpty();
    }

    /**
     * Returns true if the entity or its super entities have a limiting
     * qualifier.
     */
    private boolean isQualifiedEntity(ObjEntity entity) {
        if (entity.getDeclaredQualifier() != null) {
            return true;
        }

        entity = entity.getSuperEntity();

        if (entity == null) {
            return false;
        }

        return isQualifiedEntity(entity);
    }

    /**
     * Returns a boolean indicating whether modifying a target of such
     * relationship in any way will not change the underlying table row of the
     * source.
     * 
     * @since 1.1
     */
    public boolean isSourceIndependentFromTargetChange() {
        // note - call "isToPK" at the end of the chain, since
        // if it is to a dependent PK, we still should return true...
        return isToMany() || isFlattened() || isToDependentEntity() || !isToPK();
    }

    /**
     * Returns true if underlying DbRelationships point to dependent entity.
     */
    public boolean isToDependentEntity() {
        return (getDbRelationships().get(0)).isToDependentPK();
    }

    /**
     * Returns true if the underlying DbRelationships point to a at least one of
     * the columns of the target entity.
     * 
     * @since 1.1
     */
    public boolean isToPK() {
        return (getDbRelationships().get(0)).isToPK();
    }

    /**
     * Returns true if the relationship is a "flattened" relationship. A
     * relationship is considered "flattened" if it maps to more than one
     * DbRelationship. Such chain of DbRelationships is also called
     * "relationship path". All flattened relationships are at least readable,
     * but only those formed across a many-many join table (with no custom
     * attributes other than foreign keys) can be automatically written.
     * 
     * @see #isReadOnly
     * @return flag indicating if the relationship is flattened or not.
     */
    public boolean isFlattened() {
        return getDbRelationships().size() > 1;
    }

    /**
     * Returns true if the relationship is flattened, but is not of the single
     * case that can have automatic write support. Otherwise, it returns false.
     * 
     * @return flag indicating if the relationship is read only or not
     */
    public boolean isReadOnly() {
        refreshFromDeferredPath();
        return readOnly;
    }

    @Override
    public boolean isToMany() {
        refreshFromDeferredPath();
        recalculateToManyValue();
        return super.isToMany();
    }

    /**
     * Returns the deleteRule. The delete rule is a constant from the DeleteRule
     * class, and specifies what should happen to the destination object when
     * the source object is deleted.
     * 
     * @return int a constant from DeleteRule
     * @see #setDeleteRule
     */
    public int getDeleteRule() {
        return deleteRule;
    }

    /**
     * Sets the delete rule of the relationship.
     * 
     * @param value
     *            New delete rule. Must be one of the constants defined in
     *            DeleteRule class.
     * @see DeleteRule
     * @throws IllegalArgumentException
     *             if the value is not a valid delete rule.
     */
    public void setDeleteRule(int value) {
        if ((value != DeleteRule.CASCADE) && (value != DeleteRule.DENY) && (value != DeleteRule.NULLIFY)
                && (value != DeleteRule.NO_ACTION)) {

            throw new IllegalArgumentException("Delete rule value " + value
                    + " is not a constant from the DeleteRule class");
        }

        this.deleteRule = value;
    }

    /**
     * Returns whether this attribute should be used for locking.
     * 
     * @since 1.1
     */
    public boolean isUsedForLocking() {
        return usedForLocking;
    }

    /**
     * Sets whether this attribute should be used for locking.
     * 
     * @since 1.1
     */
    public void setUsedForLocking(boolean usedForLocking) {
        this.usedForLocking = usedForLocking;
    }

    /**
     * Returns a dot-separated path over mapped DbRelationships.
     *
     * @return path or {@code null} if no db relationships set
     *
     * @since 1.1
     * @since 5.0 returns {@link CayennePath} instead of a plain {@code String}
     */
    public CayennePath getDbRelationshipPath() {
        refreshFromDeferredPath();

        // build path on the fly
        if (getDbRelationships().isEmpty()) {
            return null;
        }

        CayennePath path = CayennePath.EMPTY_PATH;
        for (DbRelationship next : getDbRelationships()) {
            path = path.dot(next.getName());
        }

        return path;
    }

    /**
     * Returns a reversed dbRelationship path.
     * 
     * @since 1.2
     * @since 5.0 returns {@link CayennePath} instead of a plain {@code String}
     */
    public CayennePath getReverseDbRelationshipPath() throws ExpressionException {

        List<DbRelationship> relationships = getDbRelationships();
        if (relationships == null || relationships.isEmpty()) {
            return null;
        }

        CayennePath path = CayennePath.EMPTY_PATH;
        // iterate in reverse order
        ListIterator<DbRelationship> it = relationships.listIterator(relationships.size());
        while (it.hasPrevious()) {

            DbRelationship relationship = it.previous();
            DbRelationship reverse = relationship.getReverseRelationship();

            // another sanity check
            if (reverse == null) {
                throw new CayenneRuntimeException("No reverse relationship exist for %s", relationship);
            }
            path = path.dot(reverse.getName());
        }

        return path;
    }

    /**
     * Sets mapped DbRelationships as a dot-separated path.
     */
    public void setDbRelationshipPath(String relationshipPath) {
        setDbRelationshipPath(CayennePath.of(relationshipPath));
    }

    /**
     * Sets mapped DbRelationships as a dot-separated path.
     * @since 5.0
     */
    public void setDbRelationshipPath(CayennePath relationshipPath) {
        if (!Util.nullSafeEquals(getDbRelationshipPath(), relationshipPath)) {
            refreshFromPath(relationshipPath, false);
        }
    }

    /**
     * Sets relationship path, but does not trigger its conversion to {@code List<DbRelationship>}.
     * <br>
     * <b>NOTE</b>: this method is intended for internal usage, primarily datamap loading.
     *
     * @since 4.1 this method is public as it is used by new XML loaders
     */
    public void setDeferredDbRelationshipPath(String relationshipPath) {
        CayennePath newPath = CayennePath.of(relationshipPath);
        if (!Util.nullSafeEquals(getDbRelationshipPath(), newPath)) {
            deferredPath = newPath;
        }
    }

    /**
     * Loads path from "deferredPath" variable (if specified)
     */
    void refreshFromDeferredPath() {
        if (deferredPath != null) {
            synchronized(this) {
                // check if another thread just 
                // loaded path from deferredPath
                if (deferredPath != null){
                    refreshFromPath(deferredPath, true);
                    deferredPath = null;
                }
            }
        }
    }

    /**
     * Returns dot-separated path over DbRelationships, only including
     * components that have valid DbRelationships.
     */
    String getValidRelationshipPath() {
        CayennePath path = getDbRelationshipPath();
        if (path == null) {
            return null;
        }

        ObjEntity entity = getSourceEntity();
        if (entity == null) {
            throw new CayenneRuntimeException("Can't resolve DbRelationships, null source ObjEntity");
        }

        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            return null;
        }

        CayennePath validPath = CayennePath.EMPTY_PATH;
        try {
            for (PathComponent<DbAttribute, DbRelationship> pathComponent
                    : dbEntity.resolvePath(new ASTDbPath(path), Collections.emptyMap())) {
                validPath = validPath.dot(pathComponent.getName());
            }
        } catch (ExpressionException ignored) {
        }

        return validPath.value();
    }

    /**
     * Rebuild a list of relationships if String relationshipPath has changed.
     */
    final void refreshFromPath(CayennePath dbRelationshipPath, boolean stripInvalid) {
        // remove existing relationships
        dbRelationships.clear();

        if (dbRelationshipPath != null) {

            ObjEntity entity = getSourceEntity();
            if (entity == null) {
                throw new CayenneRuntimeException("Can't resolve DbRelationships, null source ObjEntity");
            }

            try {
                // add new relationships from path
                Iterator<CayenneMapEntry> it = entity.resolvePathComponents(new ASTDbPath(dbRelationshipPath));

                while (it.hasNext()) {
                    DbRelationship relationship = (DbRelationship) it.next();

                    dbRelationships.add(relationship);
                }
            } catch (ExpressionException ex) {
                if (!stripInvalid) {
                    throw ex;
                }
            }
        }

        recalculateToManyValue();
    }

    /**
     * Recalculates whether a relationship is toMany or toOne, based on the
     * underlying db relationships.
     */
    public void recalculateToManyValue() {
        // If there is a single toMany along the path, then the flattend
        // rel is toMany. If all are toOne, then the rel is toOne.
        // Simple (non-flattened) relationships form the degenerate case
        // taking the value of the single underlying dbrel.
        for (DbRelationship thisRel : this.dbRelationships) {
            if (thisRel.isToMany()) {
                this.toMany = true;
                return;
            }
        }

        this.toMany = false;
    }

    /**
     * Recalculates a new readonly value based on the underlying DbRelationships.
     * @deprecated since 4.2
     */
    @Deprecated(since = "4.2")
    public void recalculateReadOnlyValue() {
        // not flattened, always read/write
        if (dbRelationships.size() < 2) {
            this.readOnly = false;
            return;
        }

        // too long, can't handle this yet
        if (dbRelationships.size() > 2) {
            this.readOnly = true;
            return;
        }

        DbRelationship firstRel = dbRelationships.get(0);
        DbRelationship secondRel = dbRelationships.get(1);

        // many-to-many with single-step join
        // also 1..1..1 (CAY-1744) .. TODO all this should go away eventually
        // per CAY-1743
        if (!secondRel.isToMany()) {

            // allow modifications if the joins are from FKs
            if (!secondRel.isToPK()) {
                this.readOnly = true;
                return;
            }

            DbRelationship firstReverseRel = firstRel.getReverseRelationship();
            if (firstReverseRel == null || !firstReverseRel.isToPK()) {
                this.readOnly = true;
                return;
            }

            this.readOnly = false;
        } else {
            readOnly = true;
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName())
                .append("sourceEntityName", getSourceEntity().getName())
                .append("targetEntityName", getTargetEntityName())
                .append("dbRelationshipPath", getDbRelationshipPath().value()).toString();
    }

    /**
     * Returns the interface of collection mapped by a to-many relationship.
     * Returns null for to-one relationships. Default for to-many is
     * "java.util.List". Other possible values are "java.util.Set",
     * "java.util.Collection", "java.util.Map".
     * 
     * @since 3.0
     */
    public String getCollectionType() {
        if (collectionType != null) {
            return collectionType;
        }

        return isToMany() ? DEFAULT_COLLECTION_TYPE : null;
    }

    /**
     * @since 3.0
     */
    public void setCollectionType(String collectionType) {
        this.collectionType = collectionType;
    }

    /**
     * Returns a property name of a target entity used to create a relationship
     * map. Only has effect if collectionType property is set to
     * "java.util.Map".
     * 
     * @return The attribute name used for the map key or <code>null</code> if
     *         the default (PK) is used as the map key.
     * @since 3.0
     */
    public String getMapKey() {
        return mapKey;
    }

    /**
     * @since 3.0
     */
    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    @Override
    public boolean isMandatory() {
        refreshFromDeferredPath();
        if (dbRelationships.isEmpty()) {
            return false;
        }

        return dbRelationships.get(0).isMandatory();
    }
}
