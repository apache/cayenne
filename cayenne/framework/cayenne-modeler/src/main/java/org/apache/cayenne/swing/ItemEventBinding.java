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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;

/**
 * Binds a checkbox state to an int or boolean property.
 * 
 */
public class ItemEventBinding extends BindingBase {

    protected AbstractButton boundItem;

    public ItemEventBinding(AbstractButton boundItem, String expression) {
        super(expression);
        this.boundItem = boundItem;

        boundItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                updateModel();
            }
        });
    }

    public Component getView() {
        return boundItem;
    }

    public void updateView() {
        Object value = getValue();
        boolean b = false;

        // convert to boolean
        if (value != null) {
            if (value instanceof Boolean) {
                b = ((Boolean) value).booleanValue();
            }
            else if (value instanceof Number) {
                b = ((Number) value).intValue() != 0;
            }
        }

        modelUpdateDisabled = true;
        try {
            boundItem.setSelected(b);
        }
        finally {
            modelUpdateDisabled = false;
        }
    }

    protected void updateModel() {
        setValue(boundItem.isSelected() ? Boolean.TRUE : Boolean.FALSE);
    }
}
