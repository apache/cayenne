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

import javax.swing.JCheckBox;

/**
 * A generic adapter that binds a check box to a bean property.
 *
 */
public class CheckBoxBinding extends BindingBase {

    protected JCheckBox checkBox;

    public CheckBoxBinding(JCheckBox checkBox, String propertyExpression) {
        super(propertyExpression);
        this.checkBox = checkBox;

        this.checkBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent event) {
                if (!modelUpdateDisabled) {
                    updateModel();
                }
            }
        });
    }

    public Component getView() {
        return checkBox;
    }

    public void updateView() {
        Boolean value = (Boolean) getValue();

        modelUpdateDisabled = true;
        try {
            checkBox.setSelected(value.booleanValue());
        }
        finally {
            modelUpdateDisabled = false;
        }
    }

    protected void updateModel() {
        setValue(Boolean.valueOf(checkBox.isSelected()));
    }
}
