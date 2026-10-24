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

import org.apache.cayenne.DataChannel;

/**
 * Lifecycle event listeners of a {@link DataChannel}, one {@link LifecycleEventManager} per event type. Listeners are
 * registered per event type, for a given entity and its subclasses or for all entities, e.g.
 * {@code postAdd().addListener(auditor::artistAdded, Artist.class)}. The callbacks of the entities themselves,
 * declared by implementing the {@code org.apache.cayenne.lifecycle} interfaces, are registered the same way by the
 * creator of the manager.
 *
 * @since 6.0
 */
public class LifecycleEventsManager {

    private final LifecycleEventManager postAdd;
    private final LifecycleEventManager prePersist;
    private final LifecycleEventManager postPersist;
    private final LifecycleEventManager preUpdate;
    private final LifecycleEventManager postUpdate;
    private final LifecycleEventManager preRemove;
    private final LifecycleEventManager postRemove;
    private final LifecycleEventManager postLoad;

    public LifecycleEventsManager() {
        this.postAdd = new LifecycleEventManager();
        this.prePersist = new LifecycleEventManager();
        this.postPersist = new LifecycleEventManager();
        this.preUpdate = new LifecycleEventManager();
        this.postUpdate = new LifecycleEventManager();
        this.preRemove = new LifecycleEventManager();
        this.postRemove = new LifecycleEventManager();
        this.postLoad = new LifecycleEventManager();
    }

    /**
     * Returns the manager of the events fired right after a new object is created inside
     * {@code ObjectContext.newObject()}.
     */
    public LifecycleEventManager postAdd() {
        return postAdd;
    }

    /**
     * Returns the manager of the events fired right before a new object is committed.
     */
    public LifecycleEventManager prePersist() {
        return prePersist;
    }

    /**
     * Returns the manager of the events fired right after a commit of a new object is done.
     */
    public LifecycleEventManager postPersist() {
        return postPersist;
    }

    /**
     * Returns the manager of the events fired right before a modified object is committed.
     */
    public LifecycleEventManager preUpdate() {
        return preUpdate;
    }

    /**
     * Returns the manager of the events fired right after a commit of a modified object is done.
     */
    public LifecycleEventManager postUpdate() {
        return postUpdate;
    }

    /**
     * Returns the manager of the events fired right before an object is deleted inside
     * {@code ObjectContext.deleteObjects()}.
     */
    public LifecycleEventManager preRemove() {
        return preRemove;
    }

    /**
     * Returns the manager of the events fired right after a commit of a deleted object is done.
     */
    public LifecycleEventManager postRemove() {
        return postRemove;
    }

    /**
     * Returns the manager of the events fired after an object is fetched, reverted or resolved from a fault.
     */
    public LifecycleEventManager postLoad() {
        return postLoad;
    }
}
