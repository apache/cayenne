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

package org.apache.cayenne.jpa.bridge;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.Types;
import java.util.Iterator;

import javax.persistence.InheritanceType;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaAttribute;
import org.apache.cayenne.jpa.map.JpaAttributeOverride;
import org.apache.cayenne.jpa.map.JpaAttributes;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaColumn;
import org.apache.cayenne.jpa.map.JpaColumnResult;
import org.apache.cayenne.jpa.map.JpaDiscriminatorColumn;
import org.apache.cayenne.jpa.map.JpaEmbeddable;
import org.apache.cayenne.jpa.map.JpaEmbedded;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityListener;
import org.apache.cayenne.jpa.map.JpaEntityListeners;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaEntityResult;
import org.apache.cayenne.jpa.map.JpaFieldResult;
import org.apache.cayenne.jpa.map.JpaId;
import org.apache.cayenne.jpa.map.JpaJoinColumn;
import org.apache.cayenne.jpa.map.JpaManagedClass;
import org.apache.cayenne.jpa.map.JpaManyToMany;
import org.apache.cayenne.jpa.map.JpaManyToOne;
import org.apache.cayenne.jpa.map.JpaNamedQuery;
import org.apache.cayenne.jpa.map.JpaOneToMany;
import org.apache.cayenne.jpa.map.JpaOneToOne;
import org.apache.cayenne.jpa.map.JpaPersistenceUnitDefaults;
import org.apache.cayenne.jpa.map.JpaPersistenceUnitMetadata;
import org.apache.cayenne.jpa.map.JpaPrimaryKeyJoinColumn;
import org.apache.cayenne.jpa.map.JpaQueryHint;
import org.apache.cayenne.jpa.map.JpaRelationship;
import org.apache.cayenne.jpa.map.JpaSecondaryTable;
import org.apache.cayenne.jpa.map.JpaSqlResultSetMapping;
import org.apache.cayenne.jpa.map.JpaTable;
import org.apache.cayenne.jpa.map.JpaVersion;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityListener;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.BaseTreeVisitor;
import org.apache.cayenne.util.HierarchicalTreeVisitor;
import org.apache.cayenne.util.TraversalUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.SimpleValidationFailure;

/**
 * A converter between {@link org.apache.cayenne.jpa.map.JpaEntityMap} and Cayenne
 * {@link org.apache.cayenne.map.DataMap}.
 * 
 */
public class DataMapConverter {

    protected EntityMapLoaderContext context;

    protected ProjectPath targetPath;
    protected HierarchicalTreeVisitor visitor;

    public synchronized DataMap toDataMap(String name, EntityMapLoaderContext context) {
        this.context = context;

        // reset
        DataMap dataMap = new DataMap(name);
        dataMap.setDefaultPackage(context.getEntityMap().getPackageName());
        dataMap.setDefaultSchema(context.getEntityMap().getSchema());

        this.targetPath = new ProjectPath(dataMap);

        if (visitor == null) {
            visitor = createVisitor();
        }

        TraversalUtil.traverse(context.getEntityMap(), visitor);
        postprocess(dataMap);
        return dataMap;
    }

    /**
     * Connects missing reverse relationships.
     */
    protected void postprocess(DataMap dataMap) {

        // connect relationships paired via "mappedBy"; if any other reverse relationships
        // are missing, Cayenne runtime downstream should take care of it
        for (DbEntity entity : dataMap.getDbEntities()) {
            Iterator<?> it = entity.getRelationships().iterator();
            while (it.hasNext()) {
                JpaDbRelationship relationship = (JpaDbRelationship) it.next();
                if (relationship.getMappedBy() != null) {
                    DbRelationship owner = (DbRelationship) relationship
                            .getTargetEntity()
                            .getRelationship(relationship.getMappedBy());

                    if (owner != null) {
                        for (DbJoin join : owner.getJoins()) {
                            DbJoin reverse = join.createReverseJoin();
                            reverse.setRelationship(relationship);
                            relationship.addJoin(reverse);
                        }
                    }
                }
            }
        }
    }

    protected void recordConflict(ProjectPath path, String message) {
        context.recordConflict(new SimpleValidationFailure(path.getObject(), message));
    }

