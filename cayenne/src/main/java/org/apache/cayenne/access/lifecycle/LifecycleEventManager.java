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
package org.apache.cayenne.access.lifecycle;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lifecycle callbacks and listeners of a {@link DataChannel} for all event types defined in {@link LifecycleEvent}.
 *
 * @since 6.0
 */
public class LifecycleEventManager {

    // the uniform shape every reflective callback handle is adapted to, so that it can be called with "invokeExact"
    private static final MethodType CALLBACK_TYPE = MethodType.methodType(void.class, Persistent.class);

    private final EntityResolver entityResolver;
    private final SingleLifecycleEventManager[] eventManagers;
    private final Map<String, Collection<Class<? extends Persistent>>> entitiesByAnnotation;

    private Map<String, AnnotationReader> annotations;

    public LifecycleEventManager(EntityResolver resolver) {

        this.entityResolver = resolver;

        // initialize callbacks map in constructor to avoid synchronization issues downstream.
        this.eventManagers = new SingleLifecycleEventManager[LifecycleEvent.values().length];
        for (int i = 0; i < eventManagers.length; i++) {
            eventManagers[i] = new SingleLifecycleEventManager();
        }

        // other "static" lookup maps are initialized on-demand
        this.entitiesByAnnotation = new ConcurrentHashMap<>();

        // register callbacks declared by the mapped entities: annotated methods and callbacks from the model
        for (ObjEntity entity : resolver.getObjEntities()) {
            Class<? extends Persistent> entityClass = entityClass(resolver, entity);

            for (Method m : entityClass.getDeclaredMethods()) {
                for (Annotation a : m.getAnnotations()) {
                    AnnotationReader reader = getAnnotations().get(a.annotationType().getName());
                    if (reader != null) {
                        eventManagers[reader.eventType().ordinal()].addListener(entityClass, entityCallback(m));
                    }
                }
            }

            for (CallbackDescriptor callback : entity.getCallbackMap().getCallbacks()) {
                for (String method : callback.getCallbackMethods()) {
                    eventManagers[callback.getCallbackType().ordinal()]
                            .addListener(entityClass, entityCallback(entityClass, method));
                }
            }
        }
    }

    /**
     * Removes all listeners for all event types.
     */
    public void clear() {
        for (SingleLifecycleEventManager eventCallback : eventManagers) {
            eventCallback.clear();
        }
    }

    /**
     * Removes listeners for a single event type.
     */
    public void clear(LifecycleEvent type) {
        eventManagers[type.ordinal()].clear();
    }

    /**
     * Returns true if there are no listeners for a specific event type.
     */
    public boolean hasListeners(LifecycleEvent type) {
        return eventManagers[type.ordinal()].isEmpty();
    }

    /**
     * Registers a {@link LifecycleListener} for all events on all entities.
     */
    public void addDefaultListener(LifecycleListener listener) {
        addDefaultListener(LifecycleEvent.POST_ADD, listener::postAdd);
        addDefaultListener(LifecycleEvent.PRE_PERSIST, listener::prePersist);
        addDefaultListener(LifecycleEvent.POST_PERSIST, listener::postPersist);
        addDefaultListener(LifecycleEvent.PRE_REMOVE, listener::preRemove);
        addDefaultListener(LifecycleEvent.POST_REMOVE, listener::postRemove);
        addDefaultListener(LifecycleEvent.PRE_UPDATE, listener::preUpdate);
        addDefaultListener(LifecycleEvent.POST_UPDATE, listener::postUpdate);
        addDefaultListener(LifecycleEvent.POST_LOAD, listener::postLoad);
    }

    /**
     * Registers a listener for a given event on all entities. The listener is typically a method reference, e.g.
     * {@code addDefaultListener(LifecycleEvent.POST_LOAD, auditor::objectLoaded)}.
     *
     * @since 6.0
     */
    public void addDefaultListener(LifecycleEvent type, Consumer<? super Persistent> listener) {
        eventManagers[type.ordinal()].addDefaultListener(listener);
    }

    /**
     * Registers a {@link LifecycleListener} for all events on a given entity and its subclasses.
     */
    public void addListener(Class<? extends Persistent> entityClass, LifecycleListener listener) {
        addListener(LifecycleEvent.POST_ADD, entityClass, listener::postAdd);
        addListener(LifecycleEvent.PRE_PERSIST, entityClass, listener::prePersist);
        addListener(LifecycleEvent.POST_PERSIST, entityClass, listener::postPersist);
        addListener(LifecycleEvent.PRE_REMOVE, entityClass, listener::preRemove);
        addListener(LifecycleEvent.POST_REMOVE, entityClass, listener::postRemove);
        addListener(LifecycleEvent.PRE_UPDATE, entityClass, listener::preUpdate);
        addListener(LifecycleEvent.POST_UPDATE, entityClass, listener::postUpdate);
        addListener(LifecycleEvent.POST_LOAD, entityClass, listener::postLoad);
    }

