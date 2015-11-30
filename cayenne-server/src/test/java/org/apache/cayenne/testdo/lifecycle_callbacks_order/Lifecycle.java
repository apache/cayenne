package org.apache.cayenne.testdo.lifecycle_callbacks_order;

import org.apache.cayenne.testdo.lifecycle_callbacks_order.auto._Lifecycle;
import org.apache.cayenne.validation.ValidationResult;

public class Lifecycle extends _Lifecycle {

    private static final long serialVersionUID = 1L;
    private StringBuilder callbackBuffer = new StringBuilder();

    @Override
    public void validateForInsert(ValidationResult validationResult) {
        callbackBuffer.append("validateForInsert;");
        super.validateForInsert(validationResult);
    }

    @Override
    public void validateForUpdate(ValidationResult validationResult) {
        callbackBuffer.append("validateForUpdate;");
        super.validateForUpdate(validationResult);
    }

    @Override
    public void validateForDelete(ValidationResult validationResult) {
        callbackBuffer.append("validateForDelete;");
        super.validateForDelete(validationResult);
    }

    public StringBuilder getCallbackBuffer() {
        return callbackBuffer;
    }

    public String getCallbackBufferValueAndReset() {
        String v = callbackBuffer.toString();
        callbackBuffer = new StringBuilder();
        return v;
    }
}
