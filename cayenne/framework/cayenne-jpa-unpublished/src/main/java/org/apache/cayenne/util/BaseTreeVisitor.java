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

package org.apache.cayenne.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.project.ProjectPath;

/**
 * A common superclass for specialized tree visitors. Can also be used as a noop
 * pass-through visitor for nodes that need no processing by themselves.
 * 
 */
// TODO, andrus, 4/24/2006 - move to Cayenne core in 2.0
public class BaseTreeVisitor implements HierarchicalTreeVisitor {

    protected Map<String, HierarchicalTreeVisitor> childVisitors;
    protected boolean terminatingOnNoChildVisitor;

    public BaseTreeVisitor() {
        this.terminatingOnNoChildVisitor = true;
    }

    public boolean isTerminatingOnNoChildVisitor() {
        return terminatingOnNoChildVisitor;
    }

    public void setTerminatingOnNoChildVisitor(boolean terminatingOnNoChildVisitor) {
        this.terminatingOnNoChildVisitor = terminatingOnNoChildVisitor;
    }

    public HierarchicalTreeVisitor childVisitor(ProjectPath path, Class<?> childType) {
        if (childVisitors == null) {
            return terminatingOnNoChildVisitor ? null : this;
        }

        HierarchicalTreeVisitor childVisitor = childVisitors.get(childType.getName());
        return childVisitor != null ? childVisitor : terminatingOnNoChildVisitor
                ? null
                : this;
    }

    public void onFinishNode(ProjectPath path) {
    }

    public boolean onStartNode(ProjectPath path) {
        return true;
    }

    public void addChildVisitor(Class<?> childClass, HierarchicalTreeVisitor visitor) {
        if (childVisitors == null) {
            childVisitors = new HashMap<String, HierarchicalTreeVisitor>();
        }

        childVisitors.put(childClass.getName(), visitor);
    }
}
