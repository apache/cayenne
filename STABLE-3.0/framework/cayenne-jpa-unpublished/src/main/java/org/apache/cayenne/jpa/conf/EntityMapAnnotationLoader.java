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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaAbstractEntity;
import org.apache.cayenne.jpa.map.JpaAttribute;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.map.JpaManagedClass;
import org.apache.cayenne.jpa.map.JpaPropertyDescriptor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.SimpleValidationFailure;

/**
 * {@link org.apache.cayenne.jpa.map.JpaEntityMap} loader that reads mapping information
 * from the class annotations per JPA specification.
 * <h3>Specification Documentation, persistence_1_0.xsd, "class" element.</h3>
 * <p>
 * [Each managed class] should be annotated with either \@Entity, \@Embeddable or
 * \@MappedSuperclass
 * 
 */
public class EntityMapAnnotationLoader {

    static final Map<String, Integer> TYPE_ANNOTATION_ORDERING_WEIGHTS;
    static final Map<String, Integer> MEMBER_ANNOTATION_ORDERING_WEIGHTS;

    static {

        TYPE_ANNOTATION_ORDERING_WEIGHTS = new HashMap<String, Integer>();

        // annotations that are top-level only
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(Entity.class.getName(), 1);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(Embeddable.class.getName(), 1);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(MappedSuperclass.class.getName(), 1);

        // annotations that can be a part of Entity or EntityMap
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(SequenceGenerator.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(NamedNativeQueries.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(NamedNativeQuery.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(NamedQueries.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(NamedQuery.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(SqlResultSetMapping.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(TableGenerator.class.getName(), 2);
        TYPE_ANNOTATION_ORDERING_WEIGHTS.put(EntityListeners.class.getName(), 2);

        MEMBER_ANNOTATION_ORDERING_WEIGHTS = new HashMap<String, Integer>();

        // first level of member annotations - annotations representing different types of
        // attributes
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Id.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Basic.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(EmbeddedId.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Version.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(ManyToOne.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(OneToMany.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(OneToOne.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(ManyToMany.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Embedded.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Transient.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(AssociationOverride.class.getName(), 1);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(AssociationOverrides.class.getName(), 1);

        // second level - attribute overrides (can belong to Embedded or can be a part of
        // the entity )
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(AttributeOverride.class.getName(), 2);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(AttributeOverrides.class.getName(), 2);

        // third level of member annotations - details implying one of the attributes
        // above
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(GeneratedValue.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Temporal.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(TableGenerator.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(SequenceGenerator.class.getName(), 3);

        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Lob.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Temporal.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Enumerated.class.getName(), 3);

        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(MapKey.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(OrderBy.class.getName(), 3);
        MEMBER_ANNOTATION_ORDERING_WEIGHTS.put(Column.class.getName(), 3);
    }

    protected EntityMapLoaderContext context;

    protected Comparator<Annotation> typeAnnotationsSorter;
    protected Comparator<Annotation> memberAnnotationsSorter;

    protected AnnotationProcessorFactory classProcessorFactory;
    protected AnnotationProcessorFactory memberProcessorFactory;
    protected AnnotationProcessorFactory callbackProcessorFactory;

    public EntityMapAnnotationLoader(EntityMapLoaderContext context) {
        this.context = context;
        this.typeAnnotationsSorter = new AnnotationSorter(
                TYPE_ANNOTATION_ORDERING_WEIGHTS);
        this.memberAnnotationsSorter = new AnnotationSorter(
                MEMBER_ANNOTATION_ORDERING_WEIGHTS);

        this.classProcessorFactory = new ClassAnnotationProcessorFactory();
        this.memberProcessorFactory = new MemberAnnotationProcessorFactory();
        this.callbackProcessorFactory = new EntityCallbackAnnotationProcessorFactory();
    }

    /**
     * Processes annotations of a single Java class, loading ORM mapping information to
     * the provided entity map.
     */
    public void loadClassMapping(Class<?> managedClass) throws JpaProviderException {

        // avoid duplicates loaded from annotations per CAY-756
        if (context.getEntityMap().getManagedClass(managedClass.getName()) != null) {
            context.recordConflict(new SimpleValidationFailure(
                    managedClass.getName(),
                    "Duplicate managed class declaration " + managedClass.getName()));
            return;
        }

        Annotation[] classAnnotations = managedClass.getAnnotations();

        // per 'getAnnotations' docs, array is returned by copy, so we can modify it...
        Arrays.sort(classAnnotations, typeAnnotationsSorter);

        JpaClassDescriptor descriptor = new JpaClassDescriptor(managedClass);

        // initially set access to the map level access - may be overridden below
        descriptor.setAccess(context.getEntityMap().getAccess());

        AnnotationContext stack = new AnnotationContext(descriptor);
        stack.push(context.getEntityMap());

        // === push class-level stuff
        for (int i = 0; i < classAnnotations.length; i++) {
            AnnotationProcessor processor = classProcessorFactory
                    .getProcessor(classAnnotations[i]);
            if (processor != null) {
                processor.onStartElement(managedClass, stack);
            }
        }

        // if class is not properly annotated, bail early
        if (stack.depth() == 1) {
            return;
        }

        // apply entity callbacks ...
        if (stack.peek() instanceof JpaAbstractEntity) {
            for (Method callback : getEntityCallbacks(managedClass)) {
                applyEntityCallbackAnnotations(callback, stack);
            }
        }

        // per JPA spec, 2.1.1, regarding access type:

        // When annotations are used, the placement of the mapping annotations on either
        // the persistent fields or persistent properties of the entity class specifies
        // the access type as being either field- or property-based access respectively.

        // Question (andrus) - if no annotations are placed at the field or method level,
        // we still must determine the access type to apply default mappping rules. How?
        // (using FIELD access for now).

        boolean fieldAccess = false;

        for (JpaPropertyDescriptor property : descriptor.getFieldDescriptors()) {
            stack.setPropertyDescriptor(property);
            if (applyMemberAnnotations(property, stack)) {
                fieldAccess = true;
            }
        }

        boolean propertyAccess = false;

        for (JpaPropertyDescriptor property : descriptor.getPropertyDescriptors()) {
            stack.setPropertyDescriptor(property);
            if (applyMemberAnnotations(property, stack)) {
                propertyAccess = true;
            }
        }

        if (stack.peek() instanceof JpaManagedClass) {
            JpaManagedClass entity = (JpaManagedClass) stack.peek();
            // sanity check
            if (fieldAccess && propertyAccess) {
                throw new JpaProviderException("Entity '"
                        + entity.getClassName()
                        + "' has both property and field annotations.");
            }

            // TODO: andrus - 11/29/2006 - clean this redundancy - access field should be
            // stored either in the entity or the descriptor.
            if (fieldAccess) {
                descriptor.setAccess(AccessType.FIELD);
                entity.setAccess(AccessType.FIELD);
            }
            else if (propertyAccess) {
                descriptor.setAccess(AccessType.PROPERTY);
                entity.setAccess(AccessType.PROPERTY);
            }
        }

        // === pop class-level stuff
        for (int i = classAnnotations.length - 1; i >= 0; i--) {
            AnnotationProcessor processor = classProcessorFactory
                    .getProcessor(classAnnotations[i]);
            if (processor != null) {
                processor.onFinishElement(managedClass, stack);
            }
        }
    }

    /**
     * Processes member annotations, returning true if at least one JPA annotation was
     * found.
     */
    protected boolean applyMemberAnnotations(
            JpaPropertyDescriptor property,
            AnnotationProcessorStack stack) {

        AnnotatedElement member = property.getMember();

        Annotation[] annotations = member.getAnnotations();
        // per 'getAnnotations' docs, array is returned by copy, so we can modify it...
        Arrays.sort(annotations, memberAnnotationsSorter);

        for (int j = 0; j < annotations.length; j++) {

            AnnotationProcessor memberProcessor = memberProcessorFactory
                    .getProcessor(annotations[j]);

            if (memberProcessor != null) {
                memberProcessor.onStartElement(member, stack);
            }
        }

        for (int j = annotations.length - 1; j >= 0; j--) {

            AnnotationProcessor memberProcessor = memberProcessorFactory
                    .getProcessor(annotations[j]);

            if (memberProcessor != null) {
                memberProcessor.onFinishElement(member, stack);
            }
        }

        return annotations.length > 0;
    }

    protected void applyEntityCallbackAnnotations(
            Method method,
            AnnotationProcessorStack stack) {

        Annotation[] annotations = method.getAnnotations();

        for (int j = 0; j < annotations.length; j++) {

            AnnotationProcessor callbackProcessor = callbackProcessorFactory
                    .getProcessor(annotations[j]);

            if (callbackProcessor != null) {
                callbackProcessor.onStartElement(method, stack);
            }
        }

        // don't call 'onFinishElement' as there is no nesting within callback
        // annotations...
    }

    /**
     * Returns a collection of methods that match an 'entity callback" pattern, i.e. "void
     * <METHOD>()".
     */
    protected Collection<Method> getEntityCallbacks(Class<?> managedClass) {

        Collection<Method> callbacks = new ArrayList<Method>(3);

        Method[] methods = managedClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {

            int modifiers = methods[i].getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            if (!Void.TYPE.equals(methods[i].getReturnType())) {
                continue;
            }

            Class<?>[] params = methods[i].getParameterTypes();
            if (params.length != 0) {
                continue;
            }

            callbacks.add(methods[i]);
        }

        return callbacks;
    }

    /**
     * Comparator for TYPE level JPA annotations that first returns top-level annotations
     * that define what kind of managed persistent class is being annotated.
     */
    final class AnnotationSorter implements Comparator<Annotation> {

        private Map<String, Integer> weights;

        AnnotationSorter(Map<String, Integer> weights) {
            this.weights = weights;
        }

        public int compare(Annotation o1, Annotation o2) {
            Integer w1 = weights.get(o1.annotationType().getName());
            Integer w2 = weights.get(o2.annotationType().getName());

            // nulls go last as all non-top annotations are not explicitly mentioned
            // mapped to sorting weight
            return Util.nullSafeCompare(false, w1, w2);
        }
    }

    final class AnnotationContext implements AnnotationProcessorStack {

        LinkedList<Object> stack = new LinkedList<Object>();
        JpaClassDescriptor classDescriptor;
        JpaPropertyDescriptor propertyDescriptor;

        AnnotationContext(JpaClassDescriptor classDescriptor) {
            this.classDescriptor = classDescriptor;
        }

        void setPropertyDescriptor(JpaPropertyDescriptor propertyDescriptor) {
            this.propertyDescriptor = propertyDescriptor;
        }

        public int depth() {
            return stack.size();
        }

        public Object peek() {
            return stack.peek();
        }

        public Object pop() {
            return stack.removeFirst();
        }

        public void push(Object object) {

            // do descriptor injection...
            if (object instanceof JpaAttribute) {
                JpaAttribute attribute = (JpaAttribute) object;
                attribute.setName(propertyDescriptor.getName());
                attribute.setPropertyDescriptor(propertyDescriptor);
            }
            else if (object instanceof JpaManagedClass) {
                ((JpaManagedClass) object).setClassDescriptor(classDescriptor);
            }

            stack.addFirst(object);
        }

        public void recordConflict(
                AnnotatedElement element,
                Class<?> annotatedType,
                String message) {

            StringBuilder buffer = new StringBuilder();
            buffer.append("Problem processing annotation: ").append(
                    annotatedType.getName());
            buffer.append(", annotated element: ").append(element);

            if (message != null) {
                buffer.append(", details: ").append(message);
            }

            context
                    .recordConflict(new SimpleValidationFailure(peek(), buffer.toString()));
        }
    }
}
