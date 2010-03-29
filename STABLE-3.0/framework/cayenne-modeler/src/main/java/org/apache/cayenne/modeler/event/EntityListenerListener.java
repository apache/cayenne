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
package org.apache.cayenne.modeler.event;

import java.util.EventListener;


/**
 * Listener for manipulations on entity listeners
 *
 * @version 1.0 Oct 28, 2007
 */

public interface EntityListenerListener extends EventListener {
    /**
     * entity listener was added
     * @param e event
     */
    public void entityListenerAdded(EntityListenerEvent e);

    /**
     * entity listener was edited
     * @param e event
     */
    public void entityListenerChanged(EntityListenerEvent e);

    /**
     * entity listener was removed
     * @param e event
     */
    public void entityListenerRemoved(EntityListenerEvent e);
}