    /**
     * Registers a listener for a given event on a given entity and its subclasses. The listener is typically a method
     * reference, either on a listener object, e.g. {@code addListener(LifecycleEvent.POST_ADD, Artist.class,
     * auditor::artistAdded)}, or on the entity itself, e.g.
     * {@code addListener(LifecycleEvent.POST_ADD, Artist.class, Artist::onPostAdd)}.
     *
     * @since 6.0
     */
    public <T extends Persistent> void addListener(
            LifecycleEvent type,
            Class<T> entityClass,
            Consumer<? super T> listener) {
        eventManagers[type.ordinal()].addListener(entityClass, listener);
    }

    /**
     * Adds a listener, mapping its methods to events based on annotations.
     *
     * @since 3.1
     */
    public void addListener(Object listener) {
        if (listener == null) {
            throw new NullPointerException("Null listener");
        }

        Class<?> listenerType = listener.getClass();
        do {
            for (Method m : listenerType.getDeclaredMethods()) {

                for (Annotation a : m.getAnnotations()) {
                    AnnotationReader reader = getAnnotations().get(a.annotationType().getName());

                    if (reader != null) {

                        Set<Class<? extends Persistent>> types = new HashSet<>();

                        Class<?>[] entities = reader.entities(a);
                        Class<? extends Annotation>[] entityAnnotations = reader.entityAnnotations(a);

                        // TODO: ignoring entity subclasses?
                        // whenever we add those, take into account "exlcudeSuperclassListeners" flag
                        for (Class<?> entity : entities) {
                            types.add(entity.asSubclass(Persistent.class));
                        }

                        for (Class<? extends Annotation> type : entityAnnotations) {
                            types.addAll(getAnnotatedEntities(type));
                        }

                        for (Class<? extends Persistent> type : types) {
                            eventManagers[reader.eventType().ordinal()]
                                    .addListener(type, listenerCallback(listener, m, type));
                        }

                        // if no entities specified then adding global callback
                        if (entities.length == 0 && entityAnnotations.length == 0) {
                            eventManagers[reader.eventType().ordinal()]
                                    .addDefaultListener(listenerCallback(listener, m, Persistent.class));
                        }
                    }
                }
            }

            listenerType = listenerType.getSuperclass();
        } while (listenerType != null && !listenerType.equals(Object.class));
    }

    /**
     * Invokes callbacks of a specific type for a given entity object.
     */
    public void performCallbacks(LifecycleEvent type, Persistent object) {
        eventManagers[type.ordinal()].performCallbacks(object);
    }

    /**
     * Invokes callbacks of a specific type for a collection of entity objects.
     */
    public void performCallbacks(LifecycleEvent type, Collection<? extends Persistent> objects) {
        eventManagers[type.ordinal()].performCallbacks(objects);
    }

    // used by unit tests to poke inside the manager
    SingleLifecycleEventManager getHandler(LifecycleEvent type) {
        return eventManagers[type.ordinal()];
    }

