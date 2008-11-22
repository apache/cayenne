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

package org.apache.cayenne.jpa.conf;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;

import javax.persistence.DiscriminatorType;
import javax.persistence.EnumType;
import javax.persistence.InheritanceType;
import javax.persistence.TemporalType;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaAbstractEntity;
import org.apache.cayenne.jpa.map.JpaAttribute;
import org.apache.cayenne.jpa.map.JpaAttributes;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.map.JpaColumn;
import org.apache.cayenne.jpa.map.JpaDiscriminatorColumn;
import org.apache.cayenne.jpa.map.JpaEmbeddable;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaId;
import org.apache.cayenne.jpa.map.JpaJoinColumn;
import org.apache.cayenne.jpa.map.JpaManagedClass;
import org.apache.cayenne.jpa.map.JpaManyToMany;
import org.apache.cayenne.jpa.map.JpaManyToOne;
import org.apache.cayenne.jpa.map.JpaMappedSuperclass;
import org.apache.cayenne.jpa.map.JpaOneToMany;
import org.apache.cayenne.jpa.map.JpaOneToOne;
import org.apache.cayenne.jpa.map.JpaPrimaryKeyJoinColumn;
import org.apache.cayenne.jpa.map.JpaPropertyDescriptor;
import org.apache.cayenne.jpa.map.JpaRelationship;
import org.apache.cayenne.jpa.map.JpaSecondaryTable;
import org.apache.cayenne.jpa.map.JpaTable;
import org.apache.cayenne.jpa.map.JpaVersion;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.BaseTreeVisitor;
import org.apache.cayenne.util.HierarchicalTreeVisitor;
import org.apache.cayenne.util.TraversalUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.SimpleValidationFailure;

/**
 * Initializes JPA specification compatible mapping defaults.
 * 
 */
public class EntityMapDefaultsProcessor {

    protected HierarchicalTreeVisitor visitor;
    protected transient EntityMapLoaderContext context;

    public void applyDefaults(EntityMapLoaderContext context) throws JpaProviderException {
        this.context = context;

        if (visitor == null) {
            visitor = createVisitor();
        }

        TraversalUtil.traverse(context.getEntityMap(), visitor);
    }

    /**
     * Creates a stateless instance of the JpaEntityMap traversal visitor. This method is
     * lazily invoked and cached by this object.
     */
    protected HierarchicalTreeVisitor createVisitor() {
        return new EntityMapVisitor();
    }

    abstract class AbstractEntityVisitor extends BaseTreeVisitor {

        AbstractEntityVisitor() {
            BaseTreeVisitor attributeVisitor = new BaseTreeVisitor();
            attributeVisitor.addChildVisitor(JpaId.class, new IdVisitor());
            attributeVisitor.addChildVisitor(JpaBasic.class, new BasicVisitor());
            attributeVisitor.addChildVisitor(JpaVersion.class, new VersionVisitor());
            attributeVisitor.addChildVisitor(JpaManyToOne.class, new ManyToOneVisitor());
            attributeVisitor.addChildVisitor(JpaOneToOne.class, new OneToOneVisitor());
            attributeVisitor.addChildVisitor(
                    JpaOneToMany.class,
                    new RelationshipVisitor());
            attributeVisitor.addChildVisitor(
                    JpaManyToMany.class,
                    new RelationshipVisitor());

            addChildVisitor(JpaAttributes.class, attributeVisitor);
            addChildVisitor(JpaId.class, new IdVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaAbstractEntity abstractEntity = (JpaAbstractEntity) path.getObject();

            // * entity name
            if (abstractEntity.getClassName() == null) {
                return false;
            }

            if (abstractEntity.getAttributes() == null) {
                abstractEntity.setAttributes(new JpaAttributes());
            }

            // * default persistent fields
            JpaClassDescriptor descriptor = abstractEntity.getClassDescriptor();

            AccessType access = abstractEntity.getAccess();
            if (access == null) {
                access = ((JpaEntityMap) path.getRoot()).getAccess();
                abstractEntity.setAccess(access);
            }

            if (access == AccessType.PROPERTY) {
                for (JpaPropertyDescriptor candidate : descriptor
                        .getPropertyDescriptors()) {
                    processProperty(abstractEntity, descriptor, candidate);
                }
            }
            // field is default...
            else {
                for (JpaPropertyDescriptor candidate : descriptor.getFieldDescriptors()) {
                    processProperty(abstractEntity, descriptor, candidate);
                }
            }

            return true;
        }

        void processProperty(
                JpaAbstractEntity entity,
                JpaClassDescriptor descriptor,
                JpaPropertyDescriptor property) {

            JpaAttributes attributes = entity.getAttributes();
            if (attributes.getAttribute(property.getName()) != null) {
                return;
            }

            if (property.isDefaultNonRelationalType()) {

                JpaBasic attribute = new JpaBasic();

                attribute.setPropertyDescriptor(property);
                attribute.setName(property.getName());
                attributes.getBasicAttributes().add(attribute);
            }
            else {
                String path = descriptor.getManagedClass().getName()
                        + "."
                        + property.getName();
                context.recordConflict(new SimpleValidationFailure(
                        property.getMember(),
                        "Undefined property persistence status: " + path));
            }
        }
    }

