package org.apache.cayenne.swing.components;

import javax.swing.JTextField;

public class LimitedJTextField extends JTextField {
    public LimitedJTextField(int limit) {
        setDocument(new LimitedDocument(limit));
    }
}
