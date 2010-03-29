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
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cayenne.jpa.map.JpaAssociationOverride;
import org.apache.cayenne.jpa.map.JpaAttribute;
import org.apache.cayenne.jpa.map.JpaAttributeOverride;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaColumn;
import org.apache.cayenne.jpa.map.JpaEmbedded;
import org.apache.cayenne.jpa.map.JpaEmbeddedId;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaGeneratedValue;
import org.apache.cayenne.jpa.map.JpaId;
import org.apache.cayenne.jpa.map.JpaJoinColumn;
import org.apache.cayenne.jpa.map.JpaJoinTable;
import org.apache.cayenne.jpa.map.JpaManagedClass;
import org.apache.cayenne.jpa.map.JpaManyToMany;
import org.apache.cayenne.jpa.map.JpaManyToOne;
import org.apache.cayenne.jpa.map.JpaOneToMany;
import org.apache.cayenne.jpa.map.JpaOneToOne;
import org.apache.cayenne.jpa.map.JpaTransient;
import org.apache.cayenne.jpa.map.JpaVersion;

/**
 * A factory of member annotation processors.
 * 
 */
class MemberAnnotationProcessorFactory extends AnnotationProcessorFactory {

    // superclass of the top-level member annotations
    abstract static class L1Processor implements AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object parent = context.peek();

