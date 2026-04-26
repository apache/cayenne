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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A DbEntity is a mapping descriptor that defines a structure of a database
 * table.
 */
public class DbEntity extends Entity<DbEntity, DbAttribute, DbRelationship>
        implements ConfigurationNode {

    protected String catalog;
    protected String schema;
    protected List<DbAttribute> primaryKey;

    /**
     * @since 1.2
     */
    protected Collection<DbAttribute> generatedAttributes;
    protected DbKeyGenerator primaryKeyGenerator;

    /**
     * Qualifier, that will be applied to all select queries and joins with this
     * DbEntity
     */
    protected Expression qualifier;

    /**
     * Creates an unnamed DbEntity.
     */
    public DbEntity() {
        super();
        this.primaryKey = new ArrayList<>(2);
        this.generatedAttributes = new ArrayList<>(2);
    }

    /**
     * Creates a named DbEntity.
     */
    public DbEntity(String name) {
        this();
        this.setName(name);
    }

    /**
     * @since 3.1
     */
    @Override
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitDbEntity(this);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     *
     * @since 1.1
     */
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("db-entity").attribute("name", getName());

        if (getSchema() != null && getSchema().trim().length() > 0) {
            encoder.attribute("schema", getSchema().trim());
        }
        if (getCatalog() != null && getCatalog().trim().length() > 0) {
            encoder.attribute("catalog", getCatalog().trim());
        }

        encoder.nested(new TreeMap<>(getAttributeMap()), delegate);

        if (getPrimaryKeyGenerator() != null) {
            getPrimaryKeyGenerator().encodeAsXML(encoder, delegate);
        }

        if (getQualifier() != null) {
            encoder.start("qualifier");
            getQualifier().encodeAsXML(encoder, delegate);
            encoder.end();
        }

        delegate.visitDbEntity(this);
        encoder.end();
    }

    /**
     * Returns table name including catalog and schema, if any of those are
     * present.
     */
    public String getFullyQualifiedName() {
        return ((catalog != null && !catalog.isEmpty()) ? catalog + '.' : "")
                + ((schema != null && !schema.isEmpty()) ? schema + '.' : "")
                + name;
    }

    /**
     * Returns database schema of this table.
     *
     * @return table's schema, null if not set.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the database schema name of the table described by this DbEntity.
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns the catalog name of the table described by this DbEntity.
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Sets the catalog name of the table described by this DbEntity.
     */
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    /**
     * Returns an unmodifiable collection of DbAttributes representing the
     * primary key of the table described by this DbEntity.
     *
     * @since 3.0
     */
    public List<DbAttribute> getPrimaryKeys() {
        return Collections.unmodifiableList(primaryKey);
    }

    /**
     * Returns an unmodifiable collection of DbAttributes that are generated by
     * the database.
     *
     * @since 1.2
     */
    public Collection<DbAttribute> getGeneratedAttributes() {
        return Collections.unmodifiableCollection(generatedAttributes);
    }

    /**
     * Adds a new attribute to this entity.
     *
     * @throws IllegalArgumentException if Attribute has no name or there is an existing attribute
     *                                  with the same name
     * @throws IllegalArgumentException if a relationship has the same name as this attribute
     * @since 3.0
     */
    public void addAttribute(DbAttribute attr) {
        super.addAttribute(attr);
        if (attr.isPrimaryKey() && !primaryKey.contains(attr)) {
            primaryKey.add(attr);
        }
        if (attr.isGenerated() && !generatedAttributes.contains(attr)) {
            generatedAttributes.add(attr);
        }
    }

    /**
     * Removes attribute from the entity, removes any relationship joins
     * containing this attribute. Does nothing if the attribute name is not
     * found.
     *
     * @see org.apache.cayenne.map.Entity#removeAttribute(String)
     */
    @Override
    public void removeAttribute(String attrName) {
        DbAttribute attr = getAttribute(attrName);
        if (attr == null) {
            return;
        }

        DataMap map = getDataMap();
        if (map != null) {
            for (DbEntity ent : map.getDbEntities()) {
                for (DbRelationship relationship : ent.getRelationships()) {
                    relationship.getJoins().removeIf(join -> join.getSource() == attr || join.getTarget() == attr);
                }
            }
        }

        super.removeAttribute(attrName);
        boolean wasPrimaryKey = primaryKey.remove(attr);
        generatedAttributes.remove(attr);
        if (wasPrimaryKey) {
            invalidateToDepPkRelationships();
        }
    }

    @Override
    public void clearAttributes() {
        super.clearAttributes();
        primaryKey.clear();
        generatedAttributes.clear();
    }

    /**
     * Notification from a child {@link DbAttribute} that its primary-key flag has changed.
     * Updates the cached primaryKey collection and enforces the {@code toDependentPK}
     * relationship invariant.
     */
    void attributePrimaryKeyChanged(DbAttribute attr) {
        if (attr.isPrimaryKey()) {
            if (!primaryKey.contains(attr)) {
                primaryKey.add(attr);
            }
        } else {
            primaryKey.remove(attr);
        }
        invalidateToDepPkRelationships();
    }

    /**
     * Notification from a child {@link DbAttribute} that its generated flag has changed.
     */
    void attributeGeneratedChanged(DbAttribute attr) {
        if (attr.isGenerated()) {
            if (!generatedAttributes.contains(attr)) {
                generatedAttributes.add(attr);
            }
        } else {
            generatedAttributes.remove(attr);
        }
    }

    private void invalidateToDepPkRelationships() {
        for (DbRelationship rel : getRelationships()) {
            DbRelationship reverse = rel.getReverseRelationship();
            if (reverse != null && reverse.isToDependentPK() && !reverse.isValidForDepPk()) {
                reverse.setToDependentPK(false);
            }
        }
    }

    /**
     * Renames an attribute owned by this entity, re-keying the attribute map and updating all
     * dependent references across the parent {@link DataMap}: ObjAttribute paths and DbJoin
     * source/target names.
     *
     * @since 5.0
     */
    public void renameAttribute(DbAttribute attr, String newName) {
        if (attr == null || newName == null) {
            throw new NullPointerException("Null attribute or name");
        }
        if (attr.getEntity() != this) {
            throw new IllegalArgumentException("Attribute does not belong to this entity: " + attr.getName());
        }

        String oldName = attr.getName();
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        DataMap map = getDataMap();
        if (map != null) {
            for (ObjEntity oe : map.getMappedEntities(this)) {
                for (ObjAttribute oa : oe.getAttributes()) {
                    if (oa.getDbAttribute() == attr) {
                        oa.setDbAttributePath(newName);
                    }
                }
            }
            for (DbEntity ent : map.getDbEntities()) {
                for (DbRelationship rel : ent.getRelationships()) {
                    for (DbJoin join : rel.getJoins()) {
                        if (join.getSource() == attr) {
                            join.setSourceName(newName);
                        }
                        if (join.getTarget() == attr) {
                            join.setTargetName(newName);
                        }
                    }
                }
            }
        }

        attributes.remove(oldName);
        attr.setName(newName);
        super.addAttribute(attr);
    }

    /**
     * Renames a relationship owned by this entity, re-keying the relationship map and refreshing
     * dependent ObjAttribute db-paths across the parent {@link DataMap}.
     *
     * @since 5.0
     */
    public void renameRelationship(DbRelationship rel, String newName) {
        if (rel == null || newName == null) {
            throw new NullPointerException("Null relationship or name");
        }
        if (rel.getSourceEntity() != this) {
            throw new IllegalArgumentException("Relationship does not belong to this entity: " + rel.getName());
        }

        String oldName = rel.getName();
        if (Util.nullSafeEquals(oldName, newName)) {
            return;
        }

        relationships.remove(oldName);
        rel.setName(newName);
        super.addRelationship(rel);

        DataMap map = getDataMap();
        if (map != null) {
            for (ObjEntity objEntity : map.getObjEntities()) {
                for (ObjAttribute attribute : objEntity.getAttributes()) {
                    attribute.updateDbAttributePath();
                }
            }
        }
    }

    /**
     * Updates dependent references in the parent {@link DataMap} prior to a name change of this
     * entity. Called by {@link DataMap#renameDbEntity(DbEntity, String)}.
     */
    void renameSelf(String newName) {
        DataMap map = getDataMap();
        if (map != null) {
            for (DbEntity dbe : map.getDbEntities()) {
                for (DbRelationship relationship : dbe.getRelationships()) {
                    if (relationship.getTargetEntity() == this) {
                        relationship.setTargetEntityName(newName);
                    }
                }
            }
            for (ObjEntity oe : map.getMappedEntities(this)) {
                if (oe.getDbEntity() == this) {
                    oe.setDbEntityName(newName);
                }
            }
        }
        setName(newName);
    }

    /**
     * Returns an Iterable instance over expression path components based on
     * this entity.
     *
     * @since 3.0
     */
    @Override
    public Iterable<PathComponent<DbAttribute, DbRelationship>> resolvePath(Expression pathExp, Map<String, String> aliasMap) {

        if (pathExp.getType() == Expression.DB_PATH) {
            return () -> new PathComponentIterator<>(DbEntity.this, (CayennePath) pathExp.getOperand(0), aliasMap);
        }

        throw new ExpressionException("Invalid expression type: '" + pathExp.expName() + "',  DB_PATH is expected.");
    }

    @Override
    public Iterator<CayenneMapEntry> resolvePathComponents(Expression pathExp) throws ExpressionException {
        if (pathExp.getType() != Expression.DB_PATH) {
            throw new ExpressionException("Invalid expression type: '" + pathExp.expName() + "',  DB_PATH is expected.");
        }

        return new PathIterator((CayennePath) pathExp.getOperand(0));
    }

    /**
     * Set the primary key generator for this entity. If null is passed, nothing
     * is changed.
     */
    public void setPrimaryKeyGenerator(DbKeyGenerator primaryKeyGenerator) {
        this.primaryKeyGenerator = primaryKeyGenerator;
        if (primaryKeyGenerator != null) {
            primaryKeyGenerator.setDbEntity(this);
        }
    }

    /**
     * Return the primary key generator for this entity.
     */
    public DbKeyGenerator getPrimaryKeyGenerator() {
        return primaryKeyGenerator;
    }

    /**
     * @return qualifier that will be ANDed to all select queries with this
     * entity
     */
    public Expression getQualifier() {
        return qualifier;
    }

    /**
     * Sets qualifier for this entity
     */
    public void setQualifier(Expression qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Returns true if there is full replacement id is attached to an ObjectId.
     * "Full" means that all PK columns are present and only PK columns are
     * present.
     *
     * @since 1.2
     */
    public boolean isFullReplacementIdAttached(ObjectId id) {
        if (!id.isReplacementIdAttached()) {
            return false;
        }

        Map<String, Object> replacement = id.getReplacementIdMap();
        Collection<DbAttribute> pk = getPrimaryKeys();
        if (pk.size() != replacement.size()) {
            return false;
        }

        for (DbAttribute attribute : pk) {
            if (!replacement.containsKey(attribute.getName())) {
                return false;
            }
        }

        return true;
    }

    public Collection<ObjEntity> mappedObjEntities() {
        Collection<ObjEntity> objEntities = new HashSet<>();
        MappingNamespace mns = getDataMap().getNamespace();
        if (mns != null) {
            for (ObjEntity objEntity : mns.getObjEntities()) {
                if (equals(objEntity.getDbEntity())) {
                    objEntities.add(objEntity);
                }
            }
        }
        return objEntities;
    }

    /**
     * Transforms Expression rooted in this entity to an analogous expression
     * rooted in related entity.
     *
     * @since 5.0
     */
    @Override
    public Expression translateToRelatedEntity(Expression expression, CayennePath relationshipPath) {

        if (expression == null) {
            return null;
        }

        if (relationshipPath == null || relationshipPath.isEmpty()) {
            return expression;
        }

        return expression.transform(new RelationshipPathConverter(relationshipPath));
    }

    final class RelationshipPathConverter implements Function<Object, Object> {

        CayennePath relationshipPath;
        boolean toMany;

        RelationshipPathConverter(CayennePath relationshipPath) {
            this.relationshipPath = relationshipPath;

            Iterator<CayenneMapEntry> relationshipIt = resolvePathComponents(relationshipPath);
            while (relationshipIt.hasNext()) {
                // relationship path components must be DbRelationships
                DbRelationship nextDBR = (DbRelationship) relationshipIt.next();
                if (nextDBR.isToMany()) {
                    toMany = true;
                    break;
                }
            }
        }

        public Object apply(Object input) {
            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;
            if (expression.getType() != Expression.DB_PATH) {
                return input;
            }

            CayennePath path = (CayennePath) expression.getOperand(0);
            CayennePath converted = translatePath(path);
            return ExpressionFactory.dbPathExp(converted);
        }

        private PathComponentIterator<DbEntity, DbAttribute, DbRelationship> createPathIterator(CayennePath path) {
            return new PathComponentIterator<>(DbEntity.this, path, Collections.emptyMap());
            // TODO: do we need aliases here?
        }

        CayennePath translatePath(CayennePath path) {
            CayennePath finalPath = CayennePath.EMPTY_PATH;
            PathComponentIterator<DbEntity, DbAttribute, DbRelationship> pathIt = createPathIterator(relationshipPath);
            while (pathIt.hasNext()) {
                // relationship path components must be DbRelationships
                DbRelationship lastDBR = pathIt.next().getRelationship();
                if(lastDBR != null) {
                    finalPath = prependReversedPath(finalPath, lastDBR);
                }
            }
            return finalPath.dot(path);
        }

        private CayennePath prependReversedPath(CayennePath finalPath, DbRelationship relationship) {
            DbRelationship revNextDBR = relationship.getReverseRelationship();

            if (revNextDBR == null) {
                throw new CayenneRuntimeException("Unable to find reverse DbRelationship for %s.%s."
                        , relationship.getSourceEntity().getName(), relationship.getName());
            }

            return CayennePath.of(revNextDBR.getName()).dot(finalPath);
        }
    }
}
