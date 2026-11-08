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
package org.apache.cayenne.map.compiled;


/**
 * A compiled parameter of a stored procedure.
 *
 * @param name      the parameter name
 * @param jdbcType  the JDBC type
 * @param direction one of {@link #IN_PARAMETER}, {@link #OUT_PARAMETER}, {@link #IN_OUT_PARAMETER}
 * @param maxLength the length of a character or binary parameter, or the max digits of a decimal, or -1
 * @param precision the digits after the period of a decimal parameter, or -1
 * @since 6.0
 */
public record CompiledProcedureParameter(String name, int jdbcType, int direction, int maxLength, int precision) {

    public static final int IN_PARAMETER = 1;
    public static final int OUT_PARAMETER = 2;
    public static final int IN_OUT_PARAMETER = 3;

    public boolean isInParameter() {
        return direction == IN_PARAMETER || direction == IN_OUT_PARAMETER;
    }

    public boolean isOutParameter() {
        return direction == OUT_PARAMETER || direction == IN_OUT_PARAMETER;
    }
}
