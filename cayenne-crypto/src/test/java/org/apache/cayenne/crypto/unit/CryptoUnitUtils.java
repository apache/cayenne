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
package org.apache.cayenne.crypto.unit;

import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.crypto.transformer.bytes.Header;
import org.apache.cayenne.runtime.CayenneRuntime;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public class CryptoUnitUtils {

    public static byte[] bytesOfSize(int len) {
        Random r = new Random();
        byte[] b = new byte[len];
        r.nextBytes(b);
        return b;
    }

    public static byte[] hexToBytes(String hexString) {
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();

        // http://stackoverflow.com/questions/4407779/biginteger-to-byte
        if (bytes.length > 0 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        } else {
            return bytes;
        }
    }

    public static byte[] gunzip(byte[] source) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {

            GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(source));

            int read;
            byte[] buffer = new byte[1024];
            while ((read = gunzip.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    public static byte[] decrypt_AES_CBC(byte[] source, CayenneRuntime runtime) {

        try {

            Cipher decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            Header header = Header.create(source, 0);

            int offset = header.size();
            if(header.haveHMAC()) {
                byte hmacLength = source[offset];
                offset += hmacLength + 1;
            }

            int blockSize = decCipher.getBlockSize();
            byte[] ivBytes = Arrays.copyOfRange(source, offset, offset + blockSize);
            byte[] cipherText = Arrays.copyOfRange(source, offset + blockSize, source.length);

            Key key = runtime.getInjector().getInstance(KeySource.class).getKey(header.getKeyName());

            decCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivBytes));

            return decCipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toByteArray(int integer) {

        if (integer <= Short.MAX_VALUE) {
            return toByteArray((short) integer);
        }

        return ByteBuffer.allocate(4).putInt(integer).array();
    }

    public static byte[] toByteArray(short shortInt) {
        if (shortInt <= Byte.MAX_VALUE) {
            return toByteArray((byte) shortInt);
        }

        return ByteBuffer.allocate(2).putShort(shortInt).array();
    }

    public static byte[] toByteArray(byte byteInt) {
        return new byte[]{byteInt};
    }

}
