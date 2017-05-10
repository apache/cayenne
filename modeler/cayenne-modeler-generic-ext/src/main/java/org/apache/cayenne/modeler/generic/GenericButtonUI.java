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

package org.apache.cayenne.modeler.generic;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComponentUI;

import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * @since 4.0
 */
public class GenericButtonUI extends com.jgoodies.looks.plastic.PlasticButtonUI {
    private static final GenericButtonUI INSTANCE = new GenericButtonUI();

    public GenericButtonUI() {
    }

    public static ComponentUI createUI(JComponent b) {
        return INSTANCE;
    }

    @Override
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.putClientProperty("Plastic.is3D", Boolean.FALSE);
        if(b instanceof CayenneAction.CayenneToolbarButton) {
            b.setBorder(
                    new CompoundBorder(
                            new EmptyBorder(1, 1, 1, 1),
                            new CompoundBorder(
                                    LineBorder.createGrayLineBorder(),
                                    new EmptyBorder(4, 4, 4, 4))
                    )
            );
        }
    }
}
