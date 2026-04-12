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

package org.apache.cayenne.swing;

import javax.swing.*;
import java.awt.*;

/**
 * A factory for a number of common bindings.
 * 
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
