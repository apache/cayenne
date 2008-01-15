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
package org.apache.cayenne.dba;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class LongPkRange {

    private long curValue;
    private long maxValue;

    LongPkRange(long curValue, long maxValue) {
        reset(curValue, maxValue);
    }

    void reset(long curValue, long maxValue) {
        this.curValue = curValue;
        this.maxValue = maxValue;
    }

    boolean isExhausted() {
        return curValue > maxValue;
    }

    long getNextPrimaryKey() {
        // do bound checking
        if (isExhausted()) {
            throw new RuntimeException(
                    "PkRange is exhausted and can not be used anymore.");
        }

        return curValue++;
    }
}
