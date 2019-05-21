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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;

/**
 * @since 4.2
 */
public class AliasedNode extends Node {

    protected final String alias;

    public AliasedNode(String alias) {
        this.alias = alias;
    }

    @Override
    public Node copy() {
        return new AliasedNode(alias);
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        return buffer;
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable buffer) {
        super.appendChildrenEnd(buffer);
        buffer.append(' ').append(alias);
    }

    public String getAlias() {
        return alias;
    }

}
