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

package org.apache.cayenne.modeler.osx;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

import org.apache.cayenne.modeler.CayenneModelerFrame;

/**
 * @since 4.0
 */
public class OSXPanelUI extends BasicPanelUI {

    private static final Color BACKGROUND = new Color(0xEEEEEE);

    private static final OSXPanelUI INSTANCE;

    static {
        BasicPanelUI delegate;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends  BasicPanelUI> delegateClass = (Class<? extends  BasicPanelUI>)
                    Class.forName("com.apple.laf.AquaPanelUI");
            delegate = delegateClass.newInstance();
        } catch (Exception ex) {
            delegate = new BasicPanelUI();
        }

        INSTANCE = new OSXPanelUI(delegate);
    }

    private BasicPanelUI delegate;

    private OSXPanelUI(BasicPanelUI delegate) {
        this.delegate = delegate;
    }

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    protected void installDefaults(final JPanel p) {
        super.installDefaults(p);
        if(p instanceof CayenneModelerFrame.SearchPanel) {
            SwingUtilities.invokeLater(((CayenneModelerFrame.SearchPanel) p)::hideSearchLabel);
        } else {
            p.setBackground(BACKGROUND);
        }
    }

    @Override
    public void update(Graphics g, JComponent c) {
        delegate.update(g, c);
    }
}
