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

package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.action.FindAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SearchPanel extends AppPanel {

    private final JTextField findField;

    public SearchPanel(Application app) {
        super(app);

        this.findField = new JTextField(10);

        initLayout();
        initBindings();
    }

    private void initLayout() {
        setLayout(new FlowLayout());

        findField.putClientProperty("JTextField.leadingIcon", IconFactory.buildIcon("icon-query.png"));
        findField.setPreferredSize(new Dimension(150, 22));
        add(findField);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private void initBindings() {
        findField.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                    findField.setBackground(Color.white);
                }
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }
        });
        findField.setAction(app.getActionManager().getAction(FindAction.class));

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof KeyEvent) {
                if (((KeyEvent) event).getModifiersEx() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                        && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_F) {
                    findField.requestFocus();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

}