    class EmbeddableBasicVisitor extends BaseTreeVisitor {

        EmbeddableBasicVisitor() {
            addChildVisitor(JpaColumn.class, new ColumnVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaBasic jpaBasic = (JpaBasic) path.getObject();
            if (jpaBasic.getColumn() == null) {
                JpaColumn column = new JpaColumn(AnnotationPrototypes.getColumn());
                column.setName(jpaBasic.getName());
                column.setNullable(jpaBasic.isOptional());
                jpaBasic.setColumn(column);
            }

            return true;
        }
    }

    class BasicVisitor extends BaseTreeVisitor {

        BasicVisitor() {
            addChildVisitor(JpaColumn.class, new ColumnVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaBasic jpaBasic = (JpaBasic) path.getObject();
            if (jpaBasic.getColumn() == null) {
                JpaColumn column = new JpaColumn(AnnotationPrototypes.getColumn());
                column.setName(jpaBasic.getName());
                column.setNullable(jpaBasic.isOptional());
                jpaBasic.setColumn(column);
            }

            JpaAbstractEntity entity = (JpaAbstractEntity) path
                    .firstInstanceOf(JpaAbstractEntity.class);

            // process temporal type defaults
            if (jpaBasic.getTemporal() == null && jpaBasic.getEnumerated() == null) {
                JpaClassDescriptor descriptor = entity.getClassDescriptor();
                JpaPropertyDescriptor property = descriptor.getProperty(jpaBasic
                        .getName());

                // sanity check
                if (property == null) {
                    throw new IllegalStateException("No class property found for name: "
                            + jpaBasic.getName());
                }

                if (java.sql.Date.class.isAssignableFrom(property.getType())) {
                    jpaBasic.setTemporal(TemporalType.DATE);
                }
                else if (Time.class.isAssignableFrom(property.getType())) {
                    jpaBasic.setTemporal(TemporalType.TIME);
                }
                else if (Timestamp.class.isAssignableFrom(property.getType())) {
                    jpaBasic.setTemporal(TemporalType.TIMESTAMP);
                }
                else if (Date.class.isAssignableFrom(property.getType())) {
                    jpaBasic.setTemporal(TemporalType.TIMESTAMP);
                }
                else if (property.getType().isEnum()) {
                    jpaBasic.setEnumerated(EnumType.ORDINAL);
                }
            }

            return true;
        }
    }

    class VersionVisitor extends BasicVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaVersion jpaBasic = (JpaVersion) path.getObject();
            if (jpaBasic.getColumn() == null) {
                JpaColumn column = new JpaColumn(AnnotationPrototypes.getColumn());
                column.setName(jpaBasic.getName());
                jpaBasic.setColumn(column);
            }

            if (jpaBasic.getTemporal() == null) {
                JpaEntity entity = (JpaEntity) path.firstInstanceOf(JpaEntity.class);
                JpaClassDescriptor descriptor = entity.getClassDescriptor();
                JpaPropertyDescriptor property = descriptor.getProperty(jpaBasic
                        .getName());

                if (Date.class.equals(property.getType())) {
                    jpaBasic.setTemporal(TemporalType.TIMESTAMP);
                }
            }

            return true;
        }
    }

    final class DiscriminatorColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {

            JpaDiscriminatorColumn column = (JpaDiscriminatorColumn) path.getObject();
            if (column.getName() == null) {
                column.setName("DTYPE");
            }

            if (column.getDiscriminatorType() == null) {
                column.setDiscriminatorType(DiscriminatorType.STRING);
            }

            if (column.getLength() == 0
                    && column.getDiscriminatorType() == DiscriminatorType.STRING) {
                column.setLength(31);
            }

            return false;
        }
    }

    final class ColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaColumn column = (JpaColumn) path.getObject();

            JpaAttribute parent = path.firstInstanceOf(JpaAttribute.class);

            if (column.getName() == null) {
                column.setName(parent.getName());
            }

            if (column.getTable() == null) {
                JpaEntity entity = path.firstInstanceOf(JpaEntity.class);

                // parent can be a mapped superclass
                if (entity != null) {
                    column.setTable(entity.lookupTable().getName());
                }
            }

