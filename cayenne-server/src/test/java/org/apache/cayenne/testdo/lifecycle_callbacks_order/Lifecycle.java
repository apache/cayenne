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
