package org.apache.cayenne.swing.components;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class LimitedTextField extends JTextField {

    private static final long serialVersionUID = 5615520143950793884L;

    public LimitedTextField(int limit) {
        setDocument(new LimitedDocument(limit));
    }

    private static class LimitedDocument extends PlainDocument {

        private static final long serialVersionUID = 2371422073526259311L;
        private int limit;

        LimitedDocument(int limit) {
            super();
            this.limit = limit;
        }

        public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
            if (str == null) return;

            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
        }
    }
}
