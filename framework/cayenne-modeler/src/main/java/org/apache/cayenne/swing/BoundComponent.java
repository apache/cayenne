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

package org.apache.cayenne.swing;

import java.awt.Component;
import java.beans.PropertyChangeListener;

/**
 * An API for a binding child that allows to establish bindings to custom Swing
 * components.
 * 
 * @since 1.2
 */
public interface BoundComponent {

    /**
     * Processes value pushed from parent.
     */
    void bindingUpdated(String expression, Object newValue);

    /**
     * Adds a property change listener to be notified of property updates.
     */
    // TODO: andrus, 04/8/2006 - declaring this method in the interface is redundant...
    // property "add*" methods can be discoverd via Bean introspection. See
    // BeanActionBinding for details.
    void addPropertyChangeListener(String expression, PropertyChangeListener listener);

    /**
     * Returns bound view component.
     */
    Component getView();
}
