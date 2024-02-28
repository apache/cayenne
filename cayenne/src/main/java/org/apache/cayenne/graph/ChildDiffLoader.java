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

package org.apache.cayenne.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * A GraphChangeHandler that loads child ObjectContext diffs into a parent
 * ObjectContext. Graph node ids are expected to be ObjectIds. This class is
 * made public since 3.0 to be used in ObjectContext synchronizing
 * 
 * @since 1.2
 */
public class ChildDiffLoader implements GraphChangeHandler {

	static final ThreadLocal<Boolean> childDiffProcessing = ThreadLocal.withInitial(() -> false);

	protected ObjectContext context;

	/**
	 * Returns whether child diff processing is in progress.
	 * 
	 * @since 3.0
	 */
	public static boolean isProcessingChildDiff() {
		return childDiffProcessing.get();
	}

	/**
	 * Sets whether child diff processing is in progress.
	 * 
	 * @since 3.0
	 */
	public static void setExternalChange(Boolean flag) {
		childDiffProcessing.set(flag);
	}

	public ChildDiffLoader(ObjectContext context) {
		this.context = context;
	}

	@Override
	public void nodeIdChanged(Object nodeId, Object newId) {
		throw new CayenneRuntimeException("Not supported");
	}

	@Override
	public void nodeCreated(Object nodeId) {

		setExternalChange(Boolean.TRUE);

		try {
			ObjectId id = (ObjectId) nodeId;
			if (id.getEntityName() == null) {
				throw new NullPointerException("Null entity name in id " + id);
			}

			ObjEntity entity = context.getEntityResolver().getObjEntity(id.getEntityName());
			if (entity == null) {
				throw new IllegalArgumentException("Entity not mapped with Cayenne: " + id);
			}

			Persistent persistent;
			Class<? extends Persistent> javaClass = context.getEntityResolver().getObjectFactory()
					.getJavaClass(entity.getJavaClassName());
			try {
				persistent = javaClass.getDeclaredConstructor().newInstance();
			} catch (Exception ex) {
				throw new CayenneRuntimeException("Error instantiating object.", ex);
			}

			persistent.setObjectId(id);
			context.registerNewObject(persistent);
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void nodeRemoved(Object nodeId) {
		setExternalChange(Boolean.TRUE);
		Persistent object = findObject(nodeId);
		if (object != null) {
			try {
				context.deleteObjects(object);
			} finally {
				setExternalChange(Boolean.FALSE);
			}
		} else {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {

		// this change is for simple property, so no need to convert targets to
		// server
		// objects...
		Persistent object = findObject(nodeId);
		ClassDescriptor descriptor = context.getEntityResolver()
				.getClassDescriptor(((ObjectId) nodeId).getEntityName());

		setExternalChange(Boolean.TRUE);
		try {
			descriptor.getProperty(property).writeProperty(object, oldValue, newValue);
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error setting property: " + property, e);
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {

		final Persistent source = findObject(nodeId);
		final Persistent target = findObject(targetNodeId);

		// if a target was later deleted, the diff for arcCreated is still
		// preserved and
		// can result in NULL target here.
		if (target == null) {
			return;
		}

		ClassDescriptor descriptor = context.getEntityResolver()
				.getClassDescriptor(((ObjectId) nodeId).getEntityName());
		ArcProperty property = (ArcProperty) descriptor.getProperty(arcId.toString());

		setExternalChange(Boolean.TRUE);
		try {
			property.visit(new PropertyVisitor() {

				public boolean visitAttribute(AttributeProperty property) {
					return false;
				}

				public boolean visitToMany(ToManyProperty property) {
					// connect reverse arc if the relationship is marked as
					// "runtime"
					ArcProperty reverseArc = property.getComplimentaryReverseArc();
					boolean autoConnectReverse = reverseArc != null && reverseArc.getRelationship().isRuntime();

					property.addTarget(source, target, autoConnectReverse);
					return false;
				}

				public boolean visitToOne(ToOneProperty property) {
					property.setTarget(source, target, false);
					return false;
				}
			});
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void arcDeleted(Object nodeId, final Object targetNodeId, ArcId arcId) {
		final Persistent source = findObject(nodeId);

		// needed as sometime temporary objects are evoked from the context
		// before
		// changing their relationships
		if (source == null) {
			return;
		}

		ClassDescriptor descriptor = context.getEntityResolver()
				.getClassDescriptor(((ObjectId) nodeId).getEntityName());
		PropertyDescriptor property = descriptor.getProperty(arcId.toString());

		setExternalChange(Boolean.TRUE);
		try {
			property.visit(new PropertyVisitor() {

				public boolean visitAttribute(AttributeProperty property) {
					return false;
				}

				public boolean visitToMany(ToManyProperty property) {
					// connect reverse arc if the relationship is marked as
					// "runtime"
					ArcProperty reverseArc = property.getComplimentaryReverseArc();
					boolean autoConnectReverse = reverseArc != null && reverseArc.getRelationship().isRuntime();

					Persistent target = findObject(targetNodeId);

					if (target == null) {

						// this is usually the case when a NEW object was
						// deleted and then
						// its
						// relationships were manipulated; so try to locate the
						// object in
						// the
						// collection ...
						// the performance of this is rather dubious of
						// course...
						target = findObjectInCollection(targetNodeId, property.readProperty(source));
					}

					if (target == null) {
						// ignore?
					} else {
						property.removeTarget(source, target, autoConnectReverse);
					}

					return false;
				}

				public boolean visitToOne(ToOneProperty property) {
					property.setTarget(source, null, false);
					return false;
				}
			});
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	protected Persistent findObject(Object nodeId) {
		// first do a lookup in ObjectStore; if even a hollow object is found,
		// return it;
		// if not - fetch.

		Persistent object = (Persistent) context.getGraphManager().getNode(nodeId);
		if (object != null) {
			return object;
		}

		ObjectId id = (ObjectId) nodeId;

		// this can happen if a NEW object is deleted and after that its
		// relationships are
		// modified
		if (id.isTemporary()) {
			return null;
		}

		// skip context cache lookup, go directly to its channel
		Query query = new ObjectIdQuery((ObjectId) nodeId);
		QueryResponse response = context.getChannel().onQuery(context, query);
		List<?> objects = response.firstList();

		if (objects.size() == 0) {
			throw new CayenneRuntimeException("No object for ID exists: %s", nodeId);
		} else if (objects.size() > 1) {
			throw new CayenneRuntimeException("Expected zero or one object, instead query matched: %d", objects.size());
		}

		return (Persistent) objects.get(0);
	}

	protected Persistent findObjectInCollection(Object nodeId, Object toManyHolder) {
		
		Collection<?> c = (toManyHolder instanceof Map) ? ((Map<?, ?>) toManyHolder).values() : (Collection<?>) toManyHolder;
		for(Object o : c) {
			Persistent p = (Persistent) o;
			if (nodeId.equals(p.getObjectId())) {
				return p;
			}
		}

		return null;
	}
}
