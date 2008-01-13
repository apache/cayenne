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
package org.apache.cayenne.access;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.util.ObjectContextGraphAction;

/**
 * An action object that processes graph change calls from Persistent objects. It handles
 * GraphManager notifications and bi-directional graph consistency. The main difference
 * with CayenneContextGraph action is that reverse relationships are handled by the
 * objects themselves.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class DataContextGraphAction extends ObjectContextGraphAction {

    DataContextGraphAction(DataContext context) {
        super(context);
    }

    @Override
    protected void handleSimplePropertyChange(
            Persistent object,
            String propertyName,
            Object oldValue,
            Object newValue) {

        // for simple properties ObjectStore requires a callback only the first time the
        // object changes
        if (markAsDirty(object)) {
            context.getGraphManager().nodePropertyChanged(
                    object.getObjectId(),
                    propertyName,
                    oldValue,
                    newValue);
        }
    }

    @Override
    protected void handleArcPropertyChange(
            Persistent object,
            ArcProperty property,
            Object oldValue,
            Object newValue) {

        if (oldValue != newValue) {
            markAsDirty(object);

            if (oldValue instanceof Persistent) {
                context.getGraphManager().arcDeleted(
                        object.getObjectId(),
                        ((Persistent) oldValue).getObjectId(),
                        property.getName());
            }

            if (newValue instanceof Persistent) {
                context.getGraphManager().arcCreated(
                        object.getObjectId(),
                        ((Persistent) newValue).getObjectId(),
                        property.getName());
            }
        }
    }
}