    /**
     * Creates a stateless instance of the JpaEntityMap traversal visitor. This method is
     * lazily invoked and cached by this object.
     */
    protected HierarchicalTreeVisitor createVisitor() {
        BaseTreeVisitor listenersVisitor = new BaseTreeVisitor();
        listenersVisitor.addChildVisitor(
                JpaEntityListener.class,
                new JpaDefaultEntityListenerVisitor());

        BaseTreeVisitor defaultsVisitor = new BaseTreeVisitor();
        defaultsVisitor.addChildVisitor(JpaEntityListeners.class, listenersVisitor);
        BaseTreeVisitor metadataVisitor = new BaseTreeVisitor();
        metadataVisitor
                .addChildVisitor(JpaPersistenceUnitDefaults.class, defaultsVisitor);

        BaseTreeVisitor visitor = new BaseTreeVisitor();
        visitor.addChildVisitor(JpaEntity.class, new JpaEntityVisitor());
        visitor.addChildVisitor(JpaEmbeddable.class, new JpaEmbeddableVisitor());
        visitor.addChildVisitor(JpaNamedQuery.class, new JpaNamedQueryVisitor());
        visitor.addChildVisitor(JpaPersistenceUnitMetadata.class, metadataVisitor);
        visitor.addChildVisitor(
                JpaSqlResultSetMapping.class,
                new JpaSQLResultSetMappingVisitor());
        return visitor;
    }

    private void createDbAttribute(
            String tableName,
            JpaColumn column,
            JpaAttribute attribute) {

        DbAttribute dbAttribute = new DbAttribute(column.getName());

        if (attribute instanceof JpaBasic) {
            JpaBasic basic = (JpaBasic) attribute;
            dbAttribute.setType(basic.getDefaultJdbcType());
        }
        else if (attribute instanceof JpaVersion) {
            JpaVersion version = (JpaVersion) attribute;
            dbAttribute.setType(version.getDefaultJdbcType());
        }

        dbAttribute.setMandatory(!column.isNullable());
        dbAttribute.setMaxLength(column.getLength());

        // DbAttribute "no scale" means -1, not 0 like in JPA.
        if (column.getScale() > 0) {
            dbAttribute.setScale(column.getScale());
        }

        // DbAttribute "no precision" means -1, not 0 like in JPA.
        if (column.getPrecision() > 0) {
            dbAttribute.setAttributePrecision(column.getPrecision());
        }

        DataMap dataMap = targetPath.firstInstanceOf(DataMap.class);
        DbEntity entity = dataMap.getDbEntity(tableName);

        if (entity == null) {
            // table may be defined in a superclass that is not processed yet... so create
            // a barebone version, with all remaining properties to be set later
            entity = new DbEntity(tableName);
            dataMap.addDbEntity(entity);
        }

        entity.addAttribute(dbAttribute);
    }

    private String getSecondaryTableDbRelationshipName(String secondaryTableName) {
        return "$cay_secondary_" + secondaryTableName;
    }

    private EntityListener makeEntityListener(JpaEntityListener jpaListener) {
        EntityListener listener = new EntityListener(jpaListener.getClassName());

        if (jpaListener.getPostLoad() != null) {
            listener.getCallbackMap().getPostLoad().addCallbackMethod(
                    jpaListener.getPostLoad().getMethodName());
        }

        if (jpaListener.getPostPersist() != null) {
            listener.getCallbackMap().getPostPersist().addCallbackMethod(
                    jpaListener.getPostPersist().getMethodName());
        }

        if (jpaListener.getPostRemove() != null) {
            listener.getCallbackMap().getPostRemove().addCallbackMethod(
                    jpaListener.getPostRemove().getMethodName());
        }

        if (jpaListener.getPostUpdate() != null) {
            listener.getCallbackMap().getPostUpdate().addCallbackMethod(
                    jpaListener.getPostUpdate().getMethodName());
        }

        if (jpaListener.getPrePersist() != null) {
            listener.getCallbackMap().getPostAdd().addCallbackMethod(
                    jpaListener.getPrePersist().getMethodName());
        }

        if (jpaListener.getPreRemove() != null) {
            listener.getCallbackMap().getPreRemove().addCallbackMethod(
                    jpaListener.getPreRemove().getMethodName());
        }

        if (jpaListener.getPreUpdate() != null) {
            listener.getCallbackMap().getPreUpdate().addCallbackMethod(
                    jpaListener.getPreUpdate().getMethodName());
        }
        return listener;
    }

    Field lookupFieldInHierarchy(Class<?> beanClass, String fieldName)
            throws SecurityException, NoSuchFieldException {

        try {
            return beanClass.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e) {

            Class<?> superClass = beanClass.getSuperclass();
            if (superClass == null || superClass.getName().equals(Object.class.getName())) {
                throw e;
            }

            return lookupFieldInHierarchy(superClass, fieldName);
        }
    }

    class JpaDefaultEntityListenerVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaEntityListener jpaListener = (JpaEntityListener) path.getObject();

            DataMap map = (DataMap) targetPath.firstInstanceOf(DataMap.class);
            EntityListener listener = makeEntityListener(jpaListener);
            map.addDefaultEntityListener(listener);

