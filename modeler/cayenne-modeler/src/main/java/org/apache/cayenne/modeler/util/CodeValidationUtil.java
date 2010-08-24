/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;

// TODO, andrus 4/13/2006 - merge with BeanValidationFailure.
public class CodeValidationUtil {

    private static String validationMessage(String attribute, String message) {
        StringBuffer buffer = new StringBuffer(message.length() + attribute.length() + 5);
        buffer.append('\"').append(attribute).append("\" ").append(message);
        return buffer.toString();
    }

    public static ValidationFailure validateJavaIdentifier(
            Object bean,
            String attribute,
            String identifier) {

        ValidationFailure emptyFailure = BeanValidationFailure.validateNotEmpty(
                bean,
                attribute,
                identifier);
        
        if (emptyFailure != null) {
            return emptyFailure;
        }

        char c = identifier.charAt(0);
        if (!Character.isJavaIdentifierStart(c)) {
            return new BeanValidationFailure(bean, attribute, validationMessage(
                    attribute,
                    " starts with invalid character: " + c));
        }

        for (int i = 1; i < identifier.length(); i++) {
            c = identifier.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                return new BeanValidationFailure(bean, attribute, validationMessage(
                        attribute,
                        " contains invalid character: " + c));
            }

        }

        return null;
    }
}
