package org.objectstyle.art;

import org.objectstyle.art.auto._Artist;
import org.objectstyle.cayenne.unit.util.ValidationDelegate;
import org.objectstyle.cayenne.validation.ValidationResult;

public class Artist extends _Artist {

    protected transient ValidationDelegate validationDelegate;
    protected boolean validateForSaveCalled;

    public boolean isValidateForSaveCalled() {
        return validateForSaveCalled;
    }

    public void resetValidationFlags() {
        validateForSaveCalled = false;
    }

    public void setValidationDelegate(ValidationDelegate validationDelegate) {
        this.validationDelegate = validationDelegate;
    }

    public void validateForSave(ValidationResult validationResult) {
        validateForSaveCalled = true;
        if (validationDelegate != null) {
            validationDelegate.validateForSave(this, validationResult);
        }
        super.validateForSave(validationResult);
    }
}
