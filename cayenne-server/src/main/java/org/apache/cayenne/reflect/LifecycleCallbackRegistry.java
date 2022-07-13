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
package org.apache.cayenne.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.CayenneRuntimeException;
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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;

/**
 * A registry of lifecycle callbacks for all callback event types. Valid event
 * types are defined in {@link LifecycleEvent} enum.
 * 
 * @since 3.0
 */
public class LifecycleCallbackRegistry {

	private EntityResolver entityResolver;
	private LifecycleCallbackEventHandler[] eventCallbacks;
	private Map<String, AnnotationReader> annotationsMap;
	private Map<String, Collection<Class<?>>> entitiesByAnnotation;

	/**
	 * Creates an empty callback registry.
	 */
	public LifecycleCallbackRegistry(EntityResolver resolver) {

		this.entityResolver = resolver;

		// initialize callbacks map in constructor to avoid synchronization issues downstream.
		this.eventCallbacks = new LifecycleCallbackEventHandler[LifecycleEvent.values().length];
		for (int i = 0; i < eventCallbacks.length; i++) {
			eventCallbacks[i] = new LifecycleCallbackEventHandler();
		}

		// other "static" lookup maps are initialized on-demand
		this.entitiesByAnnotation = new ConcurrentHashMap<>();
	}

	/**
	 * Removes all listeners for all event types.
	 */
	public void clear() {
		for (LifecycleCallbackEventHandler eventCallback : eventCallbacks) {
			eventCallback.clear();
		}
	}

	/**
	 * Removes listeners for a single event type.
	 */
	public void clear(int type) {
		eventCallbacks[type].clear();
	}

	/**
	 * Returns true if there are no listeners for a specific event type.
	 */
	public boolean isEmpty(LifecycleEvent type) {
		return eventCallbacks[type.ordinal()].isEmpty();
	}

	/**
	 * Registers a {@link LifecycleListener} for all events on all entities.
	 * Note that listeners are not required to implement
	 * {@link LifecycleListener} interface. Other methods in this class can be
	 * used to register arbitrary listeners.
	 */
	public void addDefaultListener(LifecycleListener listener) {
		addDefaultListener(LifecycleEvent.POST_ADD, listener, "postAdd");
		addDefaultListener(LifecycleEvent.PRE_PERSIST, listener, "prePersist");
		addDefaultListener(LifecycleEvent.POST_PERSIST, listener, "postPersist");
		addDefaultListener(LifecycleEvent.PRE_REMOVE, listener, "preRemove");
		addDefaultListener(LifecycleEvent.POST_REMOVE, listener, "postRemove");
		addDefaultListener(LifecycleEvent.PRE_UPDATE, listener, "preUpdate");
		addDefaultListener(LifecycleEvent.POST_UPDATE, listener, "postUpdate");
		addDefaultListener(LifecycleEvent.POST_LOAD, listener, "postLoad");
	}

	/**
	 * Registers a callback method to be invoked on a provided non-entity object
	 * when a lifecycle event occurs on any entity that does not suppress
	 * default callbacks.
	 */
	public void addDefaultListener(LifecycleEvent type, Object listener, String methodName) {
		eventCallbacks[type.ordinal()].addDefaultListener(listener, methodName);
	}

	/**
	 * Registers a {@link LifecycleListener} for all events on all entities.
	 * Note that listeners are not required to implement
	 * {@link LifecycleListener} interface. Other methods in this class can be
	 * used to register arbitrary listeners.
	 */
	public void addListener(Class<?> entityClass, LifecycleListener listener) {
		addListener(LifecycleEvent.POST_ADD, entityClass, listener, "postAdd");
		addListener(LifecycleEvent.PRE_PERSIST, entityClass, listener, "prePersist");
		addListener(LifecycleEvent.POST_PERSIST, entityClass, listener, "postPersist");
		addListener(LifecycleEvent.PRE_REMOVE, entityClass, listener, "preRemove");
		addListener(LifecycleEvent.POST_REMOVE, entityClass, listener, "postRemove");
		addListener(LifecycleEvent.PRE_UPDATE, entityClass, listener, "preUpdate");
		addListener(LifecycleEvent.POST_UPDATE, entityClass, listener, "postUpdate");
		addListener(LifecycleEvent.POST_LOAD, entityClass, listener, "postLoad");
	}

