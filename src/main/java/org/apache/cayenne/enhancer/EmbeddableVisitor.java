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
package org.apache.cayenne.enhancer;

import org.apache.cayenne.Persistent;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;

/**
 * Enhances classes passed through the visitor to add embeddable fields and methods needed
 * by Cayenne.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EmbeddableVisitor extends ClassAdapter {

    protected EnhancementHelper helper;

    public EmbeddableVisitor(ClassVisitor visitor) {
        super(visitor);
        this.helper = new EnhancementHelper(this);
    }

    @Override
    public void visitEnd() {
        helper.createField(Persistent.class, "owner");
        helper.createField(String.class, "embeddedProperty");
    }
}
