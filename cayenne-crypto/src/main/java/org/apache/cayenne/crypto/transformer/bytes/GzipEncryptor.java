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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * @since 3.2
 */
class GzipEncryptor implements BytesEncryptor {

    private BytesEncryptor delegate;
    private Header header;

    public GzipEncryptor(BytesEncryptor delegate, Header header) {
        this.delegate = delegate;
        this.header = header;
    }

    @Override
    public byte[] encrypt(byte[] input, int outputOffset) {

        // TODO: skip compression of small inputs... somehow flip compression
        // bit in the header back to off in that case.

        byte[] compressedIn;
        try {
            compressedIn = gzip(input);
        } catch (IOException e) {
            // really not expecting an error here...
            throw new CayenneCryptoException("Error compressing input", e);
        }

        return delegate.encrypt(compressedIn, outputOffset + header.size());
    }

    static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream(input.length);
        GZIPOutputStream out = new GZIPOutputStream(zipBytes);

        out.write(input, 0, input.length);
        out.close();

        return zipBytes.toByteArray();
    }

}
