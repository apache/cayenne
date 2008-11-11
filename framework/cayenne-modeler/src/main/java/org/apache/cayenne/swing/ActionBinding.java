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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;

/**
 */
public class ActionBinding extends BindingBase {

    protected Component view;

    public ActionBinding(AbstractButton button, String propertyExpression) {
        super(propertyExpression);

        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireAction();
            }
        });

        this.view = button;
    }

    public ActionBinding(BoundComponent component, String propertyExpression,
            String boundExpression) {
        super(propertyExpression);

        component.addPropertyChangeListener(
                boundExpression,
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent event) {
                        fireAction();
                    }

                });

        this.view = component.getView();
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
