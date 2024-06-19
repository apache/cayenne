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
package org.apache.cayenne.swing.components.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Objects;

/**
 * @since 5.0
 */
public class ChangeOptimizingTreeModel extends DefaultTreeModel {

    public ChangeOptimizingTreeModel(TreeNode root) {
        super(root);
    }

    public ChangeOptimizingTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            Object value = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (!Objects.equals(value, newValue)) {
                super.valueForPathChanged(path, newValue);
            }
        } else {
            super.valueForPathChanged(path, newValue);
        }
    }
}
