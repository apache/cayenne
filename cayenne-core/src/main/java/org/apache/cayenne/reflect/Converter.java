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
 * A helper class to do property type conversions.
 * 
 * @since 1.2
 */
public abstract class Converter<T> {

	/**
	 * Converts object to supported class without doing any type checking.
	 * @param value 
	 * 		the object to convert; the source
	 * @param type
	 * 		the Class to convert the value to; the destination type
	 * @return
	 * 		an object of type @code{type}. If the conversion fails an exception will be thrown. If value is null then the result will be null.
	 */
    protected abstract T convert(Object value, Class<T> type);
}