    private Map<String, AnnotationReader> getAnnotations() {
        if (annotations == null) {

            Map<String, AnnotationReader> annotationsMap = new HashMap<>();
            annotationsMap.put(PostAdd.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.POST_ADD;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PostAdd) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PostAdd) a).value();
                }
            });

            annotationsMap.put(PrePersist.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.PRE_PERSIST;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PrePersist) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PrePersist) a).value();
                }
            });

            annotationsMap.put(PreRemove.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.PRE_REMOVE;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PreRemove) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PreRemove) a).value();
                }
            });

            annotationsMap.put(PreUpdate.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.PRE_UPDATE;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PreUpdate) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PreUpdate) a).value();
                }
            });

            annotationsMap.put(PostLoad.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.POST_LOAD;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PostLoad) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PostLoad) a).value();
                }
            });

            annotationsMap.put(PostPersist.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.POST_PERSIST;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PostPersist) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PostPersist) a).value();
                }
            });

            annotationsMap.put(PostUpdate.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.POST_UPDATE;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PostUpdate) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PostUpdate) a).value();
                }
            });

            annotationsMap.put(PostRemove.class.getName(), new AnnotationReader() {

                @Override
                LifecycleEvent eventType() {
                    return LifecycleEvent.POST_REMOVE;
                }

                @Override
                Class<? extends Annotation>[] entityAnnotations(Annotation a) {
                    return ((PostRemove) a).entityAnnotations();
                }

                @Override
                Class<?>[] entities(Annotation a) {
                    return ((PostRemove) a).value();
                }
            });

            this.annotations = annotationsMap;
        }

        return annotations;
    }

    private Collection<Class<? extends Persistent>> getAnnotatedEntities(Class<? extends Annotation> annotationType) {

        Collection<Class<? extends Persistent>> entities = entitiesByAnnotation.get(annotationType.getName());

        if (entities == null) {

            // ensure no dupes
            entities = new HashSet<>();

            for (ObjEntity entity : entityResolver.getObjEntities()) {
                Class<? extends Persistent> entityType = entityClass(entityResolver, entity);
                // ensure that we don't register the same callback for multiple
                // classes in the same hierarchy, so find the topmost type using
                // a given annotation and register it once

                // TODO: This ignores "excludeSuperclassListeners" setting,
                // which is not possible with annotations anyways

                while (entityType != null && entityType.isAnnotationPresent(annotationType)) {

                    Class<?> superType = entityType.getSuperclass();

                    if (superType == null || !superType.isAnnotationPresent(annotationType)) {
                        entities.add(entityType);
                        break;
                    }

                    entityType = superType.asSubclass(Persistent.class);
                }

            }

            entitiesByAnnotation.put(annotationType.getName(), entities);
        }

        return entities;
    }

    private static Class<? extends Persistent> entityClass(EntityResolver resolver, ObjEntity entity) {
        return resolver.getObjectFactory().getJavaClass(entity.getJavaClassName()).asSubclass(Persistent.class);
    }

    /**
     * Creates a callback invoking a no-arg method of the entity, found by name. The method must be declared in the
     * entity class itself.
     */
    static Consumer<Persistent> entityCallback(Class<?> entityClass, String methodName) {
        Method method;
        try {
            method = entityClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + entityClass.getName()
                    + " has no valid callback method '" + methodName + "'");
        }

        return entityCallback(method);
    }

    /**
     * Creates a callback invoking a no-arg method of the entity.
     */
    static Consumer<Persistent> entityCallback(Method method) {
        // must be non-static and void, with no args. JPA spec also requires it to be non-final, but we don't care
        if (Modifier.isStatic(method.getModifiers())
                || method.getReturnType() != void.class
                || method.getParameterCount() != 0) {
            throw new IllegalArgumentException("Class " + method.getDeclaringClass().getName()
                    + " has no valid callback method '" + method.getName() + "'");
        }

        return callback(method, unreflect(method));
    }

    /**
     * Creates a callback invoking a listener method with the entity as its single argument.
     */
    static Consumer<Persistent> listenerCallback(Object listener, Method method, Class<?> entityType) {
        Objects.requireNonNull(listener, "Null listener");

        // must be non-static and void, with a single arg assignable from the entity type
        Class<?>[] parameters = method.getParameterTypes();
        if (Modifier.isStatic(method.getModifiers())
                || method.getReturnType() != void.class
                || parameters.length != 1
                || !parameters[0].isAssignableFrom(entityType)) {
            throw new IllegalArgumentException("Invalid listener method: " + method);
        }

        return callback(method, unreflect(method).bindTo(listener));
    }

    /**
     * Wraps a method handle in a callback. Unchecked exceptions and errors raised by the method propagate as they
     * are, checked exceptions are wrapped.
     */
    private static Consumer<Persistent> callback(Method method, MethodHandle handle) {
        String name = method.getDeclaringClass().getName() + "." + method.getName();
        MethodHandle callbackHandle = handle.asType(CALLBACK_TYPE);

        return entity -> {
            try {
                callbackHandle.invokeExact(entity);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable th) {
                throw new CayenneRuntimeException("Error invoking callback method " + name, th);
            }
        };
    }

    private static MethodHandle unreflect(Method method) {
        if (!Util.isAccessible(method)) {
            method.setAccessible(true);
        }

        try {
            return MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Inaccessible callback method: " + method, e);
        }
    }

    abstract static class AnnotationReader {

        abstract LifecycleEvent eventType();

        abstract Class<?>[] entities(Annotation a);

        abstract Class<? extends Annotation>[] entityAnnotations(Annotation a);
    }
}
