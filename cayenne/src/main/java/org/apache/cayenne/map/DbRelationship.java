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

import org.apache.cayenne.project.ProjectNode;
import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A DbRelationship is a descriptor of a database inter-table relationship based
 * on one or more primary key/foreign key pairs.
 */
public class DbRelationship extends Relationship<DbEntity, DbAttribute, DbRelationship> implements ProjectNode {

    // The columns through which the join is implemented.
    protected List<DbJoin> joins = new ArrayList<>(2);

    // true if the source join columns hold the FK that references the target join columns
    protected boolean foreignKey;

    public DbRelationship() {
        super();
    }

    public DbRelationship(String name) {
        super(name);
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ProjectNodeVisitor<T> visitor) {
        return visitor.visitDbRelationship(this);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder, ProjectNodeVisitor<?> delegate) {
        encoder.start("dbRelationship")
                .attribute("name", getName())
                .attribute("source", getSourceEntity().getName());

        if (getTargetEntityName() != null && getTargetEntity() != null) {
            encoder.attribute("target", getTargetEntityName());
        }

        encoder.attribute("foreignKey", isForeignKey());
        encoder.attribute("toMany", isToMany());

        // skip empty joins that would otherwise be saved as useless "<dbAttributePair/>" tags
        encoder.nested(getJoins().stream()
                .filter(j -> j.getSourceName() != null || j.getTargetName() != null)
                .toList(), delegate);

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
        if (joins.isEmpty()) {
            return Collections.emptyList();
        }
        // fast path for common case
        if(joins.size() == 1) {
            return Collections.singletonList(mapper.apply(joins.getFirst()));
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

        // a to-many never holds the FK, so the reverse holds it exactly when this side doesn't. The reverse of an FK is
        // to-one only when the FK columns are the whole source PK, as then a target row has at most one source row
        reverse.setForeignKey(!foreignKey);
        reverse.setToMany(foreignKey && !isSourcePK());

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
        if (target == src && joins.isEmpty()) {
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
     * Returns true if the joins of this relationship cover the entire primary key of the target entity, so that the
     * source join columns hold a complete target PK. A relationship to a subset of a composite PK, such as a to-many
     * from a master to a dependent entity keyed by the master PK plus a discriminator, would return false.
     *
     * @since 6.0
     */
    public boolean isToPrimaryKey() {
        // a to-many can't be to the full target PK; cheap check before the entity lookup
        if (toMany || joins.isEmpty()) {
            return false;
        }

        DbEntity target = getTargetEntity();
        if (target == null) {
            return false;
        }

        Collection<DbAttribute> pk = target.getPrimaryKeys();
        if (pk.size() != joins.size()) {
            return false;
        }

        if (joins.size() == 1) {
            DbAttribute joinTarget = joins.getFirst().getTarget();
            return joinTarget != null && pk.contains(joinTarget);
        }

        // sizes match, so every PK column is covered if each join targets a distinct PK column
        Set<DbAttribute> covered = HashSet.newHashSet(joins.size());
        for (DbJoin join : joins) {
            DbAttribute joinTarget = join.getTarget();
            if (joinTarget == null || !pk.contains(joinTarget) || !covered.add(joinTarget)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the source of this relationship holds the foreign key, i.e. the source join columns reference
     * the target join columns. A to-many never holds the FK, and the to-one of a one-to-many always does. For a
     * one-to-one joined PK to PK, this flag is the only thing that tells the two sides apart.
     *
     * @since 6.0
     */
    public boolean isForeignKey() {
        return foreignKey;
    }

    /**
     * @since 6.0
     */
    public void setForeignKey(boolean foreignKey) {
        this.foreignKey = foreignKey;
    }

    /**
     * Returns true if this is a reflexive relationship: the source holds the {@link #isForeignKey() foreign key},
     * and it references the primary key of the source's own entity, e.g. a parent reference in a tree table.
     *
     * @since 6.0
     */
    public boolean isReflexive() {
        if (!foreignKey || joins.isEmpty() || getTargetEntity() != getSourceEntity()) {
            return false;
        }

        for (DbJoin join : joins) {
            DbAttribute target = join.getTarget();
            if (target != null && target.isPrimaryKey()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this is an identifying relationship: the source holds the {@link #isForeignKey() foreign key},
     * and the FK columns are a part of the source primary key, so a source row can't exist without its target row.
     * A join table of a many-to-many references each side this way, and so does a detail keyed by its master PK plus
     * a sequence number.
     *
     * @since 6.0
     */
    public boolean isIdentifying() {
        if (!foreignKey || joins.isEmpty()) {
            return false;
        }

        for (DbJoin join : joins) {
            DbAttribute source = join.getSource();
            if (source == null || !source.isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }

    // true if the join source columns are exactly the PK of the source entity
    private boolean isSourcePK() {
        List<DbAttribute> pk = getSourceEntity().getPrimaryKeys();
        if (joins.size() != pk.size()) {
            return false;
        }
        for (DbJoin join : joins) {
            if (!pk.contains(join.getSource())) {
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
     * Sets relationship multiplicity.
     */
    public void setToMany(boolean toMany) {
        this.toMany = toMany;
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

            if (!(o instanceof DbJoin j)) {
                return false;
            }

            return j.relationship == this.relationship && Objects.equals(j.sourceName, this.sourceName)
                    && Objects.equals(j.targetName, this.targetName);
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