            if (parent.getPropertyDescriptor().isStringType()) {
                if (column.getLength() <= 0) {
                    column.setLength(JpaColumn.DEFAULT_LENGTH);
                }
            }
            else {
                // length for non-string types should be ignored...
                column.setLength(-1);
            }

            return true;
        }
    }

    final class EntityMapVisitor extends BaseTreeVisitor {

        EntityMapVisitor() {
            addChildVisitor(JpaEntity.class, new EntityVisitor());
            addChildVisitor(JpaMappedSuperclass.class, new MappedSuperclassVisitor());
            addChildVisitor(JpaEmbeddable.class, new EmbeddableVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaEntityMap entityMap = (JpaEntityMap) path.getObject();

            // TODO: andrus, 4/28/2006 - actually we need to analyze preloaded classes and
            // see how they were annotated to choose the right access type...

            entityMap.setAccess(AccessType.FIELD);

            return true;
        }
    }

    final class EmbeddableVisitor extends BaseTreeVisitor {

        EmbeddableVisitor() {
            BaseTreeVisitor attributeVisitor = new BaseTreeVisitor();
            attributeVisitor
                    .addChildVisitor(JpaBasic.class, new EmbeddableBasicVisitor());
            addChildVisitor(JpaAttributes.class, attributeVisitor);
        }
    }

    final class EntityVisitor extends AbstractEntityVisitor {

        EntityVisitor() {
            addChildVisitor(
                    JpaDiscriminatorColumn.class,
                    new DiscriminatorColumnVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {
            if (super.onStartNode(path)) {

                JpaEntity entity = (JpaEntity) path.getObject();

                if (entity.getInheritance() != null
                        && entity.getInheritance().getStrategy() == null) {
                    entity.getInheritance().setStrategy(InheritanceType.SINGLE_TABLE);
                }

                if (entity.getName() == null) {
                    // use unqualified class name
                    String fqName = entity.getClassName();
                    int split = fqName.lastIndexOf('.');
                    entity.setName(split > 0 ? fqName.substring(split + 1) : fqName);
                }

                if (entity.getInheritance() == null
                        && entity.lookupInheritanceStrategy() == InheritanceType.SINGLE_TABLE) {
                    // no dedicated table for the single_table inheritance subclass
                }
                // default table (see @Table annotation defaults, JPA spec 9.1.1)
                else if (entity.getTable() == null) {
                    JpaTable table = new JpaTable(AnnotationPrototypes.getTable());

                    // unclear whether we need to apply any other name transformations
                    // ...
                    // or even if we need to uppercase the name. Per default examples
                    // looks
                    // like we need. table.setName(entity.getName().toUpperCase());
                    table.setName(entity.getName());
                    entity.setTable(table);
                }

                return true;
            }

            return false;
        }

        @Override
        public void onFinishNode(ProjectPath path) {

            // now that attributes are parsed, we can fill in secondary table joins that
            // may depend on previous entity id column processing.

            JpaEntity entity = (JpaEntity) path.getObject();
            for (JpaSecondaryTable table : entity.getSecondaryTables()) {

                if (table.getPrimaryKeyJoinColumns().isEmpty()) {

                    for (JpaId id : entity.getAttributes().getIds()) {
                        JpaPrimaryKeyJoinColumn joinColumn = new JpaPrimaryKeyJoinColumn();
                        joinColumn.setName(id.getColumn().getName());
                        joinColumn.setReferencedColumnName(joinColumn.getName());
                        table.getPrimaryKeyJoinColumns().add(joinColumn);
                    }
                }
                else {
                    for (JpaPrimaryKeyJoinColumn joinColumn : table
                            .getPrimaryKeyJoinColumns()) {

                        if (joinColumn.getReferencedColumnName() == null) {
                            if (entity.getAttributes().getIds().size() == 1) {
                                JpaId id = entity
                                        .getAttributes()
                                        .getIds()
                                        .iterator()
                                        .next();
                                joinColumn.setReferencedColumnName(id
                                        .getColumn()
                                        .getName());
                            }
                        }
                    }
                }
            }

            JpaDiscriminatorColumn discriminator = entity.lookupDiscriminatorColumn();
            if (discriminator != null) {

                if (entity.getDiscriminatorValue() == null) {
                    switch (discriminator.getDiscriminatorType()) {

                        case STRING:
                            entity.setDiscriminatorValue(entity.getName());
                            break;
                        default:
                            context.recordConflict(new SimpleValidationFailure(
                                    entity,
                                    "Can't guess default discriminator value for non-String discriminator column: "
                                            + discriminator.getName()));
                    }
                }
            }
        }
    }

    final class IdVisitor extends BaseTreeVisitor {

        IdVisitor() {
            addChildVisitor(JpaColumn.class, new ColumnVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaId id = (JpaId) path.getObject();

            if (id.getColumn() == null) {

                JpaColumn column = new JpaColumn(AnnotationPrototypes.getColumn());
                column.setName(id.getName());

                JpaEntity entity = (JpaEntity) path.firstInstanceOf(JpaEntity.class);
                column.setTable(entity.getTable().getName());
                id.setColumn(column);
            }

            return true;
        }
    }

    final class JoinColumnVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {
            JpaRelationship relationship = (JpaRelationship) path.getObjectParent();
            JpaJoinColumn column = (JpaJoinColumn) path.getObject();

            if (column.getTable() == null) {
                JpaEntity entity = path.firstInstanceOf(JpaEntity.class);
                column.setTable(entity.getTable().getName());
            }

            // JPA Spec, 2.1.8.2 (same for all relationship owners):
            // The following mapping defaults apply: [...]
            // Table A contains a foreign key to table B. The foreign key column
            // name is formed as the concatenation of the following: the name of
            // the relationship property or field of entityA; "_" ; the name of
            // the primary key column in table B. The foreign key column has the
            // same type as the primary key of table B.

            JpaEntityMap map = path.firstInstanceOf(JpaEntityMap.class);
            JpaEntity target = map.entityForClass(relationship.getTargetEntityName());

            if (target == null) {
                context.recordConflict(new SimpleValidationFailure(
                        relationship,
                        "Invalid relationship target "
                                + relationship.getTargetEntityName()));
            }
            else if (target.getAttributes() == null
                    || target.getAttributes().getIds().isEmpty()) {
                context.recordConflict(new SimpleValidationFailure(
                        target,
                        "Relationship target has no PK defined: "
                                + relationship.getTargetEntityName()));
            }
            else if (target.getAttributes().getIds().size() > 1) {
                // TODO: andrus, 4/30/2006 implement this; note that instead of
                // checking for "attribute.getJoinColumns().isEmpty()" above,
                // we'll have to match individual columns
                context.recordConflict(new SimpleValidationFailure(
                        relationship,
                        "Defaults for compound FK are not implemented."));
            }
            else {
                JpaId id = target.getAttributes().getIds().iterator().next();

                String pkName = id.getColumn() != null ? id.getColumn().getName() : id
                        .getName();
                column.setName(relationship.getName() + '_' + pkName);
                column.setReferencedColumnName(id.getColumn() != null ? id
                        .getColumn()
                        .getName() : id.getName());
            }

            return true;
        }
    }

    final class MappedSuperclassVisitor extends AbstractEntityVisitor {

    }

    final class ManyToOneVisitor extends RelationshipVisitor {

        ManyToOneVisitor() {
            addChildVisitor(JpaJoinColumn.class, new JoinColumnVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {

            if (!super.onStartNode(path)) {
                return false;
            }

            JpaManyToOne manyToOne = (JpaManyToOne) path.getObject();
            Collection<JpaJoinColumn> joinColumns = manyToOne.getJoinColumns();
            if (joinColumns.isEmpty()) {
                joinColumns.add(new JpaJoinColumn(AnnotationPrototypes.getJoinColumn()));
            }

            return true;
        }
    }

    final class OneToOneVisitor extends RelationshipVisitor {

        OneToOneVisitor() {
            addChildVisitor(JpaJoinColumn.class, new JoinColumnVisitor());
        }

        @Override
        public boolean onStartNode(ProjectPath path) {

            if (!super.onStartNode(path)) {
                return false;
            }

            JpaOneToOne oneToOne = (JpaOneToOne) path.getObject();
            Collection<JpaJoinColumn> joinColumns = oneToOne.getJoinColumns();
            String mappedBy = oneToOne.getMappedBy();
            if (joinColumns.isEmpty() && mappedBy == null) {
                joinColumns.add(new JpaJoinColumn(AnnotationPrototypes.getJoinColumn()));
            }

            return true;
        }
    }

    class RelationshipVisitor extends BaseTreeVisitor {

        @Override
        public boolean onStartNode(ProjectPath path) {

            JpaRelationship relationship = (JpaRelationship) path.getObject();
            if (Util.isEmptyString(relationship.getTargetEntityName())) {

                JpaManagedClass relationshipOwner = (JpaManagedClass) path
                        .firstInstanceOf(JpaManagedClass.class);

                String name = relationship.getName();

                JpaClassDescriptor srcDescriptor = relationshipOwner.getClassDescriptor();
                JpaPropertyDescriptor property = srcDescriptor.getProperty(name);

                Class<?> targetEntityType = property.getTargetEntityType();

                if (targetEntityType == null) {
                    context.recordConflict(new SimpleValidationFailure(property
                            .getMember(), "Undefined target entity type: " + name));
                    return false;
                }
                else {
                    relationship.setTargetEntityName(targetEntityType.getName());
                }
            }

            return true;
        }
    }
}
