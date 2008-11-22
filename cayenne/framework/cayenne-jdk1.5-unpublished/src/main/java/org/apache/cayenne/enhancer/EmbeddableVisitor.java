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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.Persistent;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

/**
 * Enhances classes passed through the visitor to add embeddable fields and methods needed
 * by Cayenne.
 * 
 * @since 3.0
 */
public class EmbeddableVisitor extends ClassAdapter {

    private static String OWNER_FEILD = "owner";
    private static String EMBEDDED_PROPERTY_FEILD = "embeddedProperty";

    protected EnhancementHelper helper;
    protected Collection<String> reservedFieldNames;
    protected boolean checkReserved;

    public EmbeddableVisitor(ClassVisitor visitor) {
        super(visitor);
        this.helper = new EnhancementHelper(this);
        this.checkReserved = true;

        this.reservedFieldNames = new ArrayList<String>(2);
        reservedFieldNames.add(helper.getPropertyField(OWNER_FEILD));
        reservedFieldNames.add(helper.getPropertyField(EMBEDDED_PROPERTY_FEILD));
    }

    /**
     * Checks that no double enhancement happens.
     */
    @Override
    public FieldVisitor visitField(
            int access,
            String name,
            String desc,
            String signature,
            Object value) {

        if (checkReserved && reservedFieldNames.contains(name)) {
            throw new DoubleEnhanceException("Embeddable class already contains field "
                    + name);
        }

        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {

        // 'checkReserved' flipping makes the visitor thread-unsafe... pay attention if
        // we are to ever reuse the visitor for multiple classes...

        checkReserved = false;

        try {
            helper.createField(Persistent.class, OWNER_FEILD);
            helper.createField(String.class, EMBEDDED_PROPERTY_FEILD);
        }
        finally {
            checkReserved = true;
        }
    }
}
