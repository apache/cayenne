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

package org.apache.cayenne.modeler.ui.preferences.general;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.pref.GeneralPrefs;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneralPreferencesController extends ChildController<PreferenceDialogController> {

    private static final String[] STANDARD_ENCODINGS = {
            "ISO-8859-1", "US-ASCII", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE"
    };

    private final GeneralPreferencesView view;
    private final String systemEncoding;
    private final String defaultLabel;

    public GeneralPreferencesController(PreferenceDialogController parent) {
        super(parent);

        GeneralPrefs prefs = GeneralPrefs.of(getApplication().getPreferencesRepository());
        this.systemEncoding = detectPlatformEncoding();
        this.defaultLabel = systemEncoding + " (default)";

        this.view = new GeneralPreferencesView();

        view.getEncodingChoices().setModel(new DefaultComboBoxModel<>(supportedEncodings()));
        selectEncoding(prefs.getEncoding());
        view.getAutoLoadProject().setSelected(prefs.isAutoLoadProject());
        view.getNoDeletePrompt().setSelected(prefs.isNoDeletePrompt());
    }

    @Override
    public Component getView() {
        return view;
    }

    public void commit() {
        GeneralPrefs prefs = GeneralPrefs.of(getApplication().getPreferencesRepository());

        Object selected = view.getEncodingChoices().getSelectedItem();
        String encoding = (selected == null || defaultLabel.equals(selected)) ? systemEncoding : selected.toString();

        prefs.setEncoding(encoding);
        prefs.setAutoLoadProject(view.getAutoLoadProject().isSelected());
        prefs.setNoDeletePrompt(view.getNoDeletePrompt().isSelected());
    }

    /**
     * Returns default encoding on the current platform.
     */
    protected String detectPlatformEncoding() {
        return new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
    }

    /**
     * Returns charsets that all JVMs must support cross-platform, with the platform
     * default placed first and labeled. See java.nio.charset.Charset for the list of
     * "standard" charsets.
     */
    private String[] supportedEncodings() {
        List<String> charsets = new ArrayList<>(Arrays.asList(STANDARD_ENCODINGS));
        charsets.remove(systemEncoding);
        charsets.add(0, defaultLabel);
        return charsets.toArray(new String[0]);
    }

    private void selectEncoding(String encoding) {
        if (encoding == null || encoding.isEmpty() || encoding.equals(systemEncoding)) {
            view.getEncodingChoices().setSelectedItem(defaultLabel);
        } else {
            view.getEncodingChoices().setSelectedItem(encoding);
        }
    }
}
