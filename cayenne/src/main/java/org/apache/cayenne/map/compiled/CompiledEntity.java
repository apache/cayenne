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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FullObjectExp;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.reflect.DefaultConstructor;
import org.apache.cayenne.reflect.PropertyException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A compiled persistent entity: the mapping of a persistent class to a table, with the runtime machinery to create,
 * read and write its objects. Every fact about the entity is resolved when the mapping is compiled, so nothing is
 * looked up at runtime.
 *
 * @since 6.0
 */
public final class CompiledEntity {

    public static final int LOCK_TYPE_NONE = 0;
    public static final int LOCK_TYPE_OPTIMISTIC = 1;

    private final String name;
    private final CompiledDataMap map;
    private final CompiledTable table;
    private final CompiledEntity superEntity;
    private final Class<?> objectClass;
    private final DefaultConstructor<?> constructor;
    private final boolean abstractEntity;
    private final boolean readOnly;
    private final boolean generic;
    private final int lockType;
    private final int declaredLockType;
    private final Expression declaredQualifier;

    // written once by the compiler, in the link step
    List<CompiledEntity> subEntities;
    List<CompiledEntity> allSubEntities;
    Map<String, CompiledEntity> subEntitiesByName;
    Map<String, CompiledAttribute> attributes;
    Map<String, CompiledAttribute> declaredAttributes;
    Map<String, CompiledEmbeddedAttribute> embeddedAttributes;
    Map<String, CompiledRelationship> relationships;
    Map<String, CompiledRelationship> declaredRelationships;
    List<CompiledAttribute> idAttributes;
    List<CompiledAttribute> primaryKeyAttributes;
    List<CompiledRelationship> mapRelationships;
    List<CompiledAttribute> allAttributes;
    List<CompiledRelationship> allRelationships;
    Expression qualifier;
    Expression tableQualifier;
    List<CompiledAttribute> discriminatorAttributes;
    Map<CompiledAttribute, Object> discriminatorValues;
    List<CompiledTable> rootTables;
    Map<CayennePath, CompiledFlattenedTable> flattenedTables;
    DataRowLayout snapshotLayout;

    // a marker instance, see CompiledMapping
    CompiledEntity() {
        this(null, null, null, null, null, false, false, false, LOCK_TYPE_NONE, LOCK_TYPE_NONE, null);
    }

    CompiledEntity(
            CompiledDataMap map,
            String name,
            CompiledTable table,
            CompiledEntity superEntity,
            Class<?> objectClass,
            boolean abstractEntity,
            boolean readOnly,
            boolean generic,
            int lockType,
            int declaredLockType,
            Expression declaredQualifier) {

        this.name = name;
        this.map = map;
        this.table = table;
        this.superEntity = superEntity;
        this.objectClass = objectClass;
        this.constructor = objectClass != null ? new DefaultConstructor<>(objectClass) : null;
        this.abstractEntity = abstractEntity;
        this.readOnly = readOnly;
        this.generic = generic;
        this.lockType = lockType;
        this.declaredLockType = declaredLockType;
        this.declaredQualifier = declaredQualifier;

        this.subEntities = List.of();
        this.allSubEntities = List.of();
        this.subEntitiesByName = Map.of();
        this.attributes = Map.of();
        this.declaredAttributes = Map.of();
        this.embeddedAttributes = Map.of();
        this.relationships = Map.of();
        this.declaredRelationships = Map.of();
        this.idAttributes = List.of();
        this.primaryKeyAttributes = List.of();
        this.mapRelationships = List.of();
        this.allAttributes = List.of();
        this.allRelationships = List.of();
        this.discriminatorAttributes = List.of();
        this.discriminatorValues = Map.of();
        this.rootTables = List.of();
        this.flattenedTables = Map.of();
    }

    public String name() {
        return name;
    }

    public CompiledDataMap map() {
        return map;
    }

    /**
     * Returns the table of the entity, or null for an abstract entity that has none.
     */
    public CompiledTable table() {
        return table;
    }

