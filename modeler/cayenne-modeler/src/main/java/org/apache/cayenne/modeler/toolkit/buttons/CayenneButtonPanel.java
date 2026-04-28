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

package org.apache.cayenne.modeler.toolkit.buttons;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/**
 * A panel that lays out a row of right-aligned buttons, typically used at the bottom of a dialog.
 */
public class CayenneButtonPanel extends JPanel {

    public CayenneButtonPanel(JButton... buttons) {
        setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 7));
        setLayout(new FlowLayout(FlowLayout.RIGHT));

        for (JButton button : buttons) {
            add(button);
        }
    }
}