	/**
	 * Registers callback method to be invoked on a provided non-entity object
	 * when a lifecycle event occurs for a specific entity.
	 */
	public void addListener(LifecycleEvent type, Class<?> entityClass, Object listener, String methodName) {
		eventCallbacks[type.ordinal()].addListener(entityClass, listener, methodName);
	}

	/**
	 * Registers a callback method to be invoked on an entity class instances
	 * when a lifecycle event occurs.
	 * 
	 * @since 4.0
	 */
	public void addCallback(LifecycleEvent type, Class<?> entityClass, String methodName) {
		eventCallbacks[type.ordinal()].addListener(entityClass, methodName);
	}

	/**
	 * Registers a callback method to be invoked on an entity class instances
	 * when a lifecycle event occurs.
	 *
	 * @param type of the lifecycle event
	 * @param entityClass type of the entity
	 * @param method callback method reference
	 *
	 * @since 4.2
	 */
	public void addCallback(LifecycleEvent type, Class<?> entityClass, Method method) {
		eventCallbacks[type.ordinal()].addListener(entityClass, method);
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
					AnnotationReader reader = getAnnotationsMap().get(a.annotationType().getName());

					if (reader != null) {

						Set<Class<?>> types = new HashSet<>();

						Class<?>[] entities = reader.entities(a);
						Class<? extends Annotation>[] entityAnnotations = reader.entityAnnotations(a);

						// TODO: ignoring entity subclasses?
						// whenever we add those, take into account "exlcudeSuperclassListeners" flag
						Collections.addAll(types, entities);

						for (Class<? extends Annotation> type : entityAnnotations) {
							types.addAll(getAnnotatedEntities(type));
						}

						for (Class<?> type : types) {
							eventCallbacks[reader.eventType().ordinal()].addListener(type, listener, m);
						}

						// if no entities specified then adding global callback
						if (entities.length == 0 && entityAnnotations.length == 0) {
							eventCallbacks[reader.eventType().ordinal()].addDefaultListener(listener, m.getName());
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
		eventCallbacks[type.ordinal()].performCallbacks(object);
	}

	/**
	 * Invokes callbacks of a specific type for a collection of entity objects.
	 */
	public void performCallbacks(LifecycleEvent type, Collection<?> objects) {
		eventCallbacks[type.ordinal()].performCallbacks(objects);
	}

	// used by unit tests to poke inside the registry
	LifecycleCallbackEventHandler getHandler(LifecycleEvent type) {
		return eventCallbacks[type.ordinal()];
	}

	private Map<String, AnnotationReader> getAnnotationsMap() {
		if (annotationsMap == null) {

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

			this.annotationsMap = annotationsMap;
		}

		return annotationsMap;
	}

	private Collection<Class<?>> getAnnotatedEntities(Class<? extends Annotation> annotationType) {

		Collection<Class<?>> entities = entitiesByAnnotation.get(annotationType.getName());

		if (entities == null) {

			// ensure no dupes
			entities = new HashSet<>();

			for (ObjEntity entity : entityResolver.getObjEntities()) {
				Class<?> entityType = entityResolver.getObjectFactory().getJavaClass(entity.getJavaClassName());
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

					entityType = superType;
				}

			}

			entitiesByAnnotation.put(annotationType.getName(), entities);
		}

		return entities;
	}

	abstract class AnnotationReader {

		abstract LifecycleEvent eventType();

		abstract Class<?>[] entities(Annotation a);

		abstract Class<? extends Annotation>[] entityAnnotations(Annotation a);
	}
}
