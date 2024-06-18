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
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A DbEntity is a mapping descriptor that defines a structure of a database
 * table.
 */
public class DbEntity extends Entity<DbEntity, DbAttribute, DbRelationship>
        implements ConfigurationNode, DbEntityListener, DbAttributeListener, DbRelationshipListener {

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
        this.dbAttributeAdded(new AttributeEvent(this, attr, this, MapEvent.ADD));
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
        this.dbAttributeRemoved(new AttributeEvent(this, attr, this, MapEvent.REMOVE));
    }

    @Override
    public void clearAttributes() {
        super.clearAttributes();
        // post dummy event for no specific attribute
        this.dbAttributeRemoved(new AttributeEvent(this, null, this, MapEvent.REMOVE));
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
     * DbEntity property changed event. May be name, attribute or relationship
     * added or removed, etc. Attribute and relationship property changes are
     * handled in respective listeners.
     *
     * @since 1.2
     */
    public void dbEntityChanged(EntityEvent e) {
        if (e == null || e.getEntity() != this) {
            // not our concern
            return;
        }

        // handle entity name changes
        if (e.getId() == EntityEvent.CHANGE && e.isNameChange()) {
            String newName = e.getNewName();
            DataMap map = getDataMap();
            if (map != null) {
                // handle all of the relationship target names that need to be
                // changed
                for (DbEntity dbe : map.getDbEntities()) {
                    for (DbRelationship relationship : dbe.getRelationships()) {
                        if (relationship.getTargetEntity() == this) {
                            relationship.setTargetEntityName(newName);
                        }
                    }
                }
                // get all of the related object entities
                for (ObjEntity oe : map.getMappedEntities(this)) {
                    if (oe.getDbEntity() == this) {
                        oe.setDbEntityName(newName);
                    }
                }
            }
        }
    }

    /**
     * New entity has been created/added.
     */
    public void dbEntityAdded(EntityEvent e) {
        // does nothing currently
    }

    /**
     * Entity has been removed.
     */
    public void dbEntityRemoved(EntityEvent e) {
        // does nothing currently
    }

    public void dbAttributeAdded(AttributeEvent e) {
        this.handleAttributeUpdate(e);
    }

    public void dbAttributeChanged(AttributeEvent e) {
        this.handleAttributeUpdate(e);
    }

    public void dbAttributeRemoved(AttributeEvent e) {
        this.handleAttributeUpdate(e);
    }

    private void handleAttributeUpdate(AttributeEvent e) {
        if (e == null || e.getEntity() != this) {
            // not our concern
            return;
        }

        // catch clearing (event with null ('any') DbAttribute)
        Attribute<?,?,?> attribute = e.getAttribute();
        if (attribute == null && this.attributes.isEmpty()) {
            this.primaryKey.clear();
            this.generatedAttributes.clear();
            return;
        }

        // make sure we handle a DbAttribute
        if (!(attribute instanceof DbAttribute)) {
            return;
        }

        DbAttribute dbAttribute = (DbAttribute) attribute;

        // handle attribute name changes
        if (e.getId() == AttributeEvent.CHANGE && e.isNameChange()) {
            String oldName = e.getOldName();
            String newName = e.getNewName();

            DataMap map = getDataMap();
            if (map != null) {
                for (DbEntity ent : map.getDbEntities()) {

                    // handle all of the dependent object entity attribute changes
                    for (ObjEntity oe : map.getMappedEntities(ent)) {
                        for (ObjAttribute attr : oe.getAttributes()) {
                            if (attr.getDbAttribute() == dbAttribute) {
                                attr.setDbAttributePath(newName);
                            }
                        }
                    }

                    // handle all of the relationships / joins that use the
                    // changed attribute
                    for (DbRelationship rel : ent.getRelationships()) {
                        for (DbJoin join : rel.getJoins()) {
                            if (join.getSource() == dbAttribute) {
                                join.setSourceName(newName);
                            }
                            if (join.getTarget() == dbAttribute) {
                                join.setTargetName(newName);
                            }
                        }
                    }
                }
            }

            // clear the attribute out of the collection
            attributes.remove(oldName);

            // add the attribute back in with the new name
            super.addAttribute(dbAttribute);
        }

        // handle PK refresh
        if (primaryKey.contains(dbAttribute) || dbAttribute.isPrimaryKey()) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    this.primaryKey.add(dbAttribute);
                    break;

                case MapEvent.REMOVE:
                    this.primaryKey.remove(dbAttribute);
                    break;

                default:
                    // generic update
                    this.primaryKey.clear();
                    for (DbAttribute next : getAttributes()) {
                        if (next.isPrimaryKey()) {
                            this.primaryKey.add(next);
                        }
                    }
            }

            // check toDep PK for reverse relationships
            for (DbRelationship rel : getRelationships()) {
                DbRelationship reverse = rel.getReverseRelationship();
                if(reverse != null && reverse.isToDependentPK() && !reverse.isValidForDepPk()) {
                    reverse.setToDependentPK(false);
                }
            }
        }

        // handle generated key refresh
        if (generatedAttributes.contains(dbAttribute) || dbAttribute.isGenerated()) {
            switch (e.getId()) {
                case MapEvent.ADD:
                    this.generatedAttributes.add(dbAttribute);
                    break;

                case MapEvent.REMOVE:
                    this.generatedAttributes.remove(dbAttribute);
                    break;

                default:
                    // generic update
                    this.generatedAttributes.clear();
                    for (DbAttribute next : getAttributes()) {
                        if (next.isGenerated()) {
                            this.generatedAttributes.add(next);
                        }
                    }
            }
        }
    }

    /**
     * Relationship property changed.
     */
    public void dbRelationshipChanged(RelationshipEvent e) {
        if (e == null || e.getEntity() != this) {
            // not our concern
            return;
        }

        Relationship<?,?,?> rel = e.getRelationship();
        // make sure we handle a DbRelationship
        if (!(rel instanceof DbRelationship)) {
            return;
        }

        DbRelationship dbRel = (DbRelationship) rel;

        // handle relationship name changes
        if (e.getId() == RelationshipEvent.CHANGE && e.isNameChange()) {
            String oldName = e.getOldName();

            DataMap map = getDataMap();
            if (map != null) {

                // updating dbAttributePaths for attributes of all ObjEntities
                for (ObjEntity objEntity : map.getObjEntities()) {

                    for (ObjAttribute attribute : objEntity.getAttributes()) {
                        attribute.updateDbAttributePath();
                    }
                }
            }

            // clear the relationship out of the collection
            relationships.remove(oldName);

            // add the relationship back in with the new name
            super.addRelationship(dbRel);
        }
    }

    /**
     * Relationship has been created/added.
     */
    public void dbRelationshipAdded(RelationshipEvent e) {
        // does nothing currently
    }

    /**
     * Relationship has been removed.
     */
    public void dbRelationshipRemoved(RelationshipEvent e) {
        // does nothing currently
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
