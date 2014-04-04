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
package org.apache.cayenne.crypto.transformer.value;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

import org.apache.cayenne.crypto.transformer.bytes.BytesDecryptor;
import org.apache.cayenne.crypto.transformer.bytes.BytesEncryptor;
import org.apache.cayenne.crypto.unit.SwapBytesTransformer;
import org.junit.Before;
import org.junit.Test;

public class DefaultEncryptorTest {

    private BytesEncryptor encryptor;
    private BytesDecryptor decryptor;

    @Before
    public void before() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        this.encryptor = SwapBytesTransformer.encryptor();
        this.decryptor = SwapBytesTransformer.decryptor();
    }

    @Test
    public void testEncrypt_BytesToBytes() throws IllegalBlockSizeException, BadPaddingException {

        DefaultEncryptor e = new DefaultEncryptor(BytesToBytesConverter.INSTANCE, BytesToBytesConverter.INSTANCE);

        byte[] b1 = new byte[] { 1, 2 };
        byte[] b2 = new byte[] { 2, 3 };

        byte[] b1_t = (byte[]) e.encrypt(encryptor, b1);

        assertNotNull(b1_t);
        assertArrayEquals(b1, decryptor.decrypt(b1_t, 0, null));

        byte[] b2_t = (byte[]) e.encrypt(encryptor, b2);

        assertNotNull(b2_t);
        assertArrayEquals(b2, decryptor.decrypt(b2_t, 0, null));
    }

    @Test
    public void testEncrypt_BytesToBytes_null() throws IllegalBlockSizeException, BadPaddingException {

        DefaultEncryptor e = new DefaultEncryptor(BytesToBytesConverter.INSTANCE, BytesToBytesConverter.INSTANCE);
        assertNull(e.encrypt(encryptor, null));
    }

    @Test
    public void testEncrypt_StringToBytes() throws UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException {

        DefaultEncryptor e = new DefaultEncryptor(Utf8StringConverter.INSTANCE, BytesToBytesConverter.INSTANCE);

        String s1 = "ab";
        String s2 = "cd";

        byte[] b1_t = (byte[]) e.encrypt(encryptor, s1);

        assertNotNull(b1_t);
        assertEquals(s1, new String(decryptor.decrypt(b1_t, 0, null), Utf8StringConverter.DEFAULT_CHARSET));

        byte[] b2_t = (byte[]) e.encrypt(encryptor, s2);

        assertNotNull(b2_t);
        assertEquals(s2, new String(decryptor.decrypt(b2_t, 0, null), Utf8StringConverter.DEFAULT_CHARSET));
    }

    @Test
    public void testEncrypt_StringToString() throws UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException {

        DefaultEncryptor e = new DefaultEncryptor(Utf8StringConverter.INSTANCE, Base64StringConverter.INSTANCE);

        String s1 = "ab";

        // try to get beyond a single block boundary and a Base64 line...
        String s2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                + "Pellentesque nisi sapien, mattis eu porttitor in, tempus quis lorem. "
                + "Integer vel dignissim quam. Maecenas pellentesque est erat, eget semper ipsum aliquet vitae. "
                + "Donec convallis mi vitae luctus rutrum. Sed ut imperdiet ante. Praesent condimentum velit eget "
                + "felis pretium blandit. Praesent lacus tortor, facilisis eget sapien quis, hendrerit iaculis tellus.";

        String s1_t = (String) e.encrypt(encryptor, s1);

        assertNotNull(s1_t);
        assertNotEquals(s1_t, s1);

        byte[] b1_t = DatatypeConverter.parseBase64Binary(s1_t);
        assertEquals(s1, new String(decryptor.decrypt(b1_t, 0, null), Utf8StringConverter.DEFAULT_CHARSET));

        String s2_t = (String) e.encrypt(encryptor, s2);

        assertNotNull(s2_t);
        assertNotEquals(s2_t, s2);

        byte[] b2_t = DatatypeConverter.parseBase64Binary(s2_t);
        assertEquals(s2, new String(decryptor.decrypt(b2_t, 0, null), Utf8StringConverter.DEFAULT_CHARSET));
    }
}
