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

package org.apache.cayenne.modeler.ui.preferences.encoding;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;

/**
 * A panel for file encoding selection.
 */
public class EncodingPreferencesView extends JPanel {

    private final JRadioButton defaultEncoding;
    private final JRadioButton otherEncoding;
    private final JComboBox encodingChoices;
    private final JLabel defaultEncodingLabel;

    public EncodingPreferencesView() {
        this.defaultEncoding = new JRadioButton();
        this.otherEncoding = new JRadioButton();
        this.encodingChoices = new JComboBox();
        this.defaultEncodingLabel = new JLabel();

        ButtonGroup group = new ButtonGroup();
        group.add(defaultEncoding);
        group.add(otherEncoding);

        FormLayout layout = new FormLayout("pref, 3dlu, pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.append(defaultEncoding, defaultEncodingLabel);
        builder.append(otherEncoding, encodingChoices);

        setLayout(new BorderLayout());
        add(builder.getPanel());
    }

    public JRadioButton getDefaultEncoding() {
        return defaultEncoding;
    }

    public JLabel getDefaultEncodingLabel() {
        return defaultEncodingLabel;
    }

    public JComboBox getEncodingChoices() {
        return encodingChoices;
    }

    public JRadioButton getOtherEncoding() {
        return otherEncoding;
    }
}
