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

import org.apache.cayenne.dba.DbAdapter;

public abstract class AbstractMergerToken implements MergerToken {

    private MergeDirection direction;

    public AbstractMergerToken(MergeDirection direction) {
        this.direction = direction;
    }

    public MergeDirection getDirection() {
        return direction;
    }

    /**
     * Return a direction value for the reverse direction.
     */
    protected MergeDirection reverseDirection() {
        return getDirection().reverseDirection();
    }

    public abstract String createSql(DbAdapter adapter);

    public abstract String getTokenName();

    public abstract String getTokenValue();
    
}
