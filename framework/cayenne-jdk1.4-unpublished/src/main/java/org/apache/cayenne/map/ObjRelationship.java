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

package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Describes navigational association between two Java classes, represented as source and
 * target ObjEntity. Maps to a path of DbRelationships.
 * 
 * @author Andrus Adamchik
 */
public class ObjRelationship extends Relationship implements EventListener {

    /**
     * Denotes a default type of to-many relationship collection which is a Java List.
     * 
     * @since 3.0
     */
    public static final String DEFAULT_COLLECTION_TYPE = "java.util.List";

    boolean readOnly;
    boolean dbRelationshipsRefreshNeeded = true;

    protected int deleteRule = DeleteRule.NO_ACTION;
    protected boolean usedForLocking;
    protected String dbRelationshipPath;

    protected List dbRelationships = new ArrayList();

    /**
     * Stores the type of collection mapped by a to-many relationship. Null for to-one
     * relationships.
     * 
     * @since 3.0
     */
    protected String collectionType;

    /**
     * Stores a property name of a target entity used to create a relationship map. Only
     * has effect if collectionType property is set to "java.util.Map".
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

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        ObjEntity source = (ObjEntity) getSourceEntity();
        if (source == null) {
            return;
        }

        encoder.print("<obj-relationship name=\"" + getName());
        encoder.print("\" source=\"" + source.getName());

        // looking up a target entity ensures that bogus names are not saved... whether
        // this is good or bad is debatable, as users may want to point to non-existent
        // entities on purpose.
        ObjEntity target = (ObjEntity) getTargetEntity();
        if (target != null) {
            encoder.print("\" target=\"" + target.getName());
        }

        if (getCollectionType() != null
                && !DEFAULT_COLLECTION_TYPE.equals(getCollectionType())) {
            encoder.print("\" collection-type=\"" + getCollectionType());
        }

        if (getMapKey() != null) {
            encoder.print("\" map-key=\"" + getMapKey());
        }

        if (isUsedForLocking()) {
            encoder.print("\" lock=\"true");
        }

        String deleteRule = DeleteRule.deleteRuleName(getDeleteRule());
        if (getDeleteRule() != DeleteRule.NO_ACTION && deleteRule != null) {
            encoder.print("\" deleteRule=\"" + deleteRule);
        }

        // quietly get rid of invalid path... this is not the best way of doing things,
        // but it is consistent across map package
        String path = getValidRelationshipPath();
        if (path != null) {
            encoder.print("\" db-relationship-path=\"" + path);
        }

        encoder.println("\"/>");
    }

    /**
     * Returns a target ObjEntity of this relationship. Entity is looked up in the parent
     * DataMap using "targetEntityName".
     */
    public Entity getTargetEntity() {
        String targetName = getTargetEntityName();
        if (targetName == null) {
            return null;
        }

        return getNonNullNamespace().getObjEntity(targetName);
    }

    /**
     * Returns the name of a complimentary relationship going in the opposite direction or
     * null if it doesn't exist.
     * 
     * @since 1.2
     */
    public String getReverseRelationshipName() {
        ObjRelationship reverse = getReverseRelationship();
        return (reverse != null) ? reverse.getName() : null;
    }

    /**
     * Returns a "complimentary" ObjRelationship going in the opposite direction. Returns
     * null if no such relationship is found.
     */
    public ObjRelationship getReverseRelationship() {
        // reverse the list
        List reversed = new ArrayList();
        Iterator rit = this.getDbRelationships().iterator();
        while (rit.hasNext()) {
            DbRelationship rel = (DbRelationship) rit.next();
            DbRelationship reverse = rel.getReverseRelationship();
            if (reverse == null)
                return null;

            reversed.add(0, reverse);
        }

        Entity target = this.getTargetEntity();
        if (target == null) {
            return null;
        }

        Entity src = this.getSourceEntity();

        Iterator it = target.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.getTargetEntity() != src)
                continue;

            List otherRels = rel.getDbRelationships();
            if (reversed.size() != otherRels.size())
                continue;

            int len = reversed.size();
            boolean relsMatch = true;
            for (int i = 0; i < len; i++) {
                if (otherRels.get(i) != reversed.get(i)) {
                    relsMatch = false;
                    break;
                }
            }

