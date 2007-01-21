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

package org.apache.cayenne.jpa.map;

import org.apache.cayenne.util.TreeNodeChild;

public abstract class JpaManagedClass {

    protected JpaClassDescriptor classDescriptor;

    protected String className;
    protected AccessType access;
    protected boolean metadataComplete;
    protected String description;

    protected JpaAttributes attributes;

    public JpaClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    public void setClassDescriptor(JpaClassDescriptor classDescriptor) {
        this.classDescriptor = classDescriptor;
    }

    public AccessType getAccess() {
        return access;
    }

    public void setAccess(AccessType access) {
        this.access = access;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isMetadataComplete() {
        return metadataComplete;
    }

    public void setMetadataComplete(boolean metadataComplete) {
        this.metadataComplete = metadataComplete;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @TreeNodeChild
    public JpaAttributes getAttributes() {
        if (attributes == null) {
            attributes = new JpaAttributes();
        }
        return attributes;
    }

    public void setAttributes(JpaAttributes attributes) {
        this.attributes = attributes;
    }
}
