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
package org.apache.cayenne.merge;

public interface MergerToken {

    public String getTokenName();

    public String getTokenValue();

    /**
     * The direction of this token. One of {@link MergeDirection#TO_DB} or
     * {@link MergeDirection#TO_MODEL}
     */
    public MergeDirection getDirection();

    /**
     * Create a token with the reverse direction. AddColumn in one direction becomes
     * DropColumn in the other direction.
     * <p>
     * Not all tokens are reversible.
     */
    public MergerToken createReverse(MergerFactory factory);

    public void execute(MergerContext mergerContext);

}
