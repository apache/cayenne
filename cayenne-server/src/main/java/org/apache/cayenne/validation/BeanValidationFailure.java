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

import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.reflect.PropertyUtils;

/**
 * ValidationFailure implementation that described a failure of a single named property of
 * a Java Bean object.
 * 
 * @since 1.1
 */
public class BeanValidationFailure extends SimpleValidationFailure {

    protected String property;

    private static String validationMessage(String attribute, String message) {
        StringBuilder buffer = new StringBuilder(message.length() + attribute.length() + 5);
        buffer.append('\"').append(attribute).append("\" ").append(message);
        return buffer.toString();
    }

    /**
     * Returns a ValidationFailure if a collection attribute of an object is null or
     * empty.
     */
    public static ValidationFailure validateNotEmpty(
            Object bean,
            String attribute,
            Collection<?> value) {

        if (value == null) {
            return new BeanValidationFailure(bean, attribute, validationMessage(
                    attribute,
                    " is required."));
        }

        if (value.isEmpty()) {
            return new BeanValidationFailure(bean, attribute, validationMessage(
                    attribute,
                    " can not be empty."));
        }

        return null;
    }

    public static ValidationFailure validateMandatory(
            Object bean,
            String attribute,
            Object value) {

        if (value instanceof String) {
            return validateNotEmpty(bean, attribute, (String) value);
        }
        if (value instanceof Collection) {
            return validateNotEmpty(bean, attribute, (Collection<?>) value);
        }
        return validateNotNull(bean, attribute, value);
    }

    public static ValidationFailure validateMandatory(Object bean, String attribute) {
        if (bean == null) {
            throw new NullPointerException("Null bean.");
        }

        try {
            Object result = PropertyUtils.getProperty(bean, attribute);
            return validateMandatory(bean, attribute, result);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error validationg bean property: "
                    + bean.getClass().getName()
                    + "."
                    + attribute, ex);
        }
    }

    public static ValidationFailure validateNotNull(
            Object bean,
            String attribute,
            Object value) {

        if (value == null) {
            return new BeanValidationFailure(bean, attribute, validationMessage(
                    attribute,
                    " is required."));
        }

        return null;
    }

    /**
     * A utility method that returns a ValidationFailure if a string is either null or has
     * a length of zero; otherwise returns null.
     */
    public static ValidationFailure validateNotEmpty(
            Object bean,
            String attribute,
            String value) {

        if (value == null || value.length() == 0) {
            return new BeanValidationFailure(bean, attribute, validationMessage(
                    attribute,
                    " is a required field."));
        }
        return null;
    }

    /**
     * A utility method that checks that a given string is a valid Java full class name,
     * returning a non-null ValidationFailure if this is not so. 
     * 
     * Special case: primitive arrays like byte[] are also handled as a valid java 
     * class name.
     * 
     * @since 1.2
     */
    public static ValidationFailure validateJavaClassName(
            Object bean,
            String attribute,
            String identifier) {

        ValidationFailure emptyFailure = validateNotEmpty(bean, attribute, identifier);
        if (emptyFailure != null) {
            return emptyFailure;
        }

        char c = identifier.charAt(0);
        if (!Character.isJavaIdentifierStart(c)) {
            return new BeanValidationFailure(bean, attribute, validationMessage(
                    attribute,
                    " starts with invalid character: " + c));
        }

        // handle arrays
        if (identifier.endsWith("[]")) {
            identifier = identifier.substring(0, identifier.length() - 2);
        }

        boolean wasDot = false;
        for (int i = 1; i < identifier.length(); i++) {
            c = identifier.charAt(i);

            if (c == '.') {
                if (wasDot || i + 1 == identifier.length()) {
                    return new BeanValidationFailure(bean, attribute, validationMessage(
                            attribute,
                            " is not a valid Java Class Name: " + identifier));
                }

                wasDot = true;
                continue;
            }

            if (!Character.isJavaIdentifierPart(c)) {
                return new BeanValidationFailure(bean, attribute, validationMessage(
                        attribute,
                        " contains invalid character: " + c));
            }

            wasDot = false;
        }

        return null;
    }

    /**
     * Creates new BeanValidationFailure.
     */
    public BeanValidationFailure(Object source, String property, Object error) {
        super(source, error);

        if (source == null && property != null) {
            throw new IllegalArgumentException(
                    "ValidationFailure cannot have 'property' when 'source' is null.");
        }

        this.property = property;
    }

    /**
     * Returns a failed property of the failure source object.
     */
    public String getProperty() {
        return property;
    }

    /**
     * Returns a String representation of the failure.
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("Validation failure for ");
        Object source = getSource();

        if (source == null) {
            buffer.append("[General]");
        }
        else {
            String property = getProperty();
            buffer.append(source.getClass().getName()).append('.').append(
                    (property == null ? "[General]" : property));
        }
        buffer.append(": ");
        buffer.append(getDescription());
        return buffer.toString();
    }
}
