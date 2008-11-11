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

package org.apache.cayenne.unit.jira;

/**
 */
public class CAY_207String2 {

    protected String string;

    public CAY_207String2(String string) {
        // mock deserialization behavior... if the raw data is invalid, an exception
        // should be thrown
        if (string != null && !string.startsWith("T2")) {
            throw new IllegalArgumentException(string);
        }

        this.string = string;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + string;
    }
}