            return false;
        }
    }

    class JpaEntityListenerVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaEntityListener jpaListener = (JpaEntityListener) path.getObject();

            EntityListener listener = makeEntityListener(jpaListener);
            ObjEntity entity = (ObjEntity) targetPath.firstInstanceOf(ObjEntity.class);
            entity.addEntityListener(listener);

            return false;
        }
    }

    class JpaEmbeddedVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {
            JpaEmbedded jpaEmbedded = (JpaEmbedded) path.getObject();

            ObjEntity entity = (ObjEntity) targetPath.getObject();

            EmbeddedAttribute embedded = new EmbeddedAttribute(jpaEmbedded.getName());
            embedded.setType(jpaEmbedded.getPropertyDescriptor().getType().getName());

            for (JpaAttributeOverride override : jpaEmbedded.getAttributeOverrides()) {
                embedded.addAttributeOverride(override.getName(), override
                        .getColumn()
                        .getName());
            }

            entity.addAttribute(embedded);

            // for each embedded attribute, add all Embeddable attributes to DbEntity,
            // honoring @Column settings
            JpaEmbeddable jpaEmbeddable = path
                    .firstInstanceOf(JpaEntityMap.class)
                    .embeddableForClass(jpaEmbedded.getPropertyDescriptor().getType());

            for (JpaBasic jpaBasic : jpaEmbeddable.getAttributes().getBasicAttributes()) {

                JpaColumn column = jpaBasic.getColumn();
                String tableName = column.getTable() != null ? column.getTable() : entity
                        .getDbEntityName();

                createDbAttribute(tableName, jpaBasic.getColumn(), jpaBasic);
            }

            return embedded;
        }
    }

    class JpaEmbeddableBasicVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {
            JpaBasic jpaBasic = (JpaBasic) path.getObject();

            Embeddable embeddable = (Embeddable) targetPath.getObject();

            EmbeddableAttribute attribute = new EmbeddableAttribute(jpaBasic.getName());
            attribute.setType(getAttributeType(path, jpaBasic.getName()).getName());
            attribute.setDbAttributeName(jpaBasic.getColumn().getName());

            embeddable.addAttribute(attribute);
            return attribute;
        }

        Class<?> getAttributeType(ProjectPath path, String name) {
            AccessType access = null;

            JpaManagedClass entity = path.firstInstanceOf(JpaManagedClass.class);
            access = entity.getAccess();

            if (access == null) {
                JpaEntityMap map = path.firstInstanceOf(JpaEntityMap.class);
                access = map.getAccess();
            }

            Class<?> objectClass = targetPath
                    .firstInstanceOf(Embeddable.class)
                    .getJavaClass();

            try {
                if (access == AccessType.FIELD) {
                    return lookupFieldInHierarchy(objectClass, name).getType();
                }
                else {
                    return new PropertyDescriptor(name, objectClass).getPropertyType();
                }
            }
            catch (Exception e) {
                throw new JpaProviderException("Error resolving attribute '"
                        + name
                        + "', access type:"
                        + access
                        + ", class: "
                        + objectClass.getName(), e);
            }
        }
    }

    class JpaBasicVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaManagedClass entity = path.firstInstanceOf(JpaManagedClass.class);
            JpaBasic jpaBasic = (JpaBasic) path.getObject();

            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            ObjAttribute cayenneAttribute = new ObjAttribute(jpaBasic.getName());
            cayenneAttribute
                    .setType(getAttributeType(path, jpaBasic.getName()).getName());
            cayenneAttribute.setDbAttributePath(getAttributePath(path, entity, jpaBasic
                    .getColumn()));

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;
        }

        Class<?> getAttributeType(ProjectPath path, String name) {
            AccessType access = null;

            JpaManagedClass entity = path.firstInstanceOf(JpaManagedClass.class);
            access = entity.getAccess();

            if (access == null) {
                JpaEntityMap map = path.firstInstanceOf(JpaEntityMap.class);
                access = map.getAccess();
            }

            Class<?> objectClass = targetPath
                    .firstInstanceOf(ObjEntity.class)
                    .getJavaClass();

            try {
                if (access == AccessType.FIELD) {
                    return lookupFieldInHierarchy(objectClass, name).getType();
                }
                else {
                    return new PropertyDescriptor(name, objectClass).getPropertyType();
                }
            }
            catch (Exception e) {
                throw new JpaProviderException("Error resolving attribute '"
                        + name
                        + "', access type:"
                        + access
                        + ", class: "
                        + objectClass.getName(), e);
            }
        }

        protected String getAttributePath(
                ProjectPath path,
                JpaManagedClass managedClass,
                JpaColumn column) {

            if (managedClass instanceof JpaEntity) {
                JpaEntity entity = (JpaEntity) managedClass;

                if (column.getTable().equals(entity.lookupTable().getName())) {
                    return column.getName();
                }

                JpaSecondaryTable table = entity.getSecondaryTable(column.getTable());
                if (table == null) {
                    recordConflict(path, "Unrecognized secondary table: '"
                            + column.getTable()
                            + "'");
                    return column.getName();
                }

                return getSecondaryTableDbRelationshipName(table.getName())
                        + '.'
                        + column.getName();
            }
            else {
                // TODO: andrus, 12/23/2007 this would miss a case if a user decides to
                // specify a secondary table on an abstract superclass that is not linked
                // to a table yet ... which would be quite crazy, but still...
                return column.getName();
            }
        }

    }

    class JpaVersionVisitor extends JpaBasicVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaManagedClass entity = path.firstInstanceOf(JpaManagedClass.class);
            JpaVersion version = (JpaVersion) path.getObject();

            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            ObjAttribute cayenneAttribute = new ObjAttribute(version.getName());
            cayenneAttribute.setType(getAttributeType(path, version.getName()).getName());
            cayenneAttribute.setDbAttributePath(getAttributePath(path, entity, version
                    .getColumn()));

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;
        }
    }

    class JpaColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn jpaColumn = (JpaColumn) path.getObject();

            if (jpaColumn.getTable() == null) {
                throw new JpaProviderException("No default table defined for JpaColumn "
                        + jpaColumn.getName());
            }

            JpaAttribute attribute = (JpaAttribute) path.getObjectParent();
            createDbAttribute(jpaColumn.getTable(), jpaColumn, attribute);
            return false;
        }
    }

    class JpaIdVisitor extends JpaBasicVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaId id = (JpaId) path.getObject();

            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            ObjAttribute cayenneAttribute = new ObjAttribute(id.getName());
            cayenneAttribute.setType(getAttributeType(path, id.getName()).getName());

            // assuming id's can not be flattened to another table...
            cayenneAttribute.setDbAttributePath(id.getColumn().getName());

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;
        }
    }

    class JpaIdColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn jpaColumn = (JpaColumn) path.getObject();

            DbAttribute dbAttribute = new DbAttribute(jpaColumn.getName());

            JpaId jpaId = path.firstInstanceOf(JpaId.class);

            dbAttribute.setType(jpaId.getDefaultJdbcType());

            dbAttribute.setMaxLength(jpaColumn.getLength());
            dbAttribute.setMandatory(true);
            dbAttribute.setPrimaryKey(true);

            if (jpaColumn.getScale() > 0) {
                dbAttribute.setScale(jpaColumn.getScale());
            }

            if (jpaColumn.getPrecision() > 0) {
                dbAttribute.setAttributePrecision(jpaColumn.getPrecision());
            }

            if (jpaColumn.getTable() == null) {
                recordConflict(path, "No table defined for JpaColumn '"
                        + jpaColumn.getName()
                        + "'");
                return false;
            }

            DbEntity entity = targetPath.firstInstanceOf(DataMap.class).getDbEntity(
                    jpaColumn.getTable());

            if (entity == null) {
                recordConflict(path, "Invalid table definition for JpaColumn: "
                        + jpaColumn.getTable()
                        + "'");
                return false;
            }

            entity.addAttribute(dbAttribute);
            return false;
        }
    }

    class JpaJoinColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {

            JpaJoinColumn jpaJoin = (JpaJoinColumn) path.getObject();
            JpaRelationship jpaRelationship = (JpaRelationship) path.getObjectParent();
            JpaEntity targetEntity = context.getEntityMap().entityForClass(
                    jpaRelationship.getTargetEntityName());
            JpaId jpaTargetId = targetEntity.getAttributes().getIdForColumnName(
                    jpaJoin.getReferencedColumnName());

            if (jpaTargetId == null) {
                throw new IllegalArgumentException("Null id "
                        + targetEntity.getName()
                        + "."
                        + jpaJoin.getReferencedColumnName());
            }

            DbRelationship dbRelationship = (DbRelationship) targetPath.getObject();

            // add FK
            DbAttribute src = new DbAttribute(jpaJoin.getName());

            // TODO: andrus, 5/2/2006 - infer this from Jpa relationship
            src.setMandatory(false);
            src.setMaxLength(jpaTargetId.getColumn().getLength());
            src.setType(jpaTargetId.getDefaultJdbcType());

            Entity srcEntity = dbRelationship.getSourceEntity();
            srcEntity.addAttribute(src);

            // add join
            DbJoin join = new DbJoin(dbRelationship, src.getName(), jpaTargetId
                    .getColumn()
                    .getName());
            dbRelationship.addJoin(join);

            return false;
        }
    }

    class JpaEmbeddableVisitor extends NestedVisitor {

        JpaEmbeddableVisitor() {

            BaseTreeVisitor attributeVisitor = new BaseTreeVisitor();

            JpaEmbeddableBasicVisitor basicVisitor = new JpaEmbeddableBasicVisitor();
            attributeVisitor.addChildVisitor(JpaBasic.class, basicVisitor);
            addChildVisitor(JpaAttributes.class, attributeVisitor);
        }

        @Override
        Object createObject(ProjectPath path) {
            JpaEmbeddable jpaEmbeddable = (JpaEmbeddable) path.getObject();
            Embeddable embeddable = new Embeddable(jpaEmbeddable.getClassName());
            ((DataMap) targetPath.getObject()).addEmbeddable(embeddable);
            return embeddable;
        }
    }

    class JpaSQLResultSetMappingVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaSqlResultSetMapping jpaMapping = (JpaSqlResultSetMapping) path.getObject();

            SQLResult mapping = new SQLResult(jpaMapping.getName());

            for (JpaColumnResult c : jpaMapping.getColumnResults()) {
                mapping.addColumnResult(c.getName());
            }

            for (JpaEntityResult e : jpaMapping.getEntityResults()) {

                EntityResult result = new EntityResult(e.getEntityClassName());
                for (JpaFieldResult f : e.getFieldResults()) {
                    result.addObjectField(f.getName(), f.getColumn());
                }

                // TODO: andrus 2/23/2008 - discriminator column...
            }

            // my understanding of JPA SQLResultSetMapping is that regardless of whether
            // it belongs to entity or to the entire persistence unit, the scope is still
            // persistence unit??
            targetPath.firstInstanceOf(DataMap.class).addResult(mapping);
            return false;
        }
    }

    class JpaEntityVisitor extends NestedVisitor {

        JpaEntityVisitor() {

            BaseTreeVisitor listenersVisitor = new BaseTreeVisitor();
            listenersVisitor.addChildVisitor(
                    JpaEntityListener.class,
                    new JpaEntityListenerVisitor());

            BaseTreeVisitor attributeVisitor = new BaseTreeVisitor();
            attributeVisitor.addChildVisitor(
                    JpaManyToOne.class,
                    new JpaManyToOneVisitor());
            attributeVisitor.addChildVisitor(JpaOneToOne.class, new JpaOneToOneVisitor());
            attributeVisitor.addChildVisitor(
                    JpaOneToMany.class,
                    new JpaOneToManyVisitor());
            attributeVisitor.addChildVisitor(
                    JpaManyToMany.class,
                    new JpaManyToManyVisitor());

            JpaBasicVisitor basicVisitor = new JpaBasicVisitor();
            basicVisitor.addChildVisitor(JpaColumn.class, new JpaColumnVisitor());
            attributeVisitor.addChildVisitor(JpaBasic.class, basicVisitor);

            JpaEmbeddedVisitor embeddedVisitor = new JpaEmbeddedVisitor();
            attributeVisitor.addChildVisitor(JpaEmbedded.class, embeddedVisitor);

            JpaVersionVisitor versionVisitor = new JpaVersionVisitor();
            versionVisitor.addChildVisitor(JpaColumn.class, new JpaColumnVisitor());
            attributeVisitor.addChildVisitor(JpaVersion.class, versionVisitor);

            JpaIdVisitor idVisitor = new JpaIdVisitor();
            idVisitor.addChildVisitor(JpaColumn.class, new JpaIdColumnVisitor());
            attributeVisitor.addChildVisitor(JpaId.class, idVisitor);

            // TODO: andrus 8/6/2006 - handle EmbeddedId, AttributeOverride

            addChildVisitor(JpaAttributes.class, attributeVisitor);
            addChildVisitor(JpaTable.class, new JpaTableVisitor());
            addChildVisitor(JpaSecondaryTable.class, new JpaSecondaryTableVisitor());
            addChildVisitor(JpaNamedQuery.class, new JpaNamedQueryVisitor());
            addChildVisitor(JpaEntityListeners.class, listenersVisitor);
            addChildVisitor(
                    JpaSqlResultSetMapping.class,
                    new JpaSQLResultSetMappingVisitor());
        }

        @Override
        Object createObject(ProjectPath path) {
            JpaEntity jpaEntity = (JpaEntity) path.getObject();
            ObjEntity cayenneEntity = new ObjEntity(jpaEntity.getName());

            if (jpaEntity.getInheritance() == null
                    && jpaEntity.lookupInheritanceStrategy() == InheritanceType.SINGLE_TABLE) {
                cayenneEntity.setSuperEntityName(jpaEntity.getSuperEntity().getName());
            }

            cayenneEntity.setClassName(jpaEntity.getClassName());
            initCallbacks(jpaEntity, cayenneEntity);

            ((DataMap) targetPath.getObject()).addObjEntity(cayenneEntity);

            return cayenneEntity;
        }

        private void initCallbacks(JpaEntity jpaEntity, ObjEntity cayenneEntity) {
            if (jpaEntity.getPostLoad() != null) {
                cayenneEntity.getCallbackMap().getPostLoad().addCallbackMethod(
                        jpaEntity.getPostLoad().getMethodName());
            }

            if (jpaEntity.getPostPersist() != null) {
                cayenneEntity.getCallbackMap().getPostPersist().addCallbackMethod(
                        jpaEntity.getPostPersist().getMethodName());
            }

            if (jpaEntity.getPostRemove() != null) {
                cayenneEntity.getCallbackMap().getPostRemove().addCallbackMethod(
                        jpaEntity.getPostRemove().getMethodName());
            }

            if (jpaEntity.getPostUpdate() != null) {
                cayenneEntity.getCallbackMap().getPostUpdate().addCallbackMethod(
                        jpaEntity.getPostUpdate().getMethodName());
            }

            if (jpaEntity.getPrePersist() != null) {
                cayenneEntity.getCallbackMap().getPostAdd().addCallbackMethod(
                        jpaEntity.getPrePersist().getMethodName());
            }

            if (jpaEntity.getPreRemove() != null) {
                cayenneEntity.getCallbackMap().getPreRemove().addCallbackMethod(
                        jpaEntity.getPreRemove().getMethodName());
            }

            if (jpaEntity.getPreUpdate() != null) {
                cayenneEntity.getCallbackMap().getPreUpdate().addCallbackMethod(
                        jpaEntity.getPreUpdate().getMethodName());
            }
        }

        @Override
        public void onFinishNode(ProjectPath path) {

            JpaEntity entity = path.firstInstanceOf(JpaEntity.class);
            DataMap dataMap = targetPath.firstInstanceOf(DataMap.class);
            ObjEntity cayenneEntity = targetPath.firstInstanceOf(ObjEntity.class);

            // as superentity may not be loaded yet, must lookup DbEntity via JPA
            // mapping...
            DbEntity cayennePrimaryTable = dataMap.getDbEntity(entity
                    .lookupTable()
                    .getName());

            for (JpaSecondaryTable secondaryTable : entity.getSecondaryTables()) {

                // create a relationship between master DbEntity and a secondary
                // DbEntity...

                DbEntity cayenneSecondaryTable = dataMap.getDbEntity(secondaryTable
                        .getName());

                JpaDbRelationship dbRelationship = new JpaDbRelationship(
                        getSecondaryTableDbRelationshipName(secondaryTable.getName()));
                dbRelationship.setTargetEntityName(secondaryTable.getName());
                cayennePrimaryTable.addRelationship(dbRelationship);

                for (JpaPrimaryKeyJoinColumn column : secondaryTable
                        .getPrimaryKeyJoinColumns()) {
                    DbAttribute pkAttribute = (DbAttribute) cayennePrimaryTable
                            .getAttribute(column.getReferencedColumnName());
                    if (pkAttribute == null) {
                        recordConflict(path, "Invalid referenced column name: "
                                + column.getReferencedColumnName());
                        continue;
                    }

                    DbAttribute attribute = new DbAttribute(column.getName());
                    attribute.setPrimaryKey(true);
                    attribute.setMandatory(true);
                    attribute.setAttributePrecision(pkAttribute.getAttributePrecision());
                    attribute.setType(pkAttribute.getType());
                    attribute.setMaxLength(pkAttribute.getMaxLength());
                    attribute.setAttributePrecision(pkAttribute.getAttributePrecision());
                    cayenneSecondaryTable.addAttribute(attribute);

                    DbJoin join = new DbJoin(dbRelationship, column
                            .getReferencedColumnName(), column.getName());
                    dbRelationship.addJoin(join);
                }

                dbRelationship.setToDependentPK(true);
                dbRelationship.setToMany(false);
            }

            // init discriminator column
            JpaDiscriminatorColumn discriminator = entity.lookupDiscriminatorColumn();
            if (discriminator != null) {

                if (cayennePrimaryTable.getAttribute(discriminator.getName()) == null) {
                    DbAttribute dbAttribute = new DbAttribute(discriminator.getName());

                    switch (discriminator.getDiscriminatorType()) {
                        case CHAR:
                            dbAttribute.setType(Types.CHAR);
                            dbAttribute.setMaxLength(1);
                            break;
                        case STRING:
                            dbAttribute.setType(Types.VARCHAR);
                            dbAttribute.setMaxLength(discriminator.getLength());
                            break;
                        case INTEGER:
                            dbAttribute.setType(Types.INTEGER);
                            break;
                    }

                    dbAttribute.setMandatory(false);
                    cayennePrimaryTable.addAttribute(dbAttribute);
                }

                String valueString = entity.getDiscriminatorValue();
                if (valueString != null && valueString.length() > 0) {

                    Object value = null;
                    switch (discriminator.getDiscriminatorType()) {
                        case CHAR:
                            value = valueString.charAt(0);
                            break;
                        case STRING:
                            value = valueString;
                            break;
                        case INTEGER:
                            try {
                                value = Integer.valueOf(valueString);
                            }
                            catch (NumberFormatException e) {
                                recordConflict(
                                        path,
                                        "Invalid integer discriminator value '"
                                                + valueString);
                            }
                            break;
                    }

                    if (value != null) {
                        cayenneEntity.setDeclaredQualifier(ExpressionFactory.matchDbExp(
                                discriminator.getName(),
                                value));
                    }

                }
            }

            super.onFinishNode(path);
        }
    }

    class JpaManyToOneVisitor extends JpaRelationshipVisitor {

        @Override
        Object createObject(ProjectPath path) {
            ObjRelationship objRelationship = (ObjRelationship) super.createObject(path);
            return objRelationship.getDbRelationships().get(0);
        }
    }

    class JpaOneToManyVisitor extends JpaRelationshipVisitor {

        @Override
        Object createObject(ProjectPath path) {
            ObjRelationship objRelationship = (ObjRelationship) super.createObject(path);
            JpaDbRelationship relationship = (JpaDbRelationship) objRelationship
                    .getDbRelationships()
                    .get(0);

            if (relationship != null) {
                JpaOneToMany jpaRelationship = (JpaOneToMany) path.getObject();
                relationship.setMappedBy(jpaRelationship.getMappedBy());
                objRelationship.setMapKey(jpaRelationship.getMapKey());

                objRelationship.setCollectionType(jpaRelationship
                        .getPropertyDescriptor()
                        .getType()
                        .getName());
            }
            return relationship;
        }
    }

    class JpaOneToOneVisitor extends JpaRelationshipVisitor {

        @Override
        Object createObject(ProjectPath path) {
            ObjRelationship objRelationship = (ObjRelationship) super.createObject(path);
            JpaDbRelationship relationship = (JpaDbRelationship) objRelationship
                    .getDbRelationships()
                    .get(0);

            if (relationship != null) {
                JpaOneToOne jpaRelationship = (JpaOneToOne) path.getObject();
                relationship.setMappedBy(jpaRelationship.getMappedBy());
            }
            return relationship;
        }
    }

    class JpaManyToManyVisitor extends JpaRelationshipVisitor {

        @Override
        Object createObject(ProjectPath path) {
            ObjRelationship objRelationship = (ObjRelationship) super.createObject(path);
            JpaDbRelationship relationship = (JpaDbRelationship) objRelationship
                    .getDbRelationships()
                    .get(0);

            if (relationship != null) {
                JpaManyToMany jpaRelationship = (JpaManyToMany) path.getObject();
                relationship.setMappedBy(jpaRelationship.getMappedBy());
            }
            return relationship;
        }
    }

    abstract class JpaRelationshipVisitor extends NestedVisitor {

        JpaRelationshipVisitor() {
            addChildVisitor(JpaJoinColumn.class, new JpaJoinColumnVisitor());
        }

        @Override
        Object createObject(ProjectPath path) {

            JpaRelationship relationship = (JpaRelationship) path.getObject();

            ObjEntity cayenneSrcEntity = (ObjEntity) targetPath.getObject();
            ObjRelationship cayenneRelationship = new ObjRelationship(relationship
                    .getName());

            cayenneSrcEntity.addRelationship(cayenneRelationship);

            JpaEntity jpaTargetEntity = ((JpaEntityMap) path.getRoot())
                    .entityForClass(relationship.getTargetEntityName());

            if (jpaTargetEntity == null) {
                recordConflict(path, "Unknown target entity '"
                        + relationship.getTargetEntityName());
                return null;
            }

            cayenneRelationship.setTargetEntityName(jpaTargetEntity.getName());

            DbEntity cayenneSrcDbEntity = cayenneSrcEntity.getDbEntity();
            DbEntity cayenneTargetDbEntity = cayenneSrcEntity.getDataMap().getDbEntity(
                    jpaTargetEntity.getTable().getName());
            if (cayenneTargetDbEntity == null) {
                cayenneTargetDbEntity = new DbEntity(jpaTargetEntity.getTable().getName());
                cayenneSrcEntity.getDataMap().addDbEntity(cayenneTargetDbEntity);
            }

            JpaDbRelationship dbRelationship = new JpaDbRelationship(cayenneRelationship
                    .getName());
            dbRelationship.setTargetEntity(cayenneTargetDbEntity);
            dbRelationship.setToMany(relationship.isToMany());

            cayenneSrcDbEntity.addRelationship(dbRelationship);
            cayenneRelationship.addDbRelationship(dbRelationship);

            return cayenneRelationship;
        }
    }

    class JpaNamedQueryVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {
            JpaNamedQuery jpaQuery = (JpaNamedQuery) path.getObject();
            JpaIndirectQuery cayenneQuery;

            JpaQueryHint hint = jpaQuery.getHint(QueryHints.QUERY_TYPE_HINT);
            if (hint != null && !Util.isEmptyString(hint.getValue())) {
                try {

                    // query class is not enhanced, so use normal class loader
                    Class<?> cayenneQueryClass = Class.forName(
                            hint.getValue(),
                            true,
                            Thread.currentThread().getContextClassLoader());

                    if (!JpaIndirectQuery.class.isAssignableFrom(cayenneQueryClass)) {
                        recordConflict(path, "Unknown type for Cayenne query '"
                                + jpaQuery.getName()
                                + "': "
                                + cayenneQueryClass.getName());
                        return null;
                    }

                    cayenneQuery = (JpaIndirectQuery) cayenneQueryClass.newInstance();
                }
                catch (Exception e) {
                    recordConflict(path, "Problem while creating Cayenne query '"
                            + jpaQuery.getName()
                            + "', exception"
                            + e.getMessage());
                    return null;
                }
            }
            else {
                // by default use EJBQL query...
                cayenneQuery = new JpaEjbQLQuery();
            }

            cayenneQuery.setName(jpaQuery.getName());
            cayenneQuery.setJpaQuery(jpaQuery);

            DataMap parentMap = (DataMap) targetPath.firstInstanceOf(DataMap.class);

            ObjEntity parentEntity = (ObjEntity) targetPath
                    .firstInstanceOf(ObjEntity.class);
            if (parentEntity != null) {
                cayenneQuery.setParentEntity(parentEntity);
            }
            else {
                cayenneQuery.setParentMap(parentMap);
            }

            parentMap.addQuery(cayenneQuery);

            return cayenneQuery;
        }
    }

    class JpaTableVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaTable jpaTable = (JpaTable) path.getObject();
            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            DbEntity cayenneEntity = parentCayenneEntity.getDataMap().getDbEntity(
                    jpaTable.getName());
            if (cayenneEntity == null) {
                cayenneEntity = new DbEntity(jpaTable.getName());
                parentCayenneEntity.getDataMap().addDbEntity(cayenneEntity);
            }

            // override catalog and schema even if this is an existing entity. See for
            // instance JpaColumnVisitor for an example on how an entity without all
            // properties is created early.
            cayenneEntity.setCatalog(jpaTable.getCatalog());
            cayenneEntity.setSchema(jpaTable.getSchema());

            parentCayenneEntity.setDbEntity(cayenneEntity);
            return cayenneEntity;
        }
    }

    class JpaSecondaryTableVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaSecondaryTable jpaTable = (JpaSecondaryTable) path.getObject();
            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            DbEntity secondaryEntity = parentCayenneEntity.getDataMap().getDbEntity(
                    jpaTable.getName());
            if (secondaryEntity == null) {
                secondaryEntity = new DbEntity(jpaTable.getName());
                parentCayenneEntity.getDataMap().addDbEntity(secondaryEntity);
            }

            secondaryEntity.setCatalog(jpaTable.getCatalog());
            secondaryEntity.setSchema(jpaTable.getSchema());

            // defer primary./secondary relationship creation till after parent entity's
            // children are fully parsed...

            return secondaryEntity;
        }
    }

    /**
     * A superclass of visitors that need to push/pop processed object from the stack.
     */
    abstract class NestedVisitor extends BaseTreeVisitor {

        abstract Object createObject(ProjectPath path);

        @Override
        public boolean onStartNode(ProjectPath path) {
            Object object = createObject(path);

            if (object != null) {
                targetPath = targetPath.appendToPath(object);
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public void onFinishNode(ProjectPath path) {
            targetPath = targetPath.subpathWithSize(targetPath.getPath().length - 1);
        }
    }
}
