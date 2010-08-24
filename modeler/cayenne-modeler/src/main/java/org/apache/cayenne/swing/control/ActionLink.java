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

package org.apache.cayenne.swing.control;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

/**
 * A label that works like an HTML hyperlink, generating ActionEvents on click.
 * 
 */
public class ActionLink extends JLabel {

    static final Color LINK_FONT_COLOR = Color.BLUE;
    static final Color LINK_MOUSEOVER_FONT_COLOR = Color.RED;

    public ActionLink(String text) {
        super(text);

        // due to this bug (that is marked as fixed, but stil doesn't work on JDK
        // 1.4.2), we can't build underlined font:
        // http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4296952

        setForeground(LINK_FONT_COLOR);
        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                fireActionPerformed();
            }

            public void mouseEntered(MouseEvent e) {
                setForeground(LINK_MOUSEOVER_FONT_COLOR);
            }

            public void mouseExited(MouseEvent e) {
                setForeground(LINK_FONT_COLOR);
            }
        });
    }

    protected void fireActionPerformed() {

        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {

                if (e == null) {
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null);
                }

                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }
}