            if (parent instanceof JpaManagedClass) {
                onManagedClass((JpaManagedClass) parent, element, context);
            }
            else if (parent instanceof JpaAttribute) {
                onAttribute((JpaAttribute) parent, element, context);
            }
            else {
                recordUnsupportedAnnotation(element, context);
            }
        }

        /**
         * Does nothing by default. Any elements that push themselves on stack in the
         * start method, must pop the stack here.
         */
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
        }

        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        void recordUnsupportedAnnotation(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            context.recordConflict(element, AnnotationProcessorFactory
                    .annotationClass(getClass()), "Unsupported in this context");
        }
    }

    // superclass of the second-level member annotations
    abstract static class L2Processor implements AnnotationProcessor {

        public void onStartElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JpaAttribute attribute = null;

            Object parent = context.peek();
            if (parent instanceof JpaAttribute) {
                attribute = (JpaAttribute) parent;
            }
            else {
                attribute = findOrCreateAttribute(element, parent, context);
            }

            if (parent != null) {
                onAttribute(attribute, element, context);
            }
            else {
                recordUnsupportedAnnotation(element, context);
            }
        }

        JpaAttribute findOrCreateAttribute(
                AnnotatedElement element,
                Object parent,
                AnnotationProcessorStack context) {

            JpaBasic basic = null;

            if (parent instanceof JpaManagedClass) {
                JpaManagedClass managedClass = (JpaManagedClass) parent;
                String name = ((Member) element).getName();
                basic = managedClass.getAttributes().getBasicAttribute(name);
                if (basic == null) {
                    basic = new JpaBasic();

                    // do push/pop as the context does some required injection
                    context.push(basic);
                    context.pop();

                    managedClass.getAttributes().getBasicAttributes().add(basic);
                }
            }

            return basic;
        }

        /**
         * Does nothing by default. Any elements that push themselves on stack in the
         * start method, must pop the stack here.
         */
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
        }

        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            recordUnsupportedAnnotation(element, context);
        }

        void recordUnsupportedAnnotation(
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            context.recordConflict(element, AnnotationProcessorFactory
                    .annotationClass(getClass()), "Unsupported in this context");
        }
    }

    static final class AssociationOverrideProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (managedClass instanceof JpaEntity) {
                JpaAssociationOverride override = new JpaAssociationOverride(element
                        .getAnnotation(AssociationOverride.class));
                ((JpaEntity) managedClass).getAssociationOverrides().add(override);
            }
            else {
                super.onManagedClass(managedClass, element, context);
            }
        }
    }

    static final class AssociationOverridesProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (managedClass instanceof JpaEntity) {
                AssociationOverrides overrides = element
                        .getAnnotation(AssociationOverrides.class);
                for (AssociationOverride overrideAnnotation : overrides.value()) {
                    JpaAssociationOverride override = new JpaAssociationOverride(
                            overrideAnnotation);
                    ((JpaEntity) managedClass).getAssociationOverrides().add(override);
                }
            }
            else {
                super.onManagedClass(managedClass, element, context);
            }
        }
    }

    static final class AttributeOverrideProcessor extends L1Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (attribute instanceof JpaEmbeddedId) {
                JpaAttributeOverride override = new JpaAttributeOverride(element
                        .getAnnotation(AttributeOverride.class));
                ((JpaEmbeddedId) attribute).getAttributeOverrides().add(override);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class AttributeOverridesProcessor extends L1Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (attribute instanceof JpaEmbeddedId) {
                AttributeOverrides overrides = element
                        .getAnnotation(AttributeOverrides.class);

                for (AttributeOverride overrideAnnotation : overrides.value()) {
                    JpaAttributeOverride override = new JpaAttributeOverride(
                            overrideAnnotation);
                    ((JpaEmbeddedId) attribute).getAttributeOverrides().add(override);
                }
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class BasicProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaBasic attribute = new JpaBasic(element.getAnnotation(Basic.class));
            managedClass.getAttributes().getBasicAttributes().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (!(attribute instanceof JpaBasic)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaBasic)) {
                context.push(pop);
            }
        }
    }

    static final class ColumnProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JpaColumn column = new JpaColumn(element.getAnnotation(Column.class));

            if (attribute instanceof JpaBasic) {
                ((JpaBasic) attribute).setColumn(column);
            }
            else if (attribute instanceof JpaVersion) {
                ((JpaVersion) attribute).setColumn(column);
            }
            else if (attribute instanceof JpaId) {
                ((JpaId) attribute).setColumn(column);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class EmbeddedProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaEmbedded attribute = new JpaEmbedded();
            managedClass.getAttributes().getEmbeddedAttributes().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (!(attribute instanceof JpaEmbedded)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaEmbedded)) {
                context.push(pop);
            }
        }
    }

    static final class EmbeddedIdProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaEmbeddedId id = new JpaEmbeddedId();
            managedClass.getAttributes().setEmbeddedId(id);
            context.push(id);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (attribute instanceof JpaEmbeddedId) {
                // was created implictly by another annotation
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaEmbeddedId)) {
                context.push(pop);
            }
        }
    }

    static final class EnumeratedProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (attribute instanceof JpaBasic) {
                EnumType enumType = element.getAnnotation(Enumerated.class).value();
                ((JpaBasic) attribute).setEnumerated(enumType);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class GeneratedValueProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            // can only attach to id
            if (attribute instanceof JpaId) {
                JpaGeneratedValue generated = new JpaGeneratedValue(element
                        .getAnnotation(GeneratedValue.class));
                ((JpaId) attribute).setGeneratedValue(generated);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class IdProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaId attribute = new JpaId();
            managedClass.getAttributes().getIds().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (attribute instanceof JpaId) {
                // id was created implictly by another annotation
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaId)) {
                context.push(pop);
            }
        }
    }

    static final class JoinColumnProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JpaJoinColumn joinColumn = new JpaJoinColumn(element
                    .getAnnotation(JoinColumn.class));

            if (attribute instanceof JpaOneToMany) {
                ((JpaOneToMany) attribute).getJoinColumns().add(joinColumn);
            }
            else if (attribute instanceof JpaOneToOne) {
                ((JpaOneToOne) attribute).getJoinColumns().add(joinColumn);
            }
            else if (attribute instanceof JpaManyToOne) {
                ((JpaManyToOne) attribute).getJoinColumns().add(joinColumn);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class JoinColumnsProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JoinColumn[] annotations = element.getAnnotation(JoinColumns.class).value();
            Collection<JpaJoinColumn> joinColumns = new ArrayList<JpaJoinColumn>(
                    annotations.length);
            for (int i = 0; i < annotations.length; i++) {
                joinColumns.add(new JpaJoinColumn(annotations[i]));
            }

            if (attribute instanceof JpaOneToMany) {
                ((JpaOneToMany) attribute).getJoinColumns().addAll(joinColumns);
            }
            else if (attribute instanceof JpaOneToOne) {
                ((JpaOneToOne) attribute).getJoinColumns().addAll(joinColumns);
            }
            else if (attribute instanceof JpaManyToOne) {
                ((JpaManyToOne) attribute).getJoinColumns().addAll(joinColumns);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class JoinTableProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            JpaJoinTable joinTable = new JpaJoinTable(element
                    .getAnnotation(JoinTable.class));

            if (attribute instanceof JpaOneToMany) {
                ((JpaOneToMany) attribute).setJoinTable(joinTable);
            }
            else if (attribute instanceof JpaOneToOne) {
                ((JpaOneToOne) attribute).setJoinTable(joinTable);
            }
            else if (attribute instanceof JpaManyToOne) {
                ((JpaManyToOne) attribute).setJoinTable(joinTable);
            }
            else if (attribute instanceof JpaManyToMany) {
                ((JpaManyToMany) attribute).setJoinTable(joinTable);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class LobProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (attribute instanceof JpaBasic) {
                ((JpaBasic) attribute).setLob(true);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class ManyToManyProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaManyToMany attribute = new JpaManyToMany(element
                    .getAnnotation(ManyToMany.class));
            managedClass.getAttributes().getManyToManyRelationships().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (!(attribute instanceof JpaManyToMany)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaManyToMany)) {
                context.push(pop);
            }
        }
    }

    static final class ManyToOneProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaManyToOne attribute = new JpaManyToOne(element
                    .getAnnotation(ManyToOne.class));
            managedClass.getAttributes().getManyToOneRelationships().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (!(attribute instanceof JpaManyToMany)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaManyToOne)) {
                context.push(pop);
            }
        }
    }

    static final class MapKeyProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            String key = element.getAnnotation(MapKey.class).name();
            if (attribute instanceof JpaOneToMany) {
                ((JpaOneToMany) attribute).setMapKey(key);
            }
            else if (attribute instanceof JpaManyToMany) {
                ((JpaManyToMany) attribute).setMapKey(key);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class OneToManyProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaOneToMany attribute = new JpaOneToMany(element
                    .getAnnotation(OneToMany.class));
            managedClass.getAttributes().getOneToManyRelationships().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (!(attribute instanceof JpaOneToMany)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaOneToMany)) {
                context.push(pop);
            }
        }
    }

    static final class OneToOneProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaOneToOne attribute = new JpaOneToOne(element.getAnnotation(OneToOne.class));
            managedClass.getAttributes().getOneToOneRelationships().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            if (!(attribute instanceof JpaOneToOne)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaOneToOne)) {
                context.push(pop);
            }
        }
    }

    static final class OrderByProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            String order = element.getAnnotation(OrderBy.class).value();
            if (attribute instanceof JpaOneToMany) {
                ((JpaOneToMany) attribute).setOrderBy(order);
            }
            else if (attribute instanceof JpaManyToMany) {
                ((JpaManyToMany) attribute).setOrderBy(order);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class TemporalProcessor extends L2Processor {

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            TemporalType value = element.getAnnotation(Temporal.class).value();

            if (attribute instanceof JpaBasic) {
                ((JpaBasic) attribute).setTemporal(value);
            }
            // according to the spec, only @Basic is compatibel with @Temporal, however
            // @Id and @Version also support it in the schema
            else if (attribute instanceof JpaId) {
                ((JpaId) attribute).setTemporal(value);
            }
            else if (attribute instanceof JpaVersion) {
                ((JpaVersion) attribute).setTemporal(value);
            }
            else {
                super.onAttribute(attribute, element, context);
            }
        }
    }

    static final class TransientProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaTransient attribute = new JpaTransient();
            managedClass.getAttributes().getTransientAttributes().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (!(attribute instanceof JpaTransient)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaTransient)) {
                context.push(pop);
            }
        }
    }

    static final class VersionProcessor extends L1Processor {

        @Override
        void onManagedClass(
                JpaManagedClass managedClass,
                AnnotatedElement element,
                AnnotationProcessorStack context) {
            JpaVersion attribute = new JpaVersion();
            managedClass.getAttributes().getVersionAttributes().add(attribute);
            context.push(attribute);
        }

        @Override
        void onAttribute(
                JpaAttribute attribute,
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            if (!(attribute instanceof JpaVersion)) {
                super.onAttribute(attribute, element, context);
            }
        }

        @Override
        public void onFinishElement(
                AnnotatedElement element,
                AnnotationProcessorStack context) {

            Object pop = context.pop();
            if (!(pop instanceof JpaVersion)) {
                context.push(pop);
            }
        }
    }
}
