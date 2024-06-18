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

package org.apache.cayenne.util;

import java.util.List;

/**
 * A QueryResponse that contains a sublist of the query result. Also contains extra
 * information about the full list.
 * 
 * @since 1.2
 */
public class IncrementalListResponse extends ListResponse {

    protected int fullSize;

    public IncrementalListResponse(List objectList, int fullSize) {
        super(objectList);

        this.fullSize = fullSize;
    }

    public int getFullSize() {
        return fullSize;
    }
}
