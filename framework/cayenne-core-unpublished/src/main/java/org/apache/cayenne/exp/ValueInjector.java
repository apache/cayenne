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
package org.apache.cayenne.exp;

/**
 * Describes expressions, that can "inject" value to an object, i.e.
 * do something reverse to "match" method. Preferrably, after
 * 
 * <code>valueInjectorExpression.inject(object)</code>,
 * valueInjectorExpression.match(object) returns <code>true</code>,
 * but this is not a requirement, e.g. "and" expression can be able
 * to inject only part of its operands
 */
public interface ValueInjector {
    void injectValue(Object o);
}
