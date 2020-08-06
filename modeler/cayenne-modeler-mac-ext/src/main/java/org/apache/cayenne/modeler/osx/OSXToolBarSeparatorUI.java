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
package org.apache.cayenne.modeler.osx;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;

/**
 * This class provides correct size as in AquaToolBarSeparatorUI, but doesn't render anything.
 *
 * @since 4.0
 */
public class OSXToolBarSeparatorUI extends BasicToolBarSeparatorUI {

    private OSXToolBarSeparatorUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new OSXToolBarSeparatorUI();
    }

    public void paint( Graphics g, JComponent c ) {
        // do nothing as we don't need it to be visible
    }

    public Dimension getMinimumSize(JComponent var1) {
        JToolBar.Separator var2 = (JToolBar.Separator)var1;
        return var2.getOrientation() == SwingConstants.HORIZONTAL
                ? new Dimension(1, 11) : new Dimension(11, 1);
    }

    public Dimension getPreferredSize(JComponent var1) {
        JToolBar.Separator var2 = (JToolBar.Separator)var1;
        return var2.getOrientation() == SwingConstants.HORIZONTAL
                ? new Dimension(1, 11) : new Dimension(11, 1);
    }

    public Dimension getMaximumSize(JComponent var1) {
        JToolBar.Separator var2 = (JToolBar.Separator)var1;
        return var2.getOrientation() == SwingConstants.HORIZONTAL
                ? new Dimension(2147483647, 11) : new Dimension(11, 2147483647);
    }
}
