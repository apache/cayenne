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
import java.util.Iterator;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaAttribute;
import org.apache.cayenne.jpa.map.JpaAttributes;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaColumn;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityListener;
import org.apache.cayenne.jpa.map.JpaEntityListeners;
import org.apache.cayenne.jpa.map.JpaEntityMap;
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
import org.apache.cayenne.jpa.map.JpaQueryHint;
import org.apache.cayenne.jpa.map.JpaRelationship;
import org.apache.cayenne.jpa.map.JpaTable;
import org.apache.cayenne.jpa.map.JpaVersion;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityListener;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
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
 * @author Andrus Adamchik
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
        Iterator entities = dataMap.getDbEntities().iterator();
        while (entities.hasNext()) {

            DbEntity entity = (DbEntity) entities.next();
            Iterator it = entity.getRelationships().iterator();
            while (it.hasNext()) {
                JpaDbRelationship relationship = (JpaDbRelationship) it.next();
                if (relationship.getMappedBy() != null) {
                    DbRelationship owner = (DbRelationship) relationship
                            .getTargetEntity()
                            .getRelationship(relationship.getMappedBy());

                    if (owner != null) {
                        Iterator joins = owner.getJoins().iterator();
                        while (joins.hasNext()) {
                            DbJoin join = (DbJoin) joins.next();
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
        visitor.addChildVisitor(JpaNamedQuery.class, new JpaNamedQueryVisitor());
        visitor.addChildVisitor(JpaPersistenceUnitMetadata.class, metadataVisitor);
        return visitor;
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
            listener.getCallbackMap().getPrePersist().addCallbackMethod(
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

    class JpaDefaultEntityListenerVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaEntityListener jpaListener = (JpaEntityListener) path.getObject();

            DataMap map = (DataMap) targetPath.firstInstanceOf(DataMap.class);
            EntityListener listener = map.getEntityListener(jpaListener.getClassName());
            if (listener == null) {
                listener = makeEntityListener(jpaListener);
                map.addEntityListener(listener);
            }

            map.addDefaultEntityListener(listener);
            return false;
        }
    }

    class JpaEntityListenerVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaEntityListener jpaListener = (JpaEntityListener) path.getObject();

            DataMap map = (DataMap) targetPath.firstInstanceOf(DataMap.class);
            EntityListener listener = map.getEntityListener(jpaListener.getClassName());
            if (listener == null) {
                listener = makeEntityListener(jpaListener);
                map.addEntityListener(listener);
            }

            ObjEntity entity = (ObjEntity) targetPath.firstInstanceOf(ObjEntity.class);
            entity.addEntityListener(listener);

            return false;
        }
    }

    class JpaBasicVisitor extends NestedVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaBasic jpaBasic = (JpaBasic) path.getObject();

            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            ObjAttribute cayenneAttribute = new ObjAttribute(jpaBasic.getName());
            cayenneAttribute
                    .setType(getAttributeType(path, jpaBasic.getName()).getName());
            cayenneAttribute.setDbAttributeName(jpaBasic.getColumn().getName());

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;
        }

        Class getAttributeType(ProjectPath path, String name) {
            AccessType access = null;

            JpaManagedClass entity = (JpaManagedClass) path
                    .firstInstanceOf(JpaManagedClass.class);
            access = entity.getAccess();

            if (access == null) {
                JpaEntityMap map = (JpaEntityMap) path
                        .firstInstanceOf(JpaEntityMap.class);
                access = map.getAccess();
            }

            Class objectClass = ((ObjEntity) targetPath.firstInstanceOf(ObjEntity.class))
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

        Field lookupFieldInHierarchy(Class beanClass, String fieldName)
                throws SecurityException, NoSuchFieldException {

            try {
                return beanClass.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException e) {

                Class superClass = beanClass.getSuperclass();
                if (superClass == null
                        || superClass.getName().equals(Object.class.getName())) {
                    throw e;
                }

                return lookupFieldInHierarchy(superClass, fieldName);
            }
        }
    }

    class JpaVersionVisitor extends JpaBasicVisitor {

        @Override
        Object createObject(ProjectPath path) {

            JpaVersion version = (JpaVersion) path.getObject();

            ObjEntity parentCayenneEntity = (ObjEntity) targetPath.getObject();

            ObjAttribute cayenneAttribute = new ObjAttribute(version.getName());
            cayenneAttribute.setType(getAttributeType(path, version.getName()).getName());
            cayenneAttribute.setDbAttributeName(version.getColumn().getName());

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;
        }
    }

    class JpaColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn jpaColumn = (JpaColumn) path.getObject();
            JpaAttribute attribute = (JpaAttribute) path.getObjectParent();

            DbAttribute dbAttribute = new DbAttribute(jpaColumn.getName());

            if (attribute instanceof JpaBasic) {
                JpaBasic basic = (JpaBasic) attribute;
                dbAttribute.setType(basic.getDefaultJdbcType());
            }
            else if (attribute instanceof JpaVersion) {
                JpaVersion version = (JpaVersion) attribute;
                dbAttribute.setType(version.getDefaultJdbcType());
            }

            dbAttribute.setMandatory(!jpaColumn.isNullable());
            dbAttribute.setMaxLength(jpaColumn.getLength());

            // DbAttribute "no scale" means -1, not 0 like in JPA.
            if (jpaColumn.getScale() > 0) {
                dbAttribute.setScale(jpaColumn.getScale());
            }

            // DbAttribute "no precision" means -1, not 0 like in JPA.
            if (jpaColumn.getPrecision() > 0) {
                dbAttribute.setAttributePrecision(jpaColumn.getPrecision());
            }

            if (jpaColumn.getTable() == null) {
                throw new JpaProviderException("No default table defined for JpaColumn "
                        + jpaColumn.getName());
            }

            DbEntity entity = ((DataMap) targetPath.firstInstanceOf(DataMap.class))
                    .getDbEntity(jpaColumn.getTable());

            if (entity == null) {
                throw new JpaProviderException("No DbEntity defined for table  "
                        + jpaColumn.getTable());
            }

            entity.addAttribute(dbAttribute);

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
            cayenneAttribute.setDbAttributeName(id.getColumn().getName());

            parentCayenneEntity.addAttribute(cayenneAttribute);
            return cayenneAttribute;
        }
    }

    class JpaIdColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn jpaColumn = (JpaColumn) path.getObject();

            DbAttribute dbAttribute = new DbAttribute(jpaColumn.getName());

            JpaId jpaId = (JpaId) path.firstInstanceOf(JpaId.class);

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

            DbEntity entity = ((DataMap) targetPath.firstInstanceOf(DataMap.class))
                    .getDbEntity(jpaColumn.getTable());

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
            JpaId jpaTargetId = targetEntity.getAttributes().getId(
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

    class JpaEntityVisitor extends NestedVisitor {

        JpaEntityVisitor() {

            BaseTreeVisitor listenersVisitor = new BaseTreeVisitor();
            listenersVisitor.addChildVisitor(
                    JpaEntityListener.class,
                    new JpaEntityListenerVisitor());

            BaseTreeVisitor attributeVisitor = new BaseTreeVisitor();
            attributeVisitor.addChildVisitor(
                    JpaManyToOne.class,
                    new JpaRelationshipVisitor());
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

            JpaVersionVisitor versionVisitor = new JpaVersionVisitor();
            versionVisitor.addChildVisitor(JpaColumn.class, new JpaColumnVisitor());
            attributeVisitor.addChildVisitor(JpaVersion.class, versionVisitor);

            JpaIdVisitor idVisitor = new JpaIdVisitor();
            idVisitor.addChildVisitor(JpaColumn.class, new JpaIdColumnVisitor());
            attributeVisitor.addChildVisitor(JpaId.class, idVisitor);

            // TODO: andrus 8/6/2006 - handle Embedded, EmbeddedId, AttributeOverride

            addChildVisitor(JpaAttributes.class, attributeVisitor);
            addChildVisitor(JpaTable.class, new JpaTableVisitor());
            addChildVisitor(JpaNamedQuery.class, new JpaNamedQueryVisitor());
            addChildVisitor(JpaEntityListeners.class, listenersVisitor);
        }

        @Override
        Object createObject(ProjectPath path) {
            JpaEntity jpaEntity = (JpaEntity) path.getObject();
            ObjEntity cayenneEntity = new ObjEntity(jpaEntity.getName());
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
                cayenneEntity.getCallbackMap().getPrePersist().addCallbackMethod(
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
    }

    class JpaOneToManyVisitor extends JpaRelationshipVisitor {

        @Override
        Object createObject(ProjectPath path) {
            JpaDbRelationship relationship = (JpaDbRelationship) super.createObject(path);

            if (relationship != null) {
                JpaOneToMany jpaRelationship = (JpaOneToMany) path.getObject();
                relationship.setMappedBy(jpaRelationship.getMappedBy());
                
                
                if(jpaRelationship.getMapKey() != null) {
                    
                }
            }
            return relationship;
        }
    }

    class JpaOneToOneVisitor extends JpaRelationshipVisitor {

        @Override
        Object createObject(ProjectPath path) {
            JpaDbRelationship relationship = (JpaDbRelationship) super.createObject(path);

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
            JpaDbRelationship relationship = (JpaDbRelationship) super.createObject(path);

            if (relationship != null) {
                JpaManyToMany jpaRelationship = (JpaManyToMany) path.getObject();
                relationship.setMappedBy(jpaRelationship.getMappedBy());
            }
            return relationship;
        }
    }

    class JpaRelationshipVisitor extends NestedVisitor {

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

            return dbRelationship;
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
                    Class cayenneQueryClass = Class.forName(hint.getValue(), true, Thread
                            .currentThread()
                            .getContextClassLoader());

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

            cayenneEntity.setCatalog(jpaTable.getCatalog());
            cayenneEntity.setSchema(jpaTable.getSchema());

            parentCayenneEntity.setDbEntity(cayenneEntity);
            return cayenneEntity;
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
