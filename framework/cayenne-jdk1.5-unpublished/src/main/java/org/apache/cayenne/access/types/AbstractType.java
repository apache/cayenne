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

package org.apache.cayenne.access.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A convenience superclass of ExtendedType implementations. Implements
 * {@link #setJdbcObject(PreparedStatement, Object, int, int, int)}in a generic fashion by
 * calling "setObject(..)" on PreparedStatement. Some adapters may need to override this
 * behavior as it doesn't work consistently across all JDBC drivers.
 * 
 * @deprecated since 3.0, as not common superclass for ExtendedTypes is deemed necessary.
 */
public abstract class AbstractType implements ExtendedType {

    /**
     * Helper method for ExtendedType implementors to check for null required values.
     * 
     * @since 1.2
     * @deprecated since 3.0 as validation should not be done at the DataNode level.
     */
    public static boolean validateNull(
            Object source,
            String property,
            Object value,
            DbAttribute dbAttribute,
            ValidationResult validationResult) {
        if (dbAttribute.isMandatory() && value == null) {
            validationResult.addFailure(new BeanValidationFailure(source, property, "'"
                    + property
                    + "' must be not null"));
            return false;
        }

        return true;
    }

    /**
     * Calls "PreparedStatement.setObject(..)". Some DbAdapters may need to override this
     * behavior for at least some of the object types, as it doesn't work consistently
     * across all JDBC drivers.
     */
    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int scale) throws Exception {

        if (scale != -1) {
            st.setObject(pos, val, type, scale);
        }
        else {
            st.setObject(pos, val, type);
        }
    }

    public abstract String getClassName();

    public abstract Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception;

    public abstract Object materializeObject(ResultSet rs, int index, int type)
            throws Exception;

    /**
     * Always returns true. Simplifies subclass implementation, as only some of the types
     * can perform the validation.
     * 
     * @deprecated since 3.0 as validation should not be done at the DataNode level.
     */
    public boolean validateProperty(
            Object source,
            String property,
            Object value,
            DbAttribute dbAttribute,
            ValidationResult validationResult) {
        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("className", getClassName()).toString();
    }

}
