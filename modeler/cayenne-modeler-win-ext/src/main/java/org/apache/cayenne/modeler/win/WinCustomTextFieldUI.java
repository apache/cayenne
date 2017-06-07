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

package org.apache.cayenne.modeler.win;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;

import com.jgoodies.looks.windows.WindowsTextFieldUI;
import org.apache.cayenne.modeler.util.combo.EditorTextField;

/**
 * @since 4.0
 */
public class WinCustomTextFieldUI extends BasicTextFieldUI {

    public WinCustomTextFieldUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        if(c instanceof EditorTextField) {
            c.putClientProperty("TextField.fullSizeBackground", Boolean.TRUE);
        }
        return new WindowsTextFieldUI();
    }

}
