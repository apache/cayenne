/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

import java.awt.Component;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A factory for a number of common bindings.
 * 
 * @author Andrus Adamchik
 */
public class BindingFactory {

    protected boolean usingNullForEmptyStrings;
    protected boolean checkingForValueChange;

    public BindingFactory() {
        // init defaults...
        usingNullForEmptyStrings = true;
        checkingForValueChange = true;
    }

    public ObjectBinding bindToTable(
            JTable table,
            String listBinding,
            String[] headers,
            BindingExpression[] columns,
            Class[] columnClass,
            boolean[] editableState,
            Object[] sampleLongValues) {

        TableBinding binding = new TableBinding(
                table,
                listBinding,
                headers,
                columns,
                columnClass,
                editableState,
                sampleLongValues);
        return prepareBinding(binding);
    }

    public ObjectBinding bindToProperty(
            BoundComponent component,
            String property,
            String boundProperty) {
        PropertyBinding binding = new PropertyBinding(component, property, boundProperty);
        return prepareBinding(binding);
    }

    /**
     * Binds to AbstractButton item state change events. Most common AbstractButton
     * subclasses are JButton, JCheckBox, JRadioButton.
     */
    public ObjectBinding bindToStateChange(AbstractButton button, String property) {
        ItemEventBinding binding = new ItemEventBinding(button, property);
        return prepareBinding(binding);
    }

    /**
     * Binds to AbstractButton action events. Most common AbstractButton subclasses are
     * JButton, JCheckBox, JRadioButton.
     */
    public ObjectBinding bindToAction(AbstractButton button, String action) {
        ActionBinding binding = new ActionBinding(button, action);
        return prepareBinding(binding);
    }

    /**
     * Binds to a generic component. Action events support is discovered via
     * introspection. If component class does not define action events, an exception is
     * thrown.
     */
    public ObjectBinding bindToAction(Component component, String action) {
        BeanActionBinding binding = new BeanActionBinding(component, action);
        return prepareBinding(binding);
    }

    public ObjectBinding bindToAction(
            BoundComponent component,
            String action,
            String boundExpression) {
        ActionBinding binding = new ActionBinding(component, action, boundExpression);
        return prepareBinding(binding);
    }

    public ObjectBinding bindToComboSelection(
            JComboBox component,
            String property,
            String noSelectionValue) {
        ComboSelectionBinding binding = new ComboSelectionBinding(
                component,
                property,
                noSelectionValue);
        return prepareBinding(binding);
    }

    public ObjectBinding bindToTextArea(JTextArea component, String property) {
        TextBinding binding = new TextBinding(component, property);
        return prepareBinding(binding);
    }

    /**
     * Creates a binding that updates a property on text field text changes.
     */
    public ObjectBinding bindToTextField(JTextField component, String property) {
        TextBinding binding = new TextBinding(component, property);
        return prepareBinding(binding);
    }

    /**
     * Configures binding with factory default settings.
     */
    protected ObjectBinding prepareBinding(BindingBase binding) {
        binding.setUsingNullForEmptyStrings(isUsingNullForEmptyStrings());
        binding.setCheckingForValueChange(isCheckingForValueChange());
        return binding;
    }

    public boolean isCheckingForValueChange() {
        return checkingForValueChange;
    }

    public void setCheckingForValueChange(boolean callingSetForEqual) {
        this.checkingForValueChange = callingSetForEqual;
    }

    public boolean isUsingNullForEmptyStrings() {
        return usingNullForEmptyStrings;
    }

    public void setUsingNullForEmptyStrings(boolean usingNullForEmptyStrings) {
        this.usingNullForEmptyStrings = usingNullForEmptyStrings;
    }
}