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
package org.apache.cayenne.crypto.transformer.bytes;

public class EncryptorWithKeyName implements BytesTransformer {

    private BytesTransformer delegate;
    private int blockSize;
    private byte[] keyName;

    public EncryptorWithKeyName(BytesTransformer delegate, byte[] keyName, int blockSize) {
        this.delegate = delegate;
        this.blockSize = blockSize;
        this.keyName = keyName;
    }

    @Override
    public int getOutputSize(int inputLength) {
        // add one block for key name storage
        return blockSize + delegate.getOutputSize(inputLength);
    }

    @Override
    public void transform(byte[] input, byte[] output, int outputOffset) {
        System.arraycopy(keyName, 0, output, outputOffset, blockSize);
        delegate.transform(input, output, outputOffset + blockSize);
    }

}
