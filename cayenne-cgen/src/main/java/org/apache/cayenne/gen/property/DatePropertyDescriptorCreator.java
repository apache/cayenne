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
package org.apache.cayenne.gen.property;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.gen.PropertyDescriptor;

/**
 * @since 4.2
 */
public class DatePropertyDescriptorCreator implements PropertyDescriptorCreator {

    private static final List<Class<?>> JAVA_DATE_TYPES = Arrays.asList(
            java.util.Date.class,
            java.time.LocalDate.class,
            java.time.LocalTime.class,
            java.time.LocalDateTime.class,
            java.sql.Date.class,
            java.sql.Time.class,
            java.sql.Timestamp.class
    );

    private static final String FACTORY_METHOD = "PropertyFactory.createDate";

    @Override
    public Optional<PropertyDescriptor> apply(Class<?> aClass) {
        if (JAVA_DATE_TYPES.contains(aClass)) {
            return Optional.of(new PropertyDescriptor(DateProperty.class.getName(), FACTORY_METHOD));
        }
        return Optional.empty();
    }
}
