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

import java.lang.reflect.AnnotatedElement;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.cayenne.jpa.map.JpaAttributeOverride;
import org.apache.cayenne.jpa.map.JpaAttributes;
import org.apache.cayenne.jpa.map.JpaDiscriminatorColumn;
import org.apache.cayenne.jpa.map.JpaEmbeddable;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityListener;
import org.apache.cayenne.jpa.map.JpaEntityListeners;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaIdClass;
import org.apache.cayenne.jpa.map.JpaInheritance;
import org.apache.cayenne.jpa.map.JpaMappedSuperclass;
import org.apache.cayenne.jpa.map.JpaNamedNativeQuery;
import org.apache.cayenne.jpa.map.JpaNamedQuery;
import org.apache.cayenne.jpa.map.JpaPrimaryKeyJoinColumn;
import org.apache.cayenne.jpa.map.JpaSecondaryTable;
import org.apache.cayenne.jpa.map.JpaSequenceGenerator;
import org.apache.cayenne.jpa.map.JpaSqlResultSetMapping;
import org.apache.cayenne.jpa.map.JpaTable;
import org.apache.cayenne.jpa.map.JpaTableGenerator;
import org.apache.cayenne.util.Util;

/**
 * A factory of class annotation processors.
 * 
 */
class ClassAnnotationProcessorFactory extends AnnotationProcessorFactory {

    static final class EntityProcessor implements AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            Entity entityAnnotation = element.getAnnotation(Entity.class);

            JpaEntity entity = new JpaEntity();
            entity.setClassName(((Class<?>) element).getName());
            entity.setAttributes(new JpaAttributes());

            if (!Util.isEmptyString(entityAnnotation.name())) {
                entity.setName(entityAnnotation.name());
            }

