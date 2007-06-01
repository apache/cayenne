/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.swing;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A builder for component bindings that delegates the creation of the binding to the
 * underlying factory, and itself configures a number of binding parameters.
 * 
 * @author Andrei Adamchik
 */
public class BindingBuilder {

    protected BindingFactory factory;
    protected BindingDelegate delegate;
    protected Object context;
    protected Map actionsMap;

    public BindingBuilder(BindingFactory factory, Object context) {
        this.factory = factory;
        this.context = context;
    }

    public BindingDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(BindingDelegate delegate) {
        this.delegate = delegate;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public BindingFactory getFactory() {
        return factory;
    }

    public ObjectBinding bindToCheckbox(JCheckBox checkbox, String property) {
        ObjectBinding binding = factory.bindToCheckbox(checkbox, property);
        return initBinding(binding, delegate);
    }

    public ObjectBinding bindToCheckbox(JCheckBox checkbox, String property, String action) {
        ObjectBinding binding = factory.bindToCheckbox(checkbox, property);
        return initBinding(binding, getActionDelegate(action));
    }

    public ObjectBinding bindToAction(JButton button, String action) {
        ObjectBinding binding = factory.bindToButton(button, action);
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