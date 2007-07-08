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

public class MockPojo1 {

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

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public int getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(int attribute2) {
        this.attribute2 = attribute2;
    }

    public double getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(double attribute3) {
        this.attribute3 = attribute3;
    }

    public byte[] getByteArrayAttribute() {
        return byteArrayAttribute;
    }

    public void setByteArrayAttribute(byte[] attribute4) {
        this.byteArrayAttribute = attribute4;
    }

    public short getAttribute5() {
        return attribute5;
    }

    public void setAttribute5(short attribute5) {
        this.attribute5 = attribute5;
    }

    public char getAttribute6() {
        return attribute6;
    }

    public void setAttribute6(char attribute6) {
        this.attribute6 = attribute6;
    }

    public byte getAttribute7() {
        return attribute7;
    }

    public void setAttribute7(byte attribute7) {
        this.attribute7 = attribute7;
    }

    public boolean isAttribute8() {
        return attribute8;
    }

    public void setAttribute8(boolean attribute8) {
        this.attribute8 = attribute8;
    }

    public long getAttribute9() {
        return attribute9;
    }

    public void setAttribute9(long attribute9) {
        this.attribute9 = attribute9;
    }

    
    public float getAttribute10() {
        return attribute10;
    }

    
    public void setAttribute10(float attribute10) {
        this.attribute10 = attribute10;
    }
}
