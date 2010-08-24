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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.Introspector;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A binding that registers for action events of an arbitrary Component JavaBean that
 * provides a way to add an ActionListener via BeanDescriptor.
 * 
 */
public class BeanActionBinding extends BindingBase {

    protected Component view;

    public BeanActionBinding(Component component, String actionExpression) {
        super(actionExpression);
        this.view = component;

        boolean foundActionEvents = false;

        try {
            BeanInfo info = Introspector.getBeanInfo(component.getClass());
            EventSetDescriptor[] events = info.getEventSetDescriptors();

            if (events != null && events.length > 0) {
                for (EventSetDescriptor event : events) {
                    if (ActionListener.class
                            .isAssignableFrom(event.getListenerType())) {

                        event.getAddListenerMethod().invoke(component, new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        fireAction();
                                    }
                                });

                        foundActionEvents = true;
                        break;
                    }
                }
            }

        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error binding to component", e);
        }

        if (!foundActionEvents) {
            throw new CayenneRuntimeException("Component does not define action events: "
                    + component);
        }
    }

    public Component getView() {
        if (view == null) {
            throw new BindingException("headless action");
        }

        return view;
    }

    public void updateView() {
        // noop
    }

    protected void fireAction() {
        // TODO: catch exceptions...
        getValue();
    }
}
