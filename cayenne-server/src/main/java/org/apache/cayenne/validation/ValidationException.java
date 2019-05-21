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

package org.apache.cayenne.validation;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * An exception thrown on unsuccessful validation.
 * 
 * @since 1.1
 */
public class ValidationException extends CayenneRuntimeException {

    private ValidationResult result;

    public ValidationException(String messageFormat, Object... messageArgs) {
        super(messageFormat, messageArgs);
    }

    public ValidationException(ValidationResult result) {
        // escape percent signs so they aren't interpreted as format specifiers when String.format is called later.
        this("Validation failures: " + result.toString().replace("%", "%%"), result);
    }

    public ValidationException(String messageFormat, ValidationResult result,
            Object... messageArgs) {
        super(messageFormat, messageArgs);
        this.result = result;
    }

    public ValidationResult getValidationResult() {
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + System.getProperty("line.separator") + this.result;
    }
}
