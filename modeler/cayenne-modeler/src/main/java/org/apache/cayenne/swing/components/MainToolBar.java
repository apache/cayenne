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

package org.apache.cayenne.swing.components;

import java.awt.Color;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * @since 4.0
 */
public class MainToolBar extends JToolBar {
    public MainToolBar() {
        setFloatable(false);
    }

    @Override
    public void setBorder(Border b) {
        Object border = UIManager.get("MainToolBar.border");
        if (border instanceof Border) {
            super.setBorder((Border) border);
        }
    }

    @Override
    public void setBackground(Color bg) {
        Object background = UIManager.get("MainToolBar.background");
        if (background instanceof Color) {
            super.setBackground((Color) background);
        }
    }
}
