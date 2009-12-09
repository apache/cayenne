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
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * A builder for component bindings that delegates the creation of the binding to the
 * underlying factory, and itself configures a number of binding parameters.
 * 
 */
public class BindingBuilder {

    protected BindingFactory factory;
    protected BindingDelegate delegate;
    protected Object context;
    protected Map actionsMap;

    /**
     * Constructs BindingBuilder with a BindingFactory and a root model object (or
     * context) of the binding.
     */
    public BindingBuilder(BindingFactory factory, Object context) {
        this.factory = factory;
        this.context = context;
    }

    public BindingDelegate getDelegate() {
        return delegate;
    }

    /**
     * Sets BindingDelegate that will be assigned to all bindings created via this
     * BindingBuilder.
     */
    public void setDelegate(BindingDelegate delegate) {
        this.delegate = delegate;
    }

    public Object getContext() {
        return context;
    }

    /**
     * Sets the context object that will be used by all bindings created via this
     * BindingBuilder. Context is a root of the domain model for the given binding.
     */
    public void setContext(Object context) {
        this.context = context;
    }

    public BindingFactory getFactory() {
        return factory;
    }

    /**
     * Binds to an instance of BoundComponent.
     * 
     * @since 1.2
     */
    public ObjectBinding bindToProperty(
            BoundComponent component,
            String property,
            String boundProperty) {
        ObjectBinding binding = factory
                .bindToProperty(component, property, boundProperty);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToAction(
            BoundComponent component,
            String action,
            String boundProperty) {
        ObjectBinding binding = factory.bindToAction(component, action, boundProperty);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToStateChange(AbstractButton button, String property) {
        ObjectBinding binding = factory.bindToStateChange(button, property);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToStateChangeAndAction(
            AbstractButton button,
            String property,
            String action) {
        ObjectBinding binding = factory.bindToStateChange(button, property);
        return initBinding(binding, getActionDelegate(action));
    }

    public ObjectBinding bindToAction(AbstractButton button, String action) {
        ObjectBinding binding = factory.bindToAction(button, action);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToAction(Component component, String action) {
        ObjectBinding binding = factory.bindToAction(component, action);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToComboSelection(JComboBox component, String property) {
        ObjectBinding binding = factory.bindToComboSelection(component, property, null);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToComboSelection(
            JComboBox component,
            String property,
            String noSelectionValue) {
        ObjectBinding binding = factory.bindToComboSelection(
                component,
                property,
                noSelectionValue);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToComboSelection(
            JComboBox component,
            String property,
            String action,
            String noSelectionValue) {
        ObjectBinding binding = factory.bindToComboSelection(
                component,
                property,
                noSelectionValue);
        return initBinding(binding, getActionDelegate(action));
    }

    public ObjectBinding bindToTextArea(JTextArea component, String property) {
        ObjectBinding binding = factory.bindToTextArea(component, property);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToTextField(JTextField component, String property) {
        ObjectBinding binding = factory.bindToTextField(component, property);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToCheckBox(JCheckBox component, String property) {
        ObjectBinding binding = factory.bindToCheckBox(component, property);
        return initBinding(binding, delegate);
    }

    protected ObjectBinding initBinding(ObjectBinding binding, BindingDelegate delegate) {
        binding.setDelegate(delegate);
        binding.setContext(context);
        return binding;
    }

    protected BindingDelegate getActionDelegate(String action) {
        BindingDelegate delegate = null;

        if (actionsMap == null) {
            actionsMap = new HashMap();
        }
        else {
            delegate = (BindingDelegate) actionsMap.get(action);
        }

        if (delegate == null) {
            delegate = new ActionDelegate(action);
            actionsMap.put(action, delegate);
        }

        return delegate;
    }
}
