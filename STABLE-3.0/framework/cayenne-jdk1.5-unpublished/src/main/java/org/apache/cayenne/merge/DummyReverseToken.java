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

/**
 * The reverse of a {@link MergerToken} that can not be reversed.. This will not execute
 * any thing, but {@link #createReverse(MergerFactory)} will get back the reverse that
 * this was made from.
 */
class DummyReverseToken implements MergerToken {

    private MergerToken reverse;

    public DummyReverseToken(MergerToken reverse) {
        this.reverse = reverse;
    }

    public MergerToken createReverse(MergerFactory factory) {
        return reverse;
    }

    public void execute(MergerContext mergerContext) {
        // can not execute
    }

    public MergeDirection getDirection() {
        return reverse.getDirection().reverseDirection();
    }

    public String getTokenName() {
        return "Can not execute the reverse of " + reverse.getTokenName();
    }

    public String getTokenValue() {
        return reverse.getTokenValue();
    }

    public boolean isReversible() {
        return true;
    }

}
