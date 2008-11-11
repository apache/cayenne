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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;

/**
 * This class in combination with the ASM Eclipse plugin is used as a reference for
 * building parts of the ASM enhancer. It demonstrates how a pojo should look like after
 * the enhancement.
 * 
 */
public class MockPojo1Enhanced implements Persistent {

    protected ObjectId $cay_objectId;
    protected int $cay_persistenceState = PersistenceState.TRANSIENT;
    protected transient ObjectContext $cay_objectContext;

    protected String attribute1;
    protected int attribute2;
    protected double attribute3;
    protected short attribute5;
    protected char attribute6;
    protected byte attribute7;
    protected boolean attribute8;
    protected long attribute9;
    protected float attribute10;

    protected byte[] byteArrayAttribute;

    public int getPersistenceState() {
        return $cay_persistenceState;
    }

    public void setPersistenceState(int persistenceState) {
        this.$cay_persistenceState = persistenceState;
    }

    public ObjectContext getObjectContext() {
        return $cay_objectContext;
    }

    public void setObjectContext(ObjectContext objectContext) {
        this.$cay_objectContext = objectContext;
    }

    public ObjectId getObjectId() {
        return $cay_objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.$cay_objectId = objectId;
    }

    public String getAttribute1() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute1", false);
        }
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute1",
                    this.attribute1,
                    attribute1);
        }

        this.attribute1 = attribute1;
    }

    public int getAttribute2() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute2", false);
        }
        return attribute2;
    }

    public void setAttribute2(int attribute2) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(this, "attribute2", Integer
                    .valueOf(this.attribute2), Integer.valueOf(attribute2));
        }

        this.attribute2 = attribute2;
    }

    public double getAttribute3() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute3", false);
        }
        return attribute3;
    }

    public void setAttribute3(double attribute3) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(this, "attribute3", Double
                    .valueOf(this.attribute3), Double.valueOf(attribute3));
        }

        this.attribute3 = attribute3;
    }

    public byte[] getByteArrayAttribute() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute4", false);
        }
        return byteArrayAttribute;
    }

    public void setByteArrayAttribute(byte[] attribute4) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute4",
                    this.byteArrayAttribute,
                    attribute4);
        }

        this.byteArrayAttribute = attribute4;
    }

    public short getAttribute5() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute5", false);
        }

        return attribute5;
    }

    public void setAttribute5(short attribute5) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute5",
                    this.attribute5,
                    attribute5);
        }

        this.attribute5 = attribute5;
    }

    public char getAttribute6() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute6", false);
        }

        return attribute6;
    }

    public void setAttribute6(char attribute6) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute6",
                    this.attribute6,
                    attribute6);
        }

        this.attribute6 = attribute6;
    }

    public byte getAttribute7() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute7", false);
        }

        return attribute7;
    }

    public void setAttribute7(byte attribute7) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute7",
                    this.attribute7,
                    attribute7);
        }
        
        this.attribute7 = attribute7;
    }

    public boolean isAttribute8() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute8", false);
        }

        return attribute8;
    }

    public void setAttribute8(boolean attribute8) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute8",
                    this.attribute8,
                    attribute8);
        }
        
        this.attribute8 = attribute8;
    }

    public long getAttribute9() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute9", false);
        }

        return attribute9;
    }

    public void setAttribute9(long attribute9) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute9",
                    this.attribute9,
                    attribute9);
        }
        
        this.attribute9 = attribute9;
    }

    
    public float getAttribute10() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "attribute10", false);
        }
        
        return attribute10;
    }

    
    public void setAttribute10(float attribute10) {
        if ($cay_objectContext != null) {
            $cay_objectContext.propertyChanged(
                    this,
                    "attribute10",
                    this.attribute10,
                    attribute10);
        }
        
        this.attribute10 = attribute10;
    }
}
