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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * A DbRelationship is a descriptor of a database inter-table relationship
 * based on one or more primary key/foreign key pairs.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbRelationship extends Relationship {
    //DbRelationship events
    public static final EventSubject PROPERTY_DID_CHANGE =
        EventSubject.getSubject(DbRelationship.class, "PropertyDidChange");

    // The columns through which the join is implemented.
    protected List joins = new ArrayList();

    // Is relationship from source to target points to dependent primary
    //  key (primary key column of destination table that is also a FK to the source column)
    protected boolean toDependentPK;

    public DbRelationship() {
        super();
    }

    public DbRelationship(String name) {
        super(name);
    }

    /**
     * @deprecated Since 1.1 use any other constructor.
     */
    public DbRelationship(DbEntity src, DbEntity target, DbAttributePair join) {
        setSourceEntity(src);
        setTargetEntity(target);
        addJoin(join.toDbJoin(this));
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<db-relationship name=\"");
        encoder.print(Util.encodeXmlAttribute(getName()));
        encoder.print("\" source=\"");
        encoder.print(getSourceEntity().getName());

        if (getTargetEntityName() != null && getTargetEntity() != null) {
            encoder.print("\" target=\"");
            encoder.print(getTargetEntityName());
        }

        if (isToDependentPK() && isValidForDepPk()) {
            encoder.print("\" toDependentPK=\"true");
        }

        encoder.print("\" toMany=\"");
        encoder.print(isToMany());
        encoder.println("\">");

        encoder.indent(1);
        encoder.print(getJoins());
        encoder.indent(-1);

        encoder.println("</db-relationship>");
    }

    /**
     * Returns a target of this relationship. If relationship is not attached
     * to a DbEntity, and DbEntity doesn't have a namcespace, and exception
     * is thrown.
     */
    public Entity getTargetEntity() {
        String targetName = getTargetEntityName();
        if (targetName == null) {
            return null;
        }

        return getNonNullNamespace().getDbEntity(targetName);
    }

    /**
     * Returns a Collection of target attributes.
     * 
     * @since 1.1
     */
    public Collection getTargetAttributes() {
        if (joins.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        return CollectionUtils.collect(joins, JoinTransformers.targetExtractor);
    }

    /**
     * Returns a Collection of target attributes.
     * 
     * @since 1.1
     */
    public Collection getSourceAttributes() {
        if (joins.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        return CollectionUtils.collect(joins, JoinTransformers.sourceExtractor);
    }

    /**
     * Creates a new relationship with the same set of joins,
     * but going in the opposite direction.
     * 
     * @since 1.0.5
     */
    public DbRelationship createReverseRelationship() {
        DbRelationship reverse = new DbRelationship();
        reverse.setSourceEntity(getTargetEntity());
        reverse.setTargetEntityName(getSourceEntity().getName());

        // TODO: must set toDepPK correctly
        //       must set toMany correctly

        reverse.setToMany(!toMany);

        Iterator it = joins.iterator();
        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
            DbJoin reverseJoin = join.createReverseJoin();
            reverseJoin.setRelationship(reverse);
            reverse.addJoin(reverseJoin);
        }

        return reverse;
    }

    /** 
     * Returns DbRelationship that is the opposite of this DbRelationship.
     * This means a relationship from this target entity to this source entity with the same
     * join semantics. Returns null if no such relationship exists. 
     */
    public DbRelationship getReverseRelationship() {
        Entity target = this.getTargetEntity();

        if (target == null) {
            return null;
        }

        Entity src = this.getSourceEntity();
        
        // special case - relationship to self with no joins...
        if(target == src && joins.size() == 0) {
            return null;
        }
        
        TestJoin testJoin = new TestJoin(this);
        Iterator it = target.getRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship rel = (DbRelationship) it.next();
            if (rel.getTargetEntity() != src)
                continue;

            List otherJoins = rel.getJoins();
            if (otherJoins.size() != joins.size()) {
                continue;
            }

            Iterator jit = otherJoins.iterator();
            boolean joinsMatch = true;
            while (jit.hasNext()) {
                DbJoin join = (DbJoin) jit.next();

                // flip join and try to find similar
                testJoin.setSourceName(join.getTargetName());
                testJoin.setTargetName(join.getSourceName());
                if (!joins.contains(testJoin)) {
                    joinsMatch = false;
                    break;
                }
            }

            if (joinsMatch) {
                return rel;
            }
        }
        
        return null;
    }

    /**
     * Returns true if the relationship points to at least one of the PK 
     * columns of the target entity.
     * 
     * @since 1.1
     */
    public boolean isToPK() {
        Iterator it = getJoins().iterator();
        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
            if(join.getTarget() == null) {
                return false;    
            }
            
            if (join.getTarget().isPrimaryKey()) {
                return true;
            }
        }

        return false;
    }

    /** 
     * Returns <code>true</code> if a method <code>isToDependentPK</code> 
     * of reverse relationship of this relationship returns <code>true</code>. 
     */
    public boolean isToMasterPK() {
        if (isToMany() || isToDependentPK()) {
            return false;
        }

        DbRelationship revRel = getReverseRelationship();
        return (revRel != null) ? revRel.isToDependentPK() : false;
    }

    /** 
     * Returns <code>true</code> if relationship from source to 
     * target points to dependent primary key. Dependent PK is
     * a primary key column of the destination table that is 
     * also a FK to the source column. 
     */
    public boolean isToDependentPK() {
        return toDependentPK;
    }

    public void setToDependentPK(boolean flag) {
        toDependentPK = flag;
    }

    /**
     * @since 1.1
     */
    public boolean isValidForDepPk() {
        Iterator it = getJoins().iterator();
        // handle case with no joins
        if (!it.hasNext()) {
            return false;
        }

        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
            DbAttribute target = join.getTarget();
            DbAttribute source = join.getSource();

            if ((target != null && !target.isPrimaryKey())
                || (source != null && !source.isPrimaryKey())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a list of joins. List is returned by reference, so 
     * any modifications of the list will affect this relationship.
     */
    public List getJoins() {
        return joins;
    }

    /** 
     * Adds a join.
     * 
     * @deprecated Since 1.1 use {@link #addJoin(DbJoin)}
     */
    public void addJoin(DbAttributePair join) {
        if (join != null) {
            addJoin(join.toDbJoin(this));
        }
    }

    /** 
     * Adds a join.
     * 
     * @since 1.1
     */
    public void addJoin(DbJoin join) {
        if (join != null) {
            joins.add(join);
        }
    }

    /** 
     * Removes a join.
     * 
     * @deprecated Since 1.1 use {@link #removeJoin(DbJoin)}
     */
    public void removeJoin(DbAttributePair join) {
        if (join != null) {
            joins.remove(join.toDbJoin(this));
        }
    }

    public void removeJoin(DbJoin join) {
        joins.remove(join);
    }

    public void removeAllJoins() {
        joins.clear();
    }

    public void setJoins(Collection newJoins) {
        this.removeAllJoins();
        
        if (newJoins != null) {
            joins.addAll(newJoins);
        }
    }

    /** 
     * Creates a snapshot of primary key attributes of a target
     * object of this relationship based on a snapshot of a source.
     * Only "to-one" relationships are supported.
     * Returns null if relationship does not point to an object.
     * Throws CayenneRuntimeException if relationship is "to many" or
     * if snapshot is missing id components. 
     */
    public Map targetPkSnapshotWithSrcSnapshot(Map srcSnapshot) {

        if (isToMany()) {
            throw new CayenneRuntimeException("Only 'to one' relationships support this method.");
        }

        Map idMap;

        int numJoins = joins.size();
        int foundNulls = 0;

        // optimize for the most common single column join
        if (numJoins == 1) {
            DbJoin join = (DbJoin) joins.get(0);
            Object val = srcSnapshot.get(join.getSourceName());
            if (val == null) {
                foundNulls++;
                idMap = Collections.EMPTY_MAP;
            }
            else {
                idMap = Collections.singletonMap(join.getTargetName(), val);
            }
        }
        // handle generic case: numJoins > 1
        else {
            idMap = new HashMap(numJoins * 2);
            for (int i = 0; i < numJoins; i++) {
                DbJoin join = (DbJoin) joins.get(i);
                Object val = srcSnapshot.get(join.getSourceName());
                if (val == null) {
                    foundNulls++;
                }
                else {
                    idMap.put(join.getTargetName(), val);
                }
            }
        }

        if (foundNulls == 0) {
            return idMap;
        }
        else if (foundNulls == numJoins) {
            return null;
        }
        else {
            throw new CayenneRuntimeException("Some parts of FK are missing in snapshot,"
                    + " relationship: "
                    + this);
        }
    }

    /** 
     * Common code to srcSnapshotWithTargetSnapshot. Both are functionally the
     * same, except for the name, and whether they operate on a toMany or a toOne.
     */
    private Map srcSnapshotWithTargetSnapshot(Map targetSnapshot) {
        Map idMap;
        int len = joins.size();

        // optimize for the most common single column join
        if (len == 1) {
            DbJoin join = (DbJoin) joins.get(0);
            Object val = targetSnapshot.get(join.getTargetName());
            if (val == null) {
                throw new CayenneRuntimeException(
                        "Some parts of FK are missing in snapshot, join: " + join);
            }
            else {
                idMap = Collections.singletonMap(join.getSourceName(), val);
            }
        }
        // general case
        else {
            idMap = new HashMap(len * 2);
            for (int i = 0; i < len; i++) {
                DbJoin join = (DbJoin) joins.get(i);
                Object val = targetSnapshot.get(join.getTargetName());
                if (val == null) {
                    throw new CayenneRuntimeException(
                            "Some parts of FK are missing in snapshot, join: " + join);
                }
                else {
                    idMap.put(join.getSourceName(), val);
                }
            }
        }

        return idMap;
    }

    /** 
     * Creates a snapshot of foreign key attributes of a source
     * object of this relationship based on a snapshot of a target.
     * Only "to-one" relationships are supported.
     * Throws CayenneRuntimeException if relationship is "to many" or
     * if snapshot is missing id components.
     */
    public Map srcFkSnapshotWithTargetSnapshot(Map targetSnapshot) {

        if (isToMany())
            throw new CayenneRuntimeException("Only 'to one' relationships support this method.");
        return srcSnapshotWithTargetSnapshot(targetSnapshot);
    }

    /** 
     * Creates a snapshot of primary key attributes of a source
     * object of this relationship based on a snapshot of a target.
     * Only "to-many" relationships are supported.
     * Throws CayenneRuntimeException if relationship is "to one" or
     * if snapshot is missing id components.
     */
    public Map srcPkSnapshotWithTargetSnapshot(Map targetSnapshot) {
        if (!isToMany())
            throw new CayenneRuntimeException("Only 'to many' relationships support this method.");
        return srcSnapshotWithTargetSnapshot(targetSnapshot);
    }

    /** 
     * Sets relationship multiplicity. 
     */
    public void setToMany(boolean toMany) {
        this.toMany = toMany;
        this.firePropertyDidChange();
    }

    protected void firePropertyDidChange() {
        RelationshipEvent event =
            new RelationshipEvent(this, this, this.getSourceEntity());
        EventManager.getDefaultManager().postEvent(event, PROPERTY_DID_CHANGE);
    }

    final static class JoinTransformers {
        static final Transformer targetExtractor = new Transformer() {
            public Object transform(Object input) {
                return (input instanceof DbJoin) ? ((DbJoin) input).getTarget() : input;
            }
        };

        static final Transformer sourceExtractor = new Transformer() {
            public Object transform(Object input) {
                return (input instanceof DbJoin) ? ((DbJoin) input).getSource() : input;
            }
        };
    }
    
    // a join used for comparison
    final static class TestJoin extends DbJoin {
        TestJoin(DbRelationship relationship) {
            super(relationship);
        }
        
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (o == this) {
                return true;
            }

            if (!(o instanceof DbJoin)) {
                return false;
            }

            DbJoin j = (DbJoin) o;
            return j.relationship == this.relationship
                    && Util.nullSafeEquals(j.sourceName, this.sourceName)
                    && Util.nullSafeEquals(j.targetName, this.targetName);
        }
    }
}
