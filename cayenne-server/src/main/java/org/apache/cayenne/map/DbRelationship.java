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
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A DbRelationship is a descriptor of a database inter-table relationship based
 * on one or more primary key/foreign key pairs.
 */
public class DbRelationship extends Relationship<DbEntity, DbAttribute, DbRelationship> implements ConfigurationNode {

    // The columns through which the join is implemented.
    protected List<DbJoin> joins = new ArrayList<>(2);

    // Is relationship from source to target points to dependent primary
    // key (primary key column of destination table that is also a FK to the
    // source
    // column)
    protected boolean toDependentPK;

    public DbRelationship() {
        super();
    }

    public DbRelationship(String name) {
        super(name);
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitDbRelationship(this);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("db-relationship")
                .attribute("name", getName())
                .attribute("source", getSourceEntity().getName());

        if (getTargetEntityName() != null && getTargetEntity() != null) {
            encoder.attribute("target", getTargetEntityName());
        }

        encoder.attribute("toDependentPK", isToDependentPK() && isValidForDepPk());
        encoder.attribute("toMany", isToMany());

        encoder.nested(getJoins(), delegate);

        delegate.visitDbRelationship(this);
        encoder.end();
    }

    /**
     * Returns a target of this relationship. If relationship is not attached to
     * a DbEntity, and DbEntity doesn't have a namespace, and exception is
     * thrown.
     */
    @Override
    public DbEntity getTargetEntity() {
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
    public Collection<DbAttribute> getTargetAttributes() {
        return mapJoinsToAttributes(DbJoin::getTarget);
    }

    /**
     * Returns a Collection of source attributes.
     * 
     * @since 1.1
     */
    public Collection<DbAttribute> getSourceAttributes() {
        return mapJoinsToAttributes(DbJoin::getSource);
    }

    private Collection<DbAttribute> mapJoinsToAttributes(Function<DbJoin, DbAttribute> mapper) {
        if (joins.size() == 0) {
            return Collections.emptyList();
        }
        // fast path for common case
        if(joins.size() == 1) {
            return Collections.singletonList(mapper.apply(joins.get(0)));
        }
        Collection<DbAttribute> result = new ArrayList<>(joins.size());
        for(DbJoin join : joins) {
            result.add(mapper.apply(join));
        }
        return result;
    }

    /**
     * Creates a new relationship with the same set of joins, but going in the
     * opposite direction.
     * 
     * @since 1.0.5
     */
    public DbRelationship createReverseRelationship() {
        DbEntity targetEntity = getTargetEntity();

        DbRelationship reverse = new DbRelationship();
        reverse.setSourceEntity(targetEntity);
        reverse.setTargetEntityName(getSourceEntity().getName());

        // TODO: andrus 12/24/2007 - one more case to handle - set reverse
        // toDepPK = true
        // if this relationship toDepPK is false, but the entities are joined on
        // a PK...
        // on the other hand, these can still be two independent entities...

        if (isToDependentPK() && !toMany && joins.size() == targetEntity.getPrimaryKeys().size()) {
            reverse.setToMany(false);
        } else {
            reverse.setToMany(!toMany);
        }

        for (DbJoin join : joins) {
            DbJoin reverseJoin = join.createReverseJoin();
            reverseJoin.setRelationship(reverse);
            reverse.addJoin(reverseJoin);
        }

        return reverse;
    }

    /**
     * Returns DbRelationship that is the opposite of this DbRelationship. This
     * means a relationship from this target entity to this source entity with
     * the same join semantics. Returns null if no such relationship exists.
     */
    public DbRelationship getReverseRelationship() {
        DbEntity target = getTargetEntity();

        if (target == null) {
            return null;
        }

        DbEntity src = this.getSourceEntity();

        // special case - relationship to self with no joins...
        if (target == src && joins.size() == 0) {
            return null;
        }

        TestJoin testJoin = new TestJoin(this);
        for (DbRelationship rel : target.getRelationships()) {
            if (rel.getTargetEntity() != src) {
                continue;
            }

            List<DbJoin> otherJoins = rel.getJoins();
            if (otherJoins.size() != joins.size()) {
                continue;
            }

            boolean joinsMatch = true;
            for (DbJoin join : otherJoins) {
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
     * Returns true if the relationship points to at least one of the PK columns
     * of the target entity.
     * 
     * @since 1.1
     */
    public boolean isToPK() {
        for (DbJoin join : getJoins()) {

            DbAttribute target = join.getTarget();
            if (target == null) {
                return false;
            }

            if (!target.isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @since 3.0
     */
    public boolean isFromPK() {
        for (DbJoin join : getJoins()) {
            DbAttribute source = join.getSource();
            if (source == null) {
                return false;
            }

            if (source.isPrimaryKey()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if a method <code>isToDependentPK</code> of
     * reverse relationship of this relationship returns <code>true</code>.
     */
    public boolean isToMasterPK() {
        if (isToMany() || isToDependentPK()) {
            return false;
        }

        DbRelationship revRel = getReverseRelationship();
        return revRel != null && revRel.isToDependentPK();
    }
    
    /**
     * Returns a boolean indicating whether modifying a target of such
     * relationship in any way will not change the underlying table row of the
     * source.
     * 
     * @since 4.0
     */
    public boolean isSourceIndependentFromTargetChange() {
        // note - call "isToPK" at the end of the chain, since
        // if it is to a dependent PK, we still should return true...
        return isToMany() || isToDependentPK() || !isToPK();
    }

    /**
     * Returns <code>true</code> if relationship from source to target points to
     * dependent primary key. Dependent PK is a primary key column of the
     * destination table that is also a FK to the source column.
     */
    public boolean isToDependentPK() {
        return toDependentPK;
    }

    public void setToDependentPK(boolean toDependentPK) {
        this.toDependentPK = toDependentPK;
    }

    /**
     * @since 1.1
     */
    public boolean isValidForDepPk() {
        // handle case with no joins
        if (getJoins().size() == 0) {
            return false;
        }

        for (DbJoin join : getJoins()) {
            DbAttribute target = join.getTarget();
            DbAttribute source = join.getSource();

            if (target != null && !target.isPrimaryKey() || source != null && !source.isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a list of joins. List is returned by reference, so any
     * modifications of the list will affect this relationship.
     */
    public List<DbJoin> getJoins() {
        return joins;
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

    public void removeJoin(DbJoin join) {
        joins.remove(join);
    }

    public void removeAllJoins() {
        joins.clear();
    }

    public void setJoins(Collection<DbJoin> newJoins) {
        this.removeAllJoins();

        if (newJoins != null) {
            joins.addAll(newJoins);
        }
    }

    /**
     * Creates a snapshot of primary key attributes of a target object of this
     * relationship based on a snapshot of a source. Only "to-one" relationships
     * are supported. Returns null if relationship does not point to an object.
     * Throws CayenneRuntimeException if relationship is "to many" or if
     * snapshot is missing id components.
     */
    public Map<String, Object> targetPkSnapshotWithSrcSnapshot(Map<String, Object> srcSnapshot) {

        if (isToMany()) {
            throw new CayenneRuntimeException("Only 'to one' relationships support this method.");
        }

        Map<String, Object> idMap;

        int numJoins = joins.size();
        int foundNulls = 0;

        // optimize for the most common single column join
        if (numJoins == 1) {
            DbJoin join = joins.get(0);
            Object val = srcSnapshot.get(join.getSourceName());
            if (val == null) {
                foundNulls++;
                idMap = Collections.emptyMap();
            } else {
                idMap = Collections.singletonMap(join.getTargetName(), val);
            }
        }
        // handle generic case: numJoins > 1
        else {
            idMap = new HashMap<>(numJoins * 2);
            for (DbJoin join : joins) {
                DbAttribute source = join.getSource();
                Object val = srcSnapshot.get(join.getSourceName());

                if (val == null) {

                    // some keys may be nulls and some not in case of multi-key
                    // relationships where PK and FK partially overlap (see
                    // CAY-284)
                    if (!source.isMandatory()) {
                        return null;
                    }

                    foundNulls++;
                } else {
                    idMap.put(join.getTargetName(), val);
                }
            }
        }

        if (foundNulls == 0) {
            return idMap;
        } else if (foundNulls == numJoins) {
            return null;
        } else {
            throw new CayenneRuntimeException("Some parts of FK are missing in snapshot, relationship: %s", this);
        }
    }

    /**
     * Common code to srcSnapshotWithTargetSnapshot. Both are functionally the
     * same, except for the name, and whether they operate on a toMany or a
     * toOne.
     */
    private Map<String, Object> srcSnapshotWithTargetSnapshot(Map<String, Object> targetSnapshot) {
        int len = joins.size();

        // optimize for the most common single column join
        if (len == 1) {
            DbJoin join = joins.get(0);
            Object val = targetSnapshot.get(join.getTargetName());
            return Collections.singletonMap(join.getSourceName(), val);
        }

        // general case
        Map<String, Object> idMap = new HashMap<>(len * 2);
        for (DbJoin join : joins) {
            Object val = targetSnapshot.get(join.getTargetName());
            idMap.put(join.getSourceName(), val);
        }

        return idMap;
    }

    /**
     * Creates a snapshot of foreign key attributes of a source object of this
     * relationship based on a snapshot of a target. Only "to-one" relationships
     * are supported. Throws CayenneRuntimeException if relationship is
     * "to many".
     */
    public Map<String, Object> srcFkSnapshotWithTargetSnapshot(Map<String, Object> targetSnapshot) {

        if (isToMany()) {
            throw new CayenneRuntimeException("Only 'to one' relationships support this method.");
        }

        return srcSnapshotWithTargetSnapshot(targetSnapshot);
    }

    /**
     * Creates a snapshot of primary key attributes of a source object of this
     * relationship based on a snapshot of a target. Only "to-many"
     * relationships are supported. Throws CayenneRuntimeException if
     * relationship is "to one".
     */
    public Map<String, Object> srcPkSnapshotWithTargetSnapshot(Map<String, Object> targetSnapshot) {
        if (!isToMany()) {
            throw new CayenneRuntimeException("Only 'to many' relationships support this method.");
        }

        return srcSnapshotWithTargetSnapshot(targetSnapshot);
    }

    /**
     * Sets relationship multiplicity.
     */
    public void setToMany(boolean toMany) {
        this.toMany = toMany;
    }

    @Override
    public boolean isMandatory() {
        for (DbJoin join : getJoins()) {
            if (join.getSource().isMandatory()) {
                return true;
            }
        }

        return false;
    }

    // a join used for comparison
    static final class TestJoin extends DbJoin {

        TestJoin(DbRelationship relationship) {
            super(relationship);
        }

        @Override
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
            return j.relationship == this.relationship && Util.nullSafeEquals(j.sourceName, this.sourceName)
                    && Util.nullSafeEquals(j.targetName, this.targetName);
        }
    }

    public String toString() {
        StringBuilder res = new StringBuilder("Db Relationship : ");
        res.append(toMany ? "toMany" : "toOne ");

        String sourceEntityName = getSourceEntityName();
        for (DbJoin join : joins) {
            res.append(" (").append(sourceEntityName).append(".").append(join.getSourceName()).append(", ")
                    .append(targetEntityName).append(".").append(join.getTargetName()).append(")");
        }
        return res.toString();
    }

    public String getSourceEntityName() {
        if (this.sourceEntity == null) {
            return null;
        }
        return this.sourceEntity.name;
    }
}
