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

package org.apache.cayenne.modeler.generic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * @since 4.0
 */
public class GenericButtonUI extends com.jgoodies.looks.plastic.PlasticButtonUI {
    private static final GenericButtonUI INSTANCE = new GenericButtonUI();

    private static final Border BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            )
    );

    private static final Border DISABLED_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            )
    );

    private static final Border ACTIVE_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(1, 1, 1, 1),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x333333)),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            )
    );

    public GenericButtonUI() {
    }

    public static ComponentUI createUI(JComponent b) {
        return INSTANCE;
    }

    @Override
    public void installDefaults(final AbstractButton b) {
        super.installDefaults(b);
        b.putClientProperty("Plastic.is3D", Boolean.FALSE);
        if(b instanceof CayenneAction.CayenneToolbarButton) {
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.getModel().setArmed(true);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    b.getModel().setArmed(false);
                }
            });
        }
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if(c instanceof CayenneAction.CayenneToolbarButton) {
            AbstractButton b = (AbstractButton)c;
            if(!b.isEnabled()) {
                b.setBorder(DISABLED_BORDER);
            } else if(b.getModel().isArmed()) {
                b.setBorder(ACTIVE_BORDER);
            } else {
                b.setBorder(BORDER);
            }
        }
        super.update(g, c);
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b){
        if ( b.isContentAreaFilled() ) {
            Dimension size = b.getSize();
            g.setColor(getSelectColor());
            if(b instanceof CayenneAction.CayenneToolbarButton) {
                // don't paint outer border area
                g.fillRect(1, 1, size.width - 2, size.height - 2);
            } else {
                g.fillRect(0, 0, size.width, size.height);
            }
        }
    }
}
