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

package org.apache.cayenne.reflect;

/**
 * JDK 15 friendly converter factory.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ConverterFactory15 extends ConverterFactory {

    private EnumConverter enumConveter = new EnumConverter();

    @Override
    Converter getConverter(Class type) {
        if (type == null) {
            throw new IllegalArgumentException("Null type");
        }
        
        // check for enum BEFORE super call, as it will return a noop converter
        if (type.isEnum()) {
            return enumConveter;
        }

        return super.getConverter(type);
    }
}
