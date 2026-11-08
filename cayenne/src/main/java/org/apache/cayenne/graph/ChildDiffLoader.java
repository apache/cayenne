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
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToManyRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;

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
	public void nodeIdChanged(ObjectId id, ObjectId newId) {
		throw new CayenneRuntimeException("Not supported");
	}

	@Override
	public void nodeCreated(ObjectId id) {

		setExternalChange(Boolean.TRUE);

		try {
			if (id.getEntityName() == null) {
				throw new NullPointerException("Null entity name in id " + id);
			}

			CompiledEntity entity = context.getMapping().entity(id.getEntityName());
			Persistent persistent = (Persistent) entity.createObject();
			persistent.setObjectId(id);
			context.registerNewObject(persistent);
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void nodeRemoved(ObjectId id) {
		setExternalChange(Boolean.TRUE);
		Persistent object = findObject(id);
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
	public void nodePropertyChanged(ObjectId id, String property, Object oldValue, Object newValue) {

		// this change is for simple property, so no need to convert targets to
		// server
		// objects...
		Persistent object = findObject(id);
		CompiledEntity entity = context.getMapping().entity(id.getEntityName());

		setExternalChange(Boolean.TRUE);
		try {
			entity.property(property).writeProperty(object, oldValue, newValue);
		} catch (Exception e) {
			throw new CayenneRuntimeException("Error setting property: " + property, e);
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {

		Persistent source = findObject(id);
		Persistent target = findObject(targetId);

		// if a target was later deleted, the diff for arcCreated is still
		// preserved and
		// can result in NULL target here.
		if (target == null) {
			return;
		}

		CompiledEntity entity = context.getMapping().entity(id.getEntityName());
		CompiledRelationship relationship = entity.relationship(arcId.toString());

		setExternalChange(Boolean.TRUE);
		try {
			switch (relationship) {
				// the compiled mapping has no synthesized "runtime" reverse relationships that would need to be
				// connected here, so the reverse arc is left alone
				case CompiledToManyRelationship toMany -> toMany.addTarget(source, target, false);
				case CompiledToOneRelationship toOne -> toOne.setTarget(source, target, false);
			}
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	@Override
	public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
		Persistent source = findObject(id);

		// needed as sometime temporary objects are evoked from the context
		// before
		// changing their relationships
		if (source == null) {
			return;
		}

		CompiledEntity entity = context.getMapping().entity(id.getEntityName());
		CompiledRelationship relationship = entity.relationship(arcId.toString());

		setExternalChange(Boolean.TRUE);
		try {
			switch (relationship) {
				case CompiledToManyRelationship toMany -> {
					Persistent target = findObject(targetId);

					if (target == null) {
						// this is usually the case when a NEW object was deleted and then its relationships were
						// manipulated; so try to locate the object in the collection ...
						// the performance of this is rather dubious of course...
						target = findObjectInCollection(targetId, toMany.readProperty(source));
					}

					if (target != null) {
						// the compiled mapping has no synthesized "runtime" reverse relationships that would need
						// to be disconnected here, so the reverse arc is left alone
						toMany.removeTarget(source, target, false);
					}
				}
				case CompiledToOneRelationship toOne -> toOne.setTarget(source, null, false);
			}
		} finally {
			setExternalChange(Boolean.FALSE);
		}
	}

	protected Persistent findObject(ObjectId id) {
		// first do a lookup in ObjectStore; if even a hollow object is found,
		// return it;
		// if not - fetch.

		Persistent object = context.getObjectStore().getObject(id);
		if (object != null) {
			return object;
		}


		// this can happen if a NEW object is deleted and after that its
		// relationships are
		// modified
		if (id.isTemporary()) {
			return null;
		}

		// skip context cache lookup, go directly to its channel
		object = context.getChannel().onIdQuery(context, id);
		if (object == null) {
			throw new CayenneRuntimeException("No object for ID exists: %s", id);
		}

		return object;
	}

	protected Persistent findObjectInCollection(ObjectId id, Object toManyHolder) {
		
		Collection<?> c = (toManyHolder instanceof Map) ? ((Map<?, ?>) toManyHolder).values() : (Collection<?>) toManyHolder;
		for(Object o : c) {
			Persistent p = (Persistent) o;
			if (id.equals(p.getObjectId())) {
				return p;
			}
		}

		return null;
	}
}
