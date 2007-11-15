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

package org.apache.cayenne.exp;

import junit.framework.Assert;

import org.apache.cayenne.TranslationCase;

public class TstExpressionCase extends TranslationCase {    
    protected int totalNodes;
    protected int totalLeaves;
    
    public TstExpressionCase(String rootEntity, Expression cayenneExp, String sqlExp, int totalNodes, int totalLeaves) {
        super(rootEntity, cayenneExp, sqlExp);
        this.totalNodes = totalNodes;
        this.totalLeaves = totalLeaves;
    }
    
    
    public int getTotalNodes() {
        return totalNodes;
    }
    
    
    public int getTotalLeaves() {
        return totalLeaves;
    }
    
    
    public Expression getCayenneExp() {
        return (Expression)tstObject;
    }
    
    public void assertParsedWell(int totalNodes, int totalLeaves) {
        Assert.assertEquals(this.totalNodes, totalNodes);
        Assert.assertEquals(this.totalLeaves, totalLeaves);
    }
}