    /**
     * Returns the super-entity in the inheritance hierarchy, or null for a root entity.
     */
    public CompiledEntity superEntity() {
        return superEntity;
    }

    /**
     * Returns the direct sub-entities of this entity.
     */
    public List<CompiledEntity> subEntities() {
        return subEntities;
    }

    /**
     * Returns this entity followed by all its sub-entities, depth-first.
     */
    public List<CompiledEntity> allSubEntities() {
        return allSubEntities;
    }

    public boolean hasSubEntities() {
        return !subEntities.isEmpty();
    }

    /**
     * Returns the sub-entity of this entity with the given name, or this entity if the name is not of a sub-entity.
     */
    public CompiledEntity subEntity(String entityName) {
        CompiledEntity subEntity = subEntitiesByName.get(entityName);
        return subEntity != null ? subEntity : this;
    }

    /**
     * Returns true if this entity directly or indirectly inherits from the given entity.
     */
    public boolean isSubEntityOf(CompiledEntity entity) {
        for (CompiledEntity e = superEntity; e != null; e = e.superEntity) {
            if (e == entity) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the persistent class, or null if the mapping was compiled without class binding, or the class could not
     * be loaded.
     */
    public Class<?> objectClass() {
        return objectClass;
    }

    /**
     * Creates a new instance of the persistent class.
     */
    public Object createObject() {
        if (constructor == null) {
            throw new CayenneRuntimeException("The class of entity '%s' is not bound or could not be loaded", name);
        }
        return constructor.newInstance();
    }

    /**
     * Returns true if the object is not fully resolved.
     */
    public boolean isFault(Object object) {
        return object instanceof Persistent persistent && persistent.getPersistenceState() == PersistenceState.HOLLOW;
    }

    public boolean isAbstract() {
        return abstractEntity;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns true if the entity is not mapped to a unique Java class: it has no class, or a class shared with the
     * other generic entities.
     */
    public boolean isGeneric() {
        return generic;
    }

    /**
     * Returns the lock type, {@link #LOCK_TYPE_NONE} or {@link #LOCK_TYPE_OPTIMISTIC}, inherited from the
     * super-entities if not declared here.
     */
    public int lockType() {
        return lockType;
    }

    /**
     * Returns the lock type declared on this entity itself, or {@link #LOCK_TYPE_NONE} if none is declared here.
     */
    public int declaredLockType() {
        return declaredLockType;
    }

    /**
     * Returns the qualifier declared on this entity, not including those of the super-entities, or null.
     */
    public Expression declaredQualifier() {
        return declaredQualifier;
    }

    /**
     * Returns the qualifier matching the objects of this entity and all its sub-entities, or null if there is no
     * filtering to do.
     */
    public Expression qualifier() {
        return qualifier;
    }

    /**
     * Returns the {@link #declaredQualifier() declared qualifier} expressed in DB paths, or null.
     */
    public Expression tableQualifier() {
        return tableQualifier;
    }

    /**
     * Returns the attributes that the inheritance qualifiers of this entity, its super-entities and sub-entities
     * refer to. A DB column with no attribute is represented by a {@link CompiledAttribute#isSynthetic()
     * synthetic} attribute.
     */
    public List<CompiledAttribute> discriminatorAttributes() {
        return discriminatorAttributes;
    }

    /**
     * Returns the attributes of this entity, its own and inherited, sorted by name.
     */
    public Collection<CompiledAttribute> attributes() {
        return attributes.values();
    }

    /**
     * Returns the attributes declared by this entity, excluding the inherited ones.
     */
    public Collection<CompiledAttribute> declaredAttributes() {
        return declaredAttributes.values();
    }

    /**
     * Returns an attribute of this entity, own or inherited, or null if there is none. The attributes of an
     * embeddable are named "embeddedAttribute.embeddableAttribute".
     */
    public CompiledAttribute attribute(String name) {
        return attributes.get(name);
    }

    /**
     * Returns the attributes of this entity holding embeddable objects, by name.
     */
    public Collection<CompiledEmbeddedAttribute> embeddedAttributes() {
        return embeddedAttributes.values();
    }

    public CompiledEmbeddedAttribute embeddedAttribute(String name) {
        return embeddedAttributes.get(name);
    }

    /**
     * Returns the relationships of this entity, its own and inherited, sorted by name.
     */
    public Collection<CompiledRelationship> relationships() {
        return relationships.values();
    }

    /**
     * Returns the relationships declared by this entity, excluding the inherited ones.
     */
    public Collection<CompiledRelationship> declaredRelationships() {
        return declaredRelationships.values();
    }

    /**
     * Returns a relationship of this entity, own or inherited, or null if there is none.
     */
    public CompiledRelationship relationship(String name) {
        return relationships.get(name);
    }

    /**
     * Returns a property of this entity, an attribute or a relationship, own or inherited, or null if there is
     * none.
     */
    public CompiledProperty property(String name) {
        CompiledAttribute attribute = attributes.get(name);
        return attribute != null ? attribute : relationships.get(name);
    }

    /**
     * Returns the attributes, own and inherited, mapped to the primary key columns.
     */
    public List<CompiledAttribute> idAttributes() {
        return idAttributes;
    }

    /**
     * Returns an attribute per primary key column of the table. A PK column that is not a property of the class is
     * represented by a {@link CompiledAttribute#isSynthetic() synthetic} attribute.
     */
    public List<CompiledAttribute> primaryKeyAttributes() {
        return primaryKeyAttributes;
    }

    /**
     * Returns the names of the primary key columns of the table, or an empty list for an entity with no table.
     */
    public List<String> primaryKeyNames() {
        return table != null ? table.primaryKeyNames() : List.of();
    }

    /**
     * Returns the relationships, own and inherited, whose reverse relationships are to-many maps.
     */
    public List<CompiledRelationship> mapRelationships() {
        return mapRelationships;
    }

    /**
     * Returns the attributes of this entity, its super-entities and all its sub-entities.
     */
    public List<CompiledAttribute> allAttributes() {
        return allAttributes;
    }

    /**
     * Returns the relationships of this entity, its super-entities and all its sub-entities.
     */
    public List<CompiledRelationship> allRelationships() {
        return allRelationships;
    }

    /**
     * Returns the tables of this entity and its sub-entities. Usually one table, more under horizontal inheritance,
     * none for an abstract entity with no table and no sub-entities.
     */
    public List<CompiledTable> rootTables() {
        return rootTables;
    }

    /**
     * Returns the tables other than the entity's own that its rows span, reached through flattened attributes and
     * relationships, by the DB path to them.
     */
    public Map<CayennePath, CompiledFlattenedTable> flattenedTables() {
        return flattenedTables;
    }

    /**
     * Returns the layout of a snapshot of an object of this entity: the columns of the attributes, the foreign keys
     * of the to-one relationships holding them, the primary key, and the primary keys of the additional tables, each
     * column listed once.
     */
    public DataRowLayout snapshotLayout() {
        return snapshotLayout;
    }

    /**
     * Returns the attribute of this entity mapped to the given column of its table, or null.
     */
    public CompiledAttribute attributeForColumn(CompiledColumn column) {
        for (CompiledAttribute attribute : attributes.values()) {
            if (attribute.column() == column && !attribute.isFlattened()) {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Returns the relationship of this entity mapped to the given single join, or null.
     */
    public CompiledRelationship relationshipForJoin(CompiledJoin join) {
        for (CompiledRelationship relationship : relationships.values()) {
            List<CompiledJoin> joins = relationship.joins();
            if (joins.size() == 1 && joins.getFirst() == join) {
                return relationship;
            }
        }
        return null;
    }

    /**
     * Returns the deepest entity in the hierarchy of this entity whose qualifier matches the row, or null if the row
     * matches no entity. A row is a snapshot keyed by column names.
     */
    public CompiledEntity entityMatchingRow(Map<String, ?> row) {
        for (CompiledEntity subEntity : subEntities) {
            CompiledEntity matched = subEntity.entityMatchingRow(row);
            if (matched != null) {
                return matched;
            }
        }
        if (tableQualifier != null) {
            return tableQualifier.match(row) ? this : null;
        }
        // no qualifier: a root matches all rows, a sub-entity none
        return superEntity == null ? this : null;
    }

    /**
     * Returns the ids that may identify an object with the given id in an object graph or a snapshot cache: the id
     * itself, followed by the same id keyed by each sub-entity, depth-first. Objects and snapshots are keyed by the
     * concrete entity of an object, so a lookup by an id of a super-entity must check all of them.
     *
     * @param id an id keyed by this entity
     */
    public List<ObjectId> polymorphicIds(ObjectId id) {
        if (subEntities.isEmpty()) {
            return List.of(id);
        }
        List<ObjectId> ids = new ArrayList<>(allSubEntities.size());
        ids.add(id);
        for (int i = 1; i < allSubEntities.size(); i++) {
            ids.add(ObjectId.of(allSubEntities.get(i).name, id));
        }
        return ids;
    }

    /**
     * Prepares the properties of an object for access, injecting value holders into its to-many relationships and
     * embeddable objects into its embedded attributes.
     */
    public void injectValueHolders(Object object) throws PropertyException {
        for (CompiledRelationship relationship : relationships.values()) {
            relationship.injectValueHolder(object);
        }
        for (CompiledEmbeddedAttribute embedded : embeddedAttributes.values()) {
            embedded.injectValueHolder(object);
        }
    }

    /**
     * Sets the attribute values that the entity qualifier implies for a new object of this entity, such as the
     * discriminator of a single-table inheritance sub-entity. Only the qualifier declared on the entity itself is
     * used, as in query translation, where entity qualifiers are not combined with those of the super-entities.
     */
    public void injectDiscriminatorValues(Object object) throws PropertyException {
        for (Map.Entry<CompiledAttribute, Object> e : discriminatorValues.entrySet()) {
            e.getKey().writePropertyDirectly(object, null, e.getValue());
        }
    }

    /**
     * Copies the attribute values from one object to another, invalidating the to-one relationships of the target
     * and leaving the to-many ones alone.
     */
    public void shallowMerge(Object from, Object to) throws PropertyException {
        injectValueHolders(to);
        for (CompiledAttribute attribute : attributes.values()) {
            attribute.writePropertyDirectly(to, attribute.readPropertyDirectly(to), attribute.readPropertyDirectly(from));
        }
        for (CompiledRelationship relationship : relationships.values()) {
            if (relationship instanceof CompiledToOneRelationship toOne) {
                toOne.invalidate(to);
            }
        }
        if (from instanceof Persistent fromPersistent && to instanceof Persistent toPersistent) {
            toPersistent.setSnapshotVersion(fromPersistent.getSnapshotVersion());
        }
    }

    /**
     * Resolves the path against this entity into the {@link CompiledRelationship relationships} it traverses and
     * the {@link CompiledAttribute attribute} it ends in, if any. An embedded attribute and the embeddable attribute
     * that follows it resolve into one attribute; a path ending in an embedded attribute resolves into the
     * {@link CompiledEmbeddedAttribute}. Aliases are not resolved here: expand them first with
     * {@link CayennePath#expandAliases(Map)}. An invalid path results in an ExpressionException.
     */
    public CompiledPropertyPath resolvePath(CayennePath path) throws ExpressionException {
        List<CayennePathSegment> segments = path.segments();
        int last = segments.size() - 1;
        List<CompiledRelationship> relationships = new ArrayList<>(segments.size());
        CompiledEntity entity = this;
        for (int i = 0; i <= last; i++) {
            String segment = segments.get(i).value();
            CompiledAttribute attribute = entity.attribute(segment);
            if (attribute == null) {
                CompiledEmbeddedAttribute embedded = entity.embeddedAttribute(segment);
                if (embedded != null && i == last) {
                    return new CompiledPropertyPath(relationships, null, embedded);
                }
                if (embedded != null) {
                    // an embedded attribute followed by an attribute of the embeddable
                    attribute = entity.attribute(segment + "." + segments.get(i + 1).value());
                    if (attribute == null) {
                        throw cantResolve(path, entity, segments.get(i + 1).value());
                    }
                    i++;
                }
            }
            if (attribute != null) {
                if (i < last) {
                    throw new ExpressionException("Attribute must be the last component of the path: '%s'.",
                            path.value(), null, segment);
                }
                return new CompiledPropertyPath(relationships, attribute, null);
            }
            CompiledRelationship relationship = entity.relationship(segment);
            if (relationship == null) {
                throw cantResolve(path, entity, segment);
            }
            relationships.add(relationship);
            entity = relationship.target();
        }
        return new CompiledPropertyPath(relationships, null, null);
    }

    private static ExpressionException cantResolve(CayennePath path, CompiledEntity entity, String segment) {
        return new ExpressionException("Can't resolve path component: [%s.%s].", path.value(), null, entity.name(),
                segment);
    }

    /**
     * Translates an object path rooted in this entity, with no aliases, to the DB path it maps to.
     */
    private CayennePath toDbPath(CayennePath objPath) {
        CayennePath dbPath = CayennePath.EMPTY_PATH;
        CompiledPropertyPath resolved = resolvePath(objPath);
        int segment = 0;
        for (CompiledRelationship relationship : resolved.relationships()) {
            boolean outer = objPath.segments().get(segment).isOuterJoin();
            for (CompiledJoin join : relationship.joins()) {
                dbPath = dbPath.dot(CayennePath.segmentOf(join.name(), outer));
            }
            segment++;
        }
        CompiledAttribute attribute = resolved.attribute();
        if (attribute != null && attribute.dbPath() != null) {
            dbPath = dbPath.dot(attribute.dbPath());
        }
        return dbPath;
    }

    /**
     * Transforms an Expression to an analogous expression in terms of the table of this entity, converting the object
     * paths to DB paths.
     */
    public Expression translateToDbPath(Expression expression) {
        if (expression == null) {
            return null;
        }
        if (table == null) {
            throw new CayenneRuntimeException("Can't translate expression to DB_PATH, no DbEntity for '%s'.", name);
        }
        return expression.transform(new DbPathConverter());
    }

    /**
     * Transforms an Expression rooted in this entity to an analogous expression in DB terms, rooted in the table of
     * the related entity reached by the relationship path. The result is a DB-path expression, as from
     * {@link #translateToDbPath(Expression)}, that a query of the related entity can use.
     */
    public Expression translateToRelatedTable(Expression expression, CayennePath relationshipPath) {
        if (expression == null) {
            return null;
        }
        if (relationshipPath == null || relationshipPath.isEmpty()) {
            return expression;
        }
        if (table == null) {
            throw new CayenneRuntimeException("Can't transform expression, no DbEntity for '%s'.", name);
        }
        CayennePath dbPath = toDbPath(relationshipPath.expandAliases(expression.getPathAliases()));
        Expression dbClone = expression.transform(new DbPathConverter());
        return table.translateToRelatedTable(dbClone, dbPath);
    }

    @Override
    public String toString() {
        return "CompiledEntity[" + name + "]";
    }

    private final class DbPathConverter implements Function<Object, Object> {

        @Override
        public Object apply(Object input) {
            if (!(input instanceof Expression expression)) {
                return input;
            }
            if (expression instanceof FullObjectExp && expression.getOperandCount() == 0) {
                // A bare "self" reference carries no path, so there is nothing to rebase on the related entity.
                // Resolve it to this entity's PK, which is what it means in the first place.
                List<CompiledColumn> pks = table.primaryKey();
                if (pks.size() > 1) {
                    throw new CayenneRuntimeException("Can't transform a 'self' expression rooted in '%s': "
                            + "entity has more than one PK. Match by id map or ObjectId instead.", name);
                }
                return ExpressionFactory.dbPathExp(pks.getFirst().name());
            }
            if (!(expression instanceof ObjPathExp pathExp)) {
                return input;
            }
            return ExpressionFactory.dbPathExp(toDbPath(pathExp.getExpandedPath()));
        }
    }
}
