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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.parser.ASTDbPath;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * Describes navigational association between two Java classes, represented as source and
 * target ObjEntity. Maps to a path of DbRelationships.
 * 
 * @author Andrei Adamchik
 */
public class ObjRelationship extends Relationship implements EventListener {

    private static Logger logObj = Logger.getLogger(ObjRelationship.class);

    int deleteRule = DeleteRule.NO_ACTION;
    boolean readOnly;
    boolean dbRelationshipsRefreshNeeded = true;

    //  Whether optimstic locking should consider this relationship
    protected boolean usedForLocking;
    protected String dbRelationshipPath;

    List dbRelationships = new ArrayList();
    List dbRelationshipsRef = Collections.unmodifiableList(dbRelationships);

    public ObjRelationship() {
    }

    public ObjRelationship(String name) {
        super(name);
    }

    /**
     * @deprecated Since 1.1 use any other constructor.
     */
    public ObjRelationship(ObjEntity source, ObjEntity target, boolean toMany) {
        this();
        this.setSourceEntity(source);
        this.setTargetEntity(target);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        ObjEntity source = (ObjEntity) getSourceEntity();
        if (source == null) {
            logObj
                    .warn("No source entity, will not encode ObjRelationship: "
                            + getName());
            return;
        }

        encoder.print("<obj-relationship name=\"" + getName());
        encoder.print("\" source=\"" + source.getName());

        ObjEntity target = (ObjEntity) getTargetEntity();
        if (target != null) {
            encoder.print("\" target=\"" + target.getName());
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

    public Entity getTargetEntity() {
        String targetName = getTargetEntityName();
        if (targetName == null) {
            return null;
        }

        return getNonNullNamespace().getObjEntity(targetName);
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
     * Returns an immutable list of underlying DbRelationships.
     */
    public List getDbRelationships() {
        refreshFromPath(true);
        return dbRelationshipsRef;
    }

    /** Appends a DbRelationship to the existing list of DbRelationships. */
    public void addDbRelationship(DbRelationship dbRel) {
        refreshFromPath(true);

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

        this.calculateReadOnlyValue();
        this.calculateToManyValue();
    }

    /**
     * Removes the relationship <code>dbRel</code> from the list of relationships.
     */
    public void removeDbRelationship(DbRelationship dbRel) {
        refreshFromPath(true);

        dbRelationships.remove(dbRel);
        //Do not listen any more
        EventManager.getDefaultManager().removeListener(
                this,
                DbRelationship.PROPERTY_DID_CHANGE,
                dbRel);

        this.calculateReadOnlyValue();
        this.calculateToManyValue();
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
        this.calculateToManyValue();
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
        //If there is a single toMany along the path, then the flattend
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
        if(!secondRel.isToPK()) {
            this.readOnly = true;
            return;
        }
        
        DbRelationship firstReverseRel = firstRel.getReverseRelationship();
        if(firstReverseRel == null || !firstReverseRel.isToPK()) {
            this.readOnly = true;
            return;
        }

        this.readOnly = false;
    }
}