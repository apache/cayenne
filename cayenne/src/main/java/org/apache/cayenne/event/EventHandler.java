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

import java.util.EventObject;

/**
 * A callback invoked by {@link EventManager} when an event is dispatched to a registered listener. The listener
 * itself is passed as the first argument, so the handler is normally an unbound method reference such as
 * {@code MyListener::onEvent}. An unbound reference captures no state and lets the EventManager hold the
 * listener via a weak reference, releasing the registration once the listener is garbage collected. A lambda
 * or bound reference that captures the listener (e.g. {@code this::onEvent}) defeats this and keeps the
 * listener alive for as long as the EventManager is.
 *
 * @param <L> the listener type
 * @param <E> the event type
 * @since 5.0
 */
@FunctionalInterface
public interface EventHandler<L, E extends EventObject> {

    void handle(L listener, E event) throws Exception;
}