            context.push(entity);
        }

        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaEntity entity = (JpaEntity) context.pop();
            JpaEntityMap entityMap = (JpaEntityMap) context.peek();
            entityMap.getEntities().add(entity);
        }
    }

    static final class EmbeddableProcessor implements AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JpaEmbeddable embeddable = new JpaEmbeddable();
            embeddable.setClassName(((Class<?>) element).getName());
            embeddable.setAttributes(new JpaAttributes());
            context.push(embeddable);
        }

        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaEmbeddable embeddable = (JpaEmbeddable) context.pop();
            JpaEntityMap entityMap = (JpaEntityMap) context.peek();
            entityMap.getEmbeddables().add(embeddable);
        }
    }

    static final class MappedSuperclassProcessor implements AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JpaMappedSuperclass superclass = new JpaMappedSuperclass();
            superclass.setClassName(((Class<?>) element).getName());
            superclass.setAttributes(new JpaAttributes());
            context.push(superclass);
        }

        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaMappedSuperclass superclass = (JpaMappedSuperclass) context.pop();
            JpaEntityMap entityMap = (JpaEntityMap) context.peek();
            entityMap.getMappedSuperclasses().add(superclass);
        }
    }

    /**
     * A superclass of all processors that for class annotations that do not define the
     * type of the mapping.
     */
    abstract static class AbstractChildProcessor implements AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object parent = context.peek();

            if (parent instanceof JpaEntity) {
                onEntity((JpaEntity) parent, element, context);
            }
            else if (parent instanceof JpaMappedSuperclass) {
                onMappedSuperclass((JpaMappedSuperclass) parent, element, context);
            }
            else if (parent instanceof JpaEntityMap) {
                onEntityMap((JpaEntityMap) parent, element, context);
            }
            else if (parent instanceof JpaEmbeddable) {
                onEmbeddable((JpaEmbeddable) parent, element, context);
            }
            else {
                recordUnsupportedAnnotation(element, context);
            }
        }

        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            // noop
        }

        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        void onMappedSuperclass(
                JpaMappedSuperclass superclass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        void onEmbeddable(
                JpaEmbeddable embeddable,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        // not sure if this is compatible with the spec, but such annotations are
        // possible...
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        void recordUnsupportedAnnotation(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            context.recordConflict(element, AnnotationProcessorFactory
                    .annotationClass(getClass()), "Unsupported in this place");
        }
    }

    static final class AttributeOverrideProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            AttributeOverride annotation = element.getAnnotation(AttributeOverride.class);
            entity.getAttributeOverrides().add(new JpaAttributeOverride(annotation));
        }
    }

    static final class AttributeOverridesProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            AttributeOverrides annotation = element
                    .getAnnotation(AttributeOverrides.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entity.getAttributeOverrides().add(
                        new JpaAttributeOverride(annotation.value()[i]));
            }
        }
    }

    static final class DiscriminatorColumnProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            DiscriminatorColumn annotation = element
                    .getAnnotation(DiscriminatorColumn.class);
            entity.setDiscriminatorColumn(new JpaDiscriminatorColumn(annotation));
        }
    }

    static final class DiscriminatorValueProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            DiscriminatorValue annotation = element
                    .getAnnotation(DiscriminatorValue.class);
            entity.setDiscriminatorValue(annotation.value());
        }
    }

    static final class EntityListenersProcessor extends AbstractChildProcessor {

        private EntityListenerAnnotationLoader listenerLoader;

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            EntityListeners annotation = element.getAnnotation(EntityListeners.class);

            if (annotation.value().length > 0) {
                if (listenerLoader == null) {
                    listenerLoader = new EntityListenerAnnotationLoader();
                }

                JpaEntityListeners listenerHolder = entity.getEntityListeners();
                if (listenerHolder == null) {
                    listenerHolder = new JpaEntityListeners();
                    entity.setEntityListeners(listenerHolder);
                }

                for (int i = 0; i < annotation.value().length; i++) {
                    JpaEntityListener listener = listenerLoader
                            .getEntityListener(annotation.value()[i]);
                    if (listener != null) {
                        listenerHolder.getEntityListeners().add(listener);
                    }
                }
            }
        }

        @Override
        void onMappedSuperclass(
                JpaMappedSuperclass superclass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            EntityListeners annotation = element.getAnnotation(EntityListeners.class);

            if (annotation.value().length > 0) {
                if (listenerLoader == null) {
                    listenerLoader = new EntityListenerAnnotationLoader();
                }

                JpaEntityListeners listenerHolder = superclass.getEntityListeners();
                if(listenerHolder  == null) {
                    listenerHolder = new JpaEntityListeners();
                    superclass.setEntityListeners(listenerHolder);
                }

                for (int i = 0; i < annotation.value().length; i++) {
                    JpaEntityListener listener = listenerLoader
                            .getEntityListener(annotation.value()[i]);
                    if (listener != null) {
                        listenerHolder.getEntityListeners().add(listener);
                    }
                }
            }
        }
    }

    static final class ExcludeDefaultListenersProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            entity.setExcludeDefaultListeners(true);
        }

        @Override
        void onMappedSuperclass(
                JpaMappedSuperclass superclass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            superclass.setExcludeDefaultListeners(true);
        }
    }

    static final class ExcludeSuperclassListenersProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            entity.setExcludeSuperclassListeners(true);
        }

        @Override
        void onMappedSuperclass(
                JpaMappedSuperclass superclass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            superclass.setExcludeSuperclassListeners(true);
        }
    }

    static final class IdClassProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            IdClass annotation = element.getAnnotation(IdClass.class);
            JpaIdClass idClass = new JpaIdClass();
            idClass.setClassName(annotation.value().getName());
            entity.setIdClass(idClass);
        }

        @Override
        void onMappedSuperclass(
                JpaMappedSuperclass superclass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            IdClass annotation = element.getAnnotation(IdClass.class);
            JpaIdClass idClass = new JpaIdClass();
            idClass.setClassName(annotation.value().getName());
            superclass.setIdClass(idClass);
        }
    }

    static final class InheritanceProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Inheritance annotation = element.getAnnotation(Inheritance.class);
            entity.setInheritance(new JpaInheritance(annotation));
        }
    }

    static final class NamedNativeQueriesProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedNativeQueries annotation = element
                    .getAnnotation(NamedNativeQueries.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entity.getNamedNativeQueries().add(
                        new JpaNamedNativeQuery(annotation.value()[i]));
            }
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedNativeQueries annotation = element
                    .getAnnotation(NamedNativeQueries.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entityMap.getNamedNativeQueries().add(
                        new JpaNamedNativeQuery(annotation.value()[i]));
            }
        }
    }

    static final class NamedNativeQueryProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedNativeQuery annotation = element.getAnnotation(NamedNativeQuery.class);
            entity.getNamedNativeQueries().add(new JpaNamedNativeQuery(annotation));
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedNativeQuery annotation = element.getAnnotation(NamedNativeQuery.class);
            entityMap.getNamedNativeQueries().add(new JpaNamedNativeQuery(annotation));
        }
    }

    static final class NamedQueriesProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedQueries annotation = element.getAnnotation(NamedQueries.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entity.getNamedQueries().add(new JpaNamedQuery(annotation.value()[i]));
            }
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedQueries annotation = element.getAnnotation(NamedQueries.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entityMap.getNamedQueries().add(new JpaNamedQuery(annotation.value()[i]));
            }
        }
    }

    static final class NamedQueryProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedQuery annotation = element.getAnnotation(NamedQuery.class);
            entity.getNamedQueries().add(new JpaNamedQuery(annotation));
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            NamedQuery annotation = element.getAnnotation(NamedQuery.class);
            entityMap.getNamedQueries().add(new JpaNamedQuery(annotation));
        }
    }

    static final class PrimaryKeyJoinColumnProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            PrimaryKeyJoinColumn annotation = element
                    .getAnnotation(PrimaryKeyJoinColumn.class);
            entity
                    .getPrimaryKeyJoinColumns()
                    .add(new JpaPrimaryKeyJoinColumn(annotation));
        }
    }

    static final class PrimaryKeyJoinColumnsProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            PrimaryKeyJoinColumns annotation = element
                    .getAnnotation(PrimaryKeyJoinColumns.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entity.getPrimaryKeyJoinColumns().add(
                        new JpaPrimaryKeyJoinColumn(annotation.value()[i]));
            }
        }
    }

    static final class SecondaryTableProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            SecondaryTable annotation = element.getAnnotation(SecondaryTable.class);
            entity.getSecondaryTables().add(new JpaSecondaryTable(annotation));
        }
    }

    static final class SecondaryTablesProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            SecondaryTables annotation = element.getAnnotation(SecondaryTables.class);
            for (int i = 0; i < annotation.value().length; i++) {
                entity.getSecondaryTables().add(
                        new JpaSecondaryTable(annotation.value()[i]));
            }
        }
    }

    static final class SequenceGeneratorProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            SequenceGenerator annotation = element.getAnnotation(SequenceGenerator.class);
            entity.setSequenceGenerator(new JpaSequenceGenerator(annotation));
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            SequenceGenerator annotation = element.getAnnotation(SequenceGenerator.class);
            entityMap.getSequenceGenerators().add(new JpaSequenceGenerator(annotation));
        }
    }

    static final class SqlResultSetMappingProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            SqlResultSetMapping annotation = element
                    .getAnnotation(SqlResultSetMapping.class);
            entity.getSqlResultSetMappings().add(new JpaSqlResultSetMapping(annotation));
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            SqlResultSetMapping annotation = element
                    .getAnnotation(SqlResultSetMapping.class);
            entityMap.getSqlResultSetMappings().add(
                    new JpaSqlResultSetMapping(annotation));
        }
    }

    static final class TableGeneratorProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            TableGenerator annotation = element.getAnnotation(TableGenerator.class);
            entity.setTableGenerator(new JpaTableGenerator(annotation));
        }

        @Override
        void onEntityMap(
                JpaEntityMap entityMap,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            TableGenerator annotation = element.getAnnotation(TableGenerator.class);
            entityMap.getTableGenerators().add(new JpaTableGenerator(annotation));
        }
    }

    static final class TableProcessor extends AbstractChildProcessor {

        @Override
        void onEntity(
                JpaEntity entity,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Table annotation = element.getAnnotation(Table.class);
            entity.setTable(new JpaTable(annotation));
        }
    }

}
