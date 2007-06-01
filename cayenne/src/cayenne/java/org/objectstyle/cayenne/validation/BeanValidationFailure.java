/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.validation;

import java.util.Collection;

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * ValidationFailure implementation that described a failure of a single
 * named property of a Java Bean object.
 * 
 * @author Fabricio Voznika
 * @author Andrei Adamchik
 * @since 1.1
 */
public class BeanValidationFailure extends SimpleValidationFailure {

    protected String property;

    private static String validationMessage(String attribute, String message) {
        StringBuffer buffer = new StringBuffer(message.length() + attribute.length() + 5);
        buffer.append('\"').append(attribute).append("\" ").append(message);
        return buffer.toString();
    }

    /**
     * Returns a ValidationFailure if a collection attribute
     * of an object is null or empty.
     */
    public static ValidationFailure validateNotEmpty(
        Object bean,
        String attribute,
        Collection value) {

        if (value == null) {
            return new BeanValidationFailure(
                bean,
                attribute,
                validationMessage(attribute, " is required."));
        }

        if (value.isEmpty()) {
            return new BeanValidationFailure(
                bean,
                attribute,
                validationMessage(attribute, " can not be empty."));
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
            return validateNotEmpty(bean, attribute, (Collection) value);
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
            throw new CayenneRuntimeException(
                "Error validationg bean property: "
                    + bean.getClass().getName()
                    + "."
                    + attribute,
                ex);
        }
    }

    public static ValidationFailure validateNotNull(
        Object bean,
        String attribute,
        Object value) {

        if (value == null) {
            return new BeanValidationFailure(
                bean,
                attribute,
                validationMessage(attribute, " is required."));
        }

        return null;
    }

    public static ValidationFailure validateNotEmpty(
        Object bean,
        String attribute,
        String value) {
        if (value == null || value.length() == 0) {
            return new BeanValidationFailure(
                bean,
                attribute,
                validationMessage(attribute, " is a required field."));
        }
        return null;
    }

    /**
     * Creates new BeanValidationFailure.
     */
    public BeanValidationFailure(Object source, String property, Object error) {
        super(source, error);

        if (source == null && property != null) {
            throw new IllegalArgumentException("ValidationFailure cannot have 'property' when 'source' is null.");
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
    public String toString() {
        StringBuffer buffer = new StringBuffer();

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
