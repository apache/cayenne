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


package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

/**
 * A controller for stream encoding picker component.
 * 
 */
public class EncodingSelector extends CayenneController {

    public static final String ENCODING_PROPERTY = "encoding";

    private final EncodingSelectorView view;
    private final String systemEncoding;

    private String encoding;
    private boolean defaultEncoding;

    public EncodingSelector(CayenneController parent, EncodingSelectorView view) {
        super(parent);
        this.view = view;

        // init static models...
        this.systemEncoding = detectPlatformEncoding();

        Vector allEncodings = supportedEncodings(systemEncoding);
        view.getEncodingChoices().setModel(new DefaultComboBoxModel(allEncodings));
        view.getDefaultEncodingLabel().setText("Default (" + systemEncoding + ")");
        view.getDefaultEncoding().setSelected(true);

        view.getDefaultEncoding().addActionListener(e -> setDefaultEncoding(view.getDefaultEncoding().isSelected()));
        view.getOtherEncoding().addActionListener(e -> setDefaultEncoding(!view.getOtherEncoding().isSelected()));

        view.getEncodingChoices().addActionListener(e -> {
            Object sel = view.getEncodingChoices().getSelectedItem();
            setEncoding(sel != null ? sel.toString() : null);
        });
    }

    @Override
    public Component getView() {
        return view;
    }

    /**
     * Returns default encoding on the current platform.
     */
    protected String detectPlatformEncoding() {
        // this info is private until 1.5, so have to hack it...
        return new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
    }

    /**
     * Returns an array of charsets that all JVMs must support cross-platform combined
     * with a default platform charset. See Javadocs for java.nio.charset.Charset for the
     * list of "standard" charsets.
     */
    protected Vector supportedEncodings(String platformEncoding) {
        String[] defaultCharsets = new String[] {
                "US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16"
        };

        Vector charsets = new Vector(Arrays.asList(defaultCharsets));
        if (!charsets.contains(platformEncoding)) {
            charsets.add(platformEncoding);
        }

        Collections.sort(charsets);
        return charsets;
    }

    public void setSelectedEncoding(String encoding) {
        this.encoding = encoding;
        this.defaultEncoding = encoding == null || encoding.equals(systemEncoding);

        view.getEncodingChoices().setSelectedItem(encoding);
        view.getDefaultEncoding().setSelected(defaultEncoding);
        view.getOtherEncoding().setSelected(!defaultEncoding);
        view.getEncodingChoices().setEnabled(!defaultEncoding);
        view.getDefaultEncodingLabel().setEnabled(defaultEncoding);
    }

    private void setEncoding(String encoding) {
        if (!Util.nullSafeEquals(this.encoding, encoding)) {
            Object old = this.encoding;

            this.encoding = encoding;
            firePropertyChange("encoding", old, encoding);
        }
    }

    private void setDefaultEncoding(boolean b) {
        if (b != defaultEncoding) {
            this.defaultEncoding = b;

            if (b) {
                setEncoding(systemEncoding);
                view.getEncodingChoices().setEnabled(false);
                view.getDefaultEncodingLabel().setEnabled(true);
            }
            else {
                view.getEncodingChoices().setEnabled(true);
                view.getDefaultEncodingLabel().setEnabled(false);
            }

        }
    }
}
