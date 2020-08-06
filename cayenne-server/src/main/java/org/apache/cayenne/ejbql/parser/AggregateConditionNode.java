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
package org.apache.cayenne.ejbql.parser;

/**
 * Superclass of aggregated conditional nodes such as NOT, AND, OR. 
 * Defines priority of operations, so that SQL can be supplied with brackets as needed
 * 
 * @since 3.0
 */
public abstract class AggregateConditionNode extends SimpleNode {
    //defining all priorities here so that it would be easy to compare them visually
    static final int NOT_PRIORITY = 10;
    static final int AND_PRIORITY = 20;
    static final int OR_PRIORITY = 30;
    
    public AggregateConditionNode(int id) {
        super(id);
    }
    
    /**
     * Returns priority of conditional operator. 
     * This is used to decide whether brackets are needed in resulting SQL 
     */
    public abstract int getPriority();
}
