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

package org.apache.cayenne.dbsync.merge.token;

/**
 * @since 4.0
 */
public abstract class AbstractMergerToken implements MergerToken {

    private final String tokenName;

    private final int sortingWeight;

    protected AbstractMergerToken(String tokenName, int sortingWeight) {
        this.tokenName = tokenName;
        this.sortingWeight = sortingWeight;
    }

    @Override
    public final String getTokenName() {
        return tokenName;
    }

    @Override
    public int getSortingWeight() {
        return sortingWeight;
    }

    @Override
    public int compareTo(MergerToken o) {
        return getSortingWeight() - o.getSortingWeight();
    }

    @Override
    public String toString() {
        return getTokenName() + ' ' + getTokenValue() + ' ' + getDirection();
    }

    public boolean isEmpty() {
        return false;
    }
}
