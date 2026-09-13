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
package org.apache.cayenne.event;

import java.lang.ref.WeakReference;
import java.util.EventObject;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A listener registered with {@link DefaultEventManager}: a weakly referenced listener object paired with the
 * handler to invoke on it. Two registrations are equal if they point to the same listener instance with the same
 * handler and dispatch mode.
 *
 * @since 5.0
 */
final class ListenerRegistration<L, E extends EventObject> {

    private final WeakReference<L> listener;
    private final Class<E> eventClass;
    private final EventHandler<? super L, ? super E> handler;
    private final boolean blocking;
    private final int hashCode;

    ListenerRegistration(L listener, Class<E> eventClass, EventHandler<? super L, ? super E> handler, boolean blocking) {
        this.listener = new WeakReference<>(listener);
        this.eventClass = eventClass;
        this.handler = handler;
        this.blocking = blocking;

        // the listener is held weakly, so the hash must be computed while it is still reachable
        this.hashCode = 31 * System.identityHashCode(listener) + handler.hashCode();
    }

    /**
     * Returns the listener, or null if it was garbage collected.
     */
    L getListener() {
        return listener.get();
    }

    boolean isBlocking() {
        return blocking;
    }

    /**
     * Invokes the handler if the listener is still reachable and the event is of the registered type. Returns false
     * if the listener was garbage collected and the registration should be discarded.
     */
    boolean fire(EventObject event) {
        L target = listener.get();
        if (target == null) {
            return false;
        }

        if (!eventClass.isInstance(event)) {
            return true;
        }

        try {
            handler.handle(target, eventClass.cast(event));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CayenneRuntimeException(e);
        }

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ListenerRegistration<?, ?> other)) {
            return false;
        }

        L target = listener.get();
        return target != null
                && target == other.listener.get()
                && blocking == other.blocking
                && handler.equals(other.handler);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
