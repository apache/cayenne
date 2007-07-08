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
package org.apache.cayenne.access.event;

import java.util.EventListener;

import org.apache.cayenne.LifecycleListener;

/**
 * This interface declares methods that DataObject classes can implement to be notified
 * about transactions of their DataContext. Note: explicit registration with EventManager
 * is not necessary, since the events are simply forwarded by ContextCommitObserver;
 * stricly speaking these methods are just regular 'callbacks'. The event argument is
 * passed along for convenience.
 * 
 * @deprecated since 3.0M1 in favor of {@link LifecycleListener}. Will be removed in
 *             later 3.0 milestones.
 */
public interface DataObjectTransactionEventListener extends EventListener {

    public void willCommit(DataContextEvent event);

    public void didCommit(DataContextEvent event);
}
