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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToManyRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A CayenneContext helper that processes object deletion.
 *
 * @since 1.2
 */
class DataContextDeleteAction {

    private final DataContext context;

    DataContextDeleteAction(DataContext context) {
        this.context = context;
    }

    void performDelete(Persistent object) throws DeleteDenyException {

        int oldState = object.getPersistenceState();

        if (oldState == PersistenceState.TRANSIENT
                || oldState == PersistenceState.DELETED) {
            // Drop out... especially in case of DELETED we might be about to get
            // into a horrible recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return;
        }

        if (object.getObjectContext() == null) {
            throw new CayenneRuntimeException(
                    "Attempt to delete unregistered non-TRANSIENT object: %s", object);
        }

        if (object.getObjectContext() != context) {
            throw new CayenneRuntimeException(
                    "Attempt to delete object registered in a different ObjectContext. Object: %s, context: %s",
                    object,
                    context);
        }

        // must resolve HOLLOW objects before delete... needed
        // to process relationships and optimistic locking...

        context.prepareForAccess(object, null, false);

        if (oldState == PersistenceState.NEW) {
            deleteNew(object);
        } else {
            deletePersistent(object);
        }
    }

    private void deleteNew(Persistent object) throws DeleteDenyException {
        object.setPersistenceState(PersistenceState.TRANSIENT);
        processDeleteRules(object, PersistenceState.NEW);

        // if an object was NEW, we must throw it out
        context.getObjectStore().unregisterObject(object.getObjectId());
    }

    private void deletePersistent(Persistent object) throws DeleteDenyException {
        context.getChannel().getLifecycleEventsManager().preRemove().onEvent(List.of(object));

        int oldState = object.getPersistenceState();
        object.setPersistenceState(PersistenceState.DELETED);
        processDeleteRules(object, oldState);
        context.getObjectStore().changeRecorder().nodeRemoved(object.getObjectId());
    }

    @SuppressWarnings("unchecked")
    private Collection<Persistent> toCollection(Object object) {

        return switch (object) {
            case null -> Collections.emptyList();

            // create copies of collections to avoid iterator exceptions
            case Collection<?> ignored -> new ArrayList<>((Collection<Persistent>) object);
            case Map<?, ?> ignored -> new ArrayList<>(((Map<?, Persistent>) object).values());
            default -> Collections.singleton((Persistent) object);
        };
    }

    private void processDeleteRules(final Persistent object, int oldState)
            throws DeleteDenyException {

        CompiledEntity entity = context.getMapping().entity(object.getObjectId().getEntityName());

        for (final CompiledRelationship relationship : entity.relationships()) {

            // a flattened relationship through a join table whose PK includes the PK of the source: the join rows
            // must go with the object even if they are not in the object graph
            boolean processFlattened = relationship.isFlattened()
                    && relationship.joins().getFirst().reverse().isIdentifying();

            // first check for no action... bail out if no flattened processing is needed
            if (relationship.deleteRule() == null && !processFlattened) {
                continue;
            }

            Collection<Persistent> relatedObjects = toCollection(relationship.readProperty(object));

            // no related object, bail out
            if (relatedObjects.isEmpty()) {
                continue;
            }

            // process DENY rule first...
            if (relationship.deleteRule() == DeleteRule.DENY) {
                object.setPersistenceState(oldState);

                String message = relatedObjects.size() == 1
                        ? "1 related object"
                        : relatedObjects.size() + " related objects";
                throw new DeleteDenyException(object, relationship.name(), message);
            }

            // process flattened with dependent join tables...
            // joins must be removed even if they are non-existent or ignored in the
            // object graph
            if (processFlattened) {
                ArcId arcId = new ArcId(relationship);
                for (Persistent relatedObject : relatedObjects) {
                    context.getObjectStore().changeRecorder()
                            .arcDeleted(object.getObjectId(), relatedObject.getObjectId(), arcId);
                }
            }

            // process remaining rules
            switch (relationship.deleteRule()) {
                case null -> { /* nothing to do */ }
                case NULLIFY -> nullifyRelationship(object, relationship, relatedObjects);
                case CASCADE -> {
                    for (Persistent relatedObject : relatedObjects) {
                        performDelete(relatedObject);
                    }
                }
                default -> {
                    object.setPersistenceState(oldState);
                    throw new CayenneRuntimeException("Invalid delete rule %s", relationship.deleteRule());
                }
            }
        }
    }

    private void nullifyRelationship(Persistent object, CompiledRelationship relationship,
                                     Collection<Persistent> relatedObjects) {

        CompiledRelationship reverseArc = relationship.reverse();
        if (reverseArc != null) {
            unsetReverseArc(reverseArc, object, relatedObjects);
            return;
        }

        // no reverse on the declared target, and no sub-entity that could declare one - nothing to unset
        if (!relationship.target().hasSubEntities()) {
            return;
        }

        // See CAY-2779: check also target subentities
        Map<String, List<Persistent>> byEntity = relatedObjects.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getObjectId().getEntityName(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        byEntity.forEach((entityName, sameEntityObjects) -> {
            CompiledEntity targetEntity = context.getMapping().entity(entityName);
            CompiledRelationship reverse = relationship.reverse(targetEntity);
            if (reverse != null) {
                unsetReverseArc(reverse, object, sameEntityObjects);
            }
        });
    }

    private void unsetReverseArc(CompiledRelationship reverseArc, Persistent object,
                                 Collection<Persistent> relatedObjects) {
        switch (reverseArc) {
            case CompiledToManyRelationship toMany -> {
                for (Persistent relatedObject : relatedObjects) {
                    toMany.removeTarget(relatedObject, object, true);
                }
            }
            case CompiledToOneRelationship toOne -> {
                // Inverse is to-one - find all related objects and nullify the reverse relationship
                for (Persistent relatedObject : relatedObjects) {
                    toOne.setTarget(relatedObject, null, true);
                }
            }
        }
    }
}
