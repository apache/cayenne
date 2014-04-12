package org.apache.cayenne.modeler.editor.validation;

import org.apache.cayenne.validation.SimpleValidationFailure;

/**
 *  ValidationFailure implementation that described a failure of attribute.
 */
public class AttributeValidationFailure extends SimpleValidationFailure {

    public  AttributeValidationFailure(int columnIndex, String error) {
        super(columnIndex, error);
    }

    public int getColumnIndex() {
        return (Integer)source;
    }
}