            if (relsMatch)
                return rel;
        }

        return null;
    }

    /**
     * Creates a complimentary reverse relationship from target entity to the source
     * entity. A new relationship is created regardless of whether one already exists.
     * Returned relationship is not attached to the source entity and has no name. Throws
     * a {@link CayenneRuntimeException} if reverse DbRelationship is not mapped.
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
    public List getDbRelationships() {
        refreshFromPath(true);
        return Collections.unmodifiableList(dbRelationships);
    }

    /**
     * Appends a DbRelationship to the existing list of DbRelationships.
     */
    public void addDbRelationship(DbRelationship dbRel) {
        refreshFromPath(true);

        if (dbRel.getName() == null) {
            throw new IllegalArgumentException("DbRelationship has no name");
        }

        // Adding a second is creating a flattened relationship.
        // Ensure that the new relationship properly continues
        // on the flattened path
        int numDbRelationships = dbRelationships.size();
        if (numDbRelationships > 0) {
            DbRelationship lastRel = (DbRelationship) dbRelationships
                    .get(numDbRelationships - 1);
            if (!lastRel.getTargetEntityName().equals(dbRel.getSourceEntity().getName())) {
                throw new CayenneRuntimeException("Error adding db relationship "
                        + dbRel
                        + " to ObjRelationship "
                        + this
                        + " because the source of the newly added relationship "
                        + "is not the target of the previous relationship "
                        + "in the chain");
            }
        }

        EventManager.getDefaultManager().addListener(
                this,
                "dbRelationshipDidChange",
                RelationshipEvent.class,
                DbRelationship.PROPERTY_DID_CHANGE,
                dbRel);

        dbRelationships.add(dbRel);

        if (dbRelationshipPath == null) {
            dbRelationshipPath = dbRel.getName();
        }
        else {
            dbRelationshipPath += '.' + dbRel.getName();
        }

        this.calculateReadOnlyValue();
        this.calculateToManyValue();
    }

    /**
     * Removes the relationship <code>dbRel</code> from the list of relationships.
     */
    public void removeDbRelationship(DbRelationship dbRel) {
        refreshFromPath(true);

        if (dbRelationships.remove(dbRel)) {

            if (dbRelationships.isEmpty()) {
                dbRelationshipPath = null;
            }
            else {
                StringBuffer path = new StringBuffer();

                for (int i = 0; i < dbRelationships.size(); i++) {
                    DbRelationship r = (DbRelationship) dbRelationships.get(i);
                    if (i > 0) {
                        path.append('.');
                    }

                    path.append(r.getName());
                }

                dbRelationshipPath = path.toString();
            }

            // Do not listen any more
            EventManager.getDefaultManager().removeListener(
                    this,
                    DbRelationship.PROPERTY_DID_CHANGE,
                    dbRel);

            this.calculateReadOnlyValue();
            this.calculateToManyValue();

        }
    }

    public void clearDbRelationships() {
        this.dbRelationshipPath = null;
        this.dbRelationshipsRefreshNeeded = false;
        this.dbRelationships.clear();
        this.readOnly = false;
        this.toMany = false;
    }

    /**
     * Returns a boolean indicating whether modifying a target of such relationship in any
     * way will not change the underlying table row of the source.
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
        return ((DbRelationship) getDbRelationships().get(0)).isToDependentPK();
    }

    /**
     * Returns true if the underlying DbRelationships point to a at least one of the
     * columns of the target entity.
     * 
     * @since 1.1
     */
    public boolean isToPK() {
        return ((DbRelationship) getDbRelationships().get(0)).isToPK();
    }

    /**
     * Returns true if the relationship is a "flattened" relationship. A relationship is
     * considered "flattened" if it maps to more than one DbRelationship. Such chain of
     * DbRelationships is also called "relationship path". All flattened relationships are
     * at least readable, but only those formed across a many-many join table (with no
     * custom attributes other than foreign keys) can be automatically written.
     * 
     * @see #isReadOnly
     * @return flag indicating if the relationship is flattened or not.
     */
    public boolean isFlattened() {
        return getDbRelationships().size() > 1;
    }

    /**
     * Returns true if the relationship is flattened, but is not of the single case that
     * can have automatic write support. Otherwise, it returns false.
     * 
     * @return flag indicating if the relationship is read only or not
     */
    public boolean isReadOnly() {
        refreshFromPath(true);
        return readOnly;
    }

    public boolean isToMany() {
        refreshFromPath(true);
        return super.isToMany();
    }

    /**
     * Returns the deleteRule. The delete rule is a constant from the DeleteRule class,
     * and specifies what should happen to the destination object when the source object
     * is deleted.
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
     * @param value New delete rule. Must be one of the constants defined in DeleteRule
     *            class.
     * @see DeleteRule
     * @throws IllegalArgumentException if the value is not a valid delete rule.
     */
    public void setDeleteRule(int value) {
        if ((value != DeleteRule.CASCADE)
                && (value != DeleteRule.DENY)
                && (value != DeleteRule.NULLIFY)
                && (value != DeleteRule.NO_ACTION)) {

            throw new IllegalArgumentException("Delete rule value "
                    + value
                    + " is not a constant from the DeleteRule class");
        }

        this.deleteRule = value;
    }

    public void dbRelationshipDidChange(RelationshipEvent event) {
        calculateToManyValue();
        calculateReadOnlyValue();
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
     * @since 1.1
     */
    public String getDbRelationshipPath() {
        if (dbRelationshipsRefreshNeeded) {
            return dbRelationshipPath;
        }
        else {
            // build path on the fly
            if (getDbRelationships().isEmpty()) {
                return null;
            }

            StringBuffer path = new StringBuffer();
            Iterator it = getDbRelationships().iterator();
            while (it.hasNext()) {
                DbRelationship next = (DbRelationship) it.next();
                path.append(next.getName());
                if (it.hasNext()) {
                    path.append(Entity.PATH_SEPARATOR);
                }
            }

            return path.toString();
        }
    }

    /**
     * Returns a reversed dbRelationship path.
     * 
     * @since 1.2
     */
    public String getReverseDbRelationshipPath() throws ExpressionException {

        List relationships = getDbRelationships();
        if (relationships == null || relationships.isEmpty()) {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        // iterate in reverse order
        ListIterator it = relationships.listIterator(relationships.size());
        while (it.hasPrevious()) {

            DbRelationship relationship = (DbRelationship) it.previous();
            DbRelationship reverse = relationship.getReverseRelationship();

            // another sanity check
            if (reverse == null) {
                throw new CayenneRuntimeException("No reverse relationship exist for "
                        + relationship);
            }

            if (buffer.length() > 0) {
                buffer.append(Entity.PATH_SEPARATOR);
            }

            buffer.append(reverse.getName());
        }

        return buffer.toString();
    }

    /**
     * Sets mapped DbRelationships as a dot-separated path.
     */
    public void setDbRelationshipPath(String relationshipPath) {
        if (!Util.nullSafeEquals(this.dbRelationshipPath, relationshipPath)) {
            this.dbRelationshipPath = relationshipPath;
            this.dbRelationshipsRefreshNeeded = true;
        }
    }

    /**
     * Returns dot-separated path over DbRelationships, only including components that
     * have valid DbRelationships.
     */
    String getValidRelationshipPath() {
        String path = getDbRelationshipPath();
        if (path == null) {
            return null;
        }

        ObjEntity entity = (ObjEntity) getSourceEntity();
        if (entity == null) {
            throw new CayenneRuntimeException(
                    "Can't resolve DbRelationships, null source ObjEntity");
        }

        StringBuffer validPath = new StringBuffer();

        try {
            Iterator it = entity.resolvePathComponents(new ASTDbPath(path));
            while (it.hasNext()) {
                DbRelationship relationship = (DbRelationship) it.next();

                if (validPath.length() > 0) {
                    validPath.append(Entity.PATH_SEPARATOR);
                }
                validPath.append(relationship.getName());
            }
        }
        catch (ExpressionException ex) {

        }

        return validPath.toString();
    }

    /**
     * Rebuild a list of relationships if String relationshipPath has changed.
     */
    final void refreshFromPath(boolean stripInvalid) {
        if (!dbRelationshipsRefreshNeeded) {
            return;
        }

        synchronized (this) {
            // check flag again in the synced block...
            if (!dbRelationshipsRefreshNeeded) {
                return;
            }

            EventManager eventLoop = EventManager.getDefaultManager();

            // remove existing relationships
            Iterator removeIt = dbRelationships.iterator();
            while (removeIt.hasNext()) {
                DbRelationship relationship = (DbRelationship) removeIt.next();
                eventLoop.removeListener(
                        this,
                        DbRelationship.PROPERTY_DID_CHANGE,
                        relationship);

                removeIt.remove();
            }

            if (this.dbRelationshipPath != null) {

                ObjEntity entity = (ObjEntity) getSourceEntity();
                if (entity == null) {
                    throw new CayenneRuntimeException(
                            "Can't resolve DbRelationships, null source ObjEntity");
                }

                try {
                    // add new relationships from path
                    Iterator it = entity.resolvePathComponents(new ASTDbPath(
                            this.dbRelationshipPath));

                    while (it.hasNext()) {
                        DbRelationship relationship = (DbRelationship) it.next();

                        // listen for changes
                        eventLoop.addListener(
                                this,
                                "dbRelationshipDidChange",
                                RelationshipEvent.class,
                                DbRelationship.PROPERTY_DID_CHANGE,
                                relationship);

                        dbRelationships.add(relationship);
                    }
                }
                catch (ExpressionException ex) {
                    if (!stripInvalid) {
                        throw ex;
                    }
                }
            }

            calculateToManyValue();
            calculateReadOnlyValue();

            dbRelationshipsRefreshNeeded = false;
        }
    }

    /**
     * Recalculates whether a relationship is toMany or toOne, based on the underlying db
     * relationships.
     */
    final void calculateToManyValue() {
        // If there is a single toMany along the path, then the flattend
        // rel is toMany. If all are toOne, then the rel is toOne.
        // Simple (non-flattened) relationships form the degenerate case
        // taking the value of the single underlying dbrel.
        Iterator dbRelIterator = this.dbRelationships.iterator();
        while (dbRelIterator.hasNext()) {
            DbRelationship thisRel = (DbRelationship) dbRelIterator.next();
            if (thisRel.isToMany()) {
                this.toMany = true;
                return;
            }
        }

        this.toMany = false;
    }

    /**
     * Recalculates a new readonly value based on the underlying DbRelationships.
     */
    final void calculateReadOnlyValue() {
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

        DbRelationship firstRel = (DbRelationship) dbRelationships.get(0);
        DbRelationship secondRel = (DbRelationship) dbRelationships.get(1);

        // only support many-to-many with single-step join
        if (!firstRel.isToMany() || secondRel.isToMany()) {
            this.readOnly = true;
            return;
        }

        DataMap map = firstRel.getTargetEntity().getDataMap();
        if (map == null) {
            throw new CayenneRuntimeException(this.getClass().getName()
                    + " could not obtain a DataMap for the destination of "
                    + firstRel.getName());
        }

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
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).append(
                "dbRelationshipPath",
                dbRelationshipPath).toString();
    }

    /**
     * Returns an ObjAttribute stripped of any server-side information, such as
     * DbAttribute mapping.
     * 
     * @since 1.2
     */
    public ObjRelationship getClientRelationship() {
        ObjRelationship reverse = getReverseRelationship();
        String reverseName = reverse != null ? reverse.getName() : null;

        ObjRelationship relationship = new ClientObjRelationship(
                getName(),
                reverseName,
                isToMany(),
                isReadOnly());

        relationship.setTargetEntityName(getTargetEntityName());
        relationship.setDeleteRule(getDeleteRule());
        relationship.setCollectionType(getCollectionType());

        // TODO: copy locking flag...

        return relationship;
    }

    /**
     * Returns the interface of collection mapped by a to-many relationship. Returns null
     * for to-one relationships. Default for to-many is "java.util.List". Other possible
     * values are "java.util.Set", "java.util.Collection", "java.util.Map".
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
     * Returns a property name of a target entity used to create a relationship map. Only
     * has effect if collectionType property is set to "java.util.Map".
     * 
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
}
