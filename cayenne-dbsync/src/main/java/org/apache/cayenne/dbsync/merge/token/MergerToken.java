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

package org.apache.cayenne.dbsync.merge.token;

import org.apache.cayenne.dbsync.merge.context.MergeDirection;
import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;

/**
 * Represents a minimal atomic synchronization operation between database and Cayenne model.
 */
public interface MergerToken {

    String getTokenName();

    String getTokenValue();

    /**
     * The direction of this token. One of {@link MergeDirection#TO_DB} or
     * {@link MergeDirection#TO_MODEL}
     */
    MergeDirection getDirection();

    /**
     * Create a complimentary token with the reverse direction. AddColumn in one direction becomes
     * DropColumn in the other direction.
     * <p>
     * Not all tokens are reversible.
     */
    MergerToken createReverse(MergerTokenFactory factory);

    /**
     * Executes synchronization operation.
     *
     * @param context merge operation context.
     */
    void execute(MergerContext context);

    boolean isEmpty();

}
