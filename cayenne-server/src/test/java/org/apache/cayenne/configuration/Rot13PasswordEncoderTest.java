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

package org.apache.cayenne.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Rot13PasswordEncoderTest {

    private final String message = "The Quick Brown Fox Jumps Over The Lazy Dog";
    private final String encoded = "Gur Dhvpx Oebja Sbk Whzcf Bire Gur Ynml Qbt";

    @Test
    public void testEncode() {
        Rot13PasswordEncoder encoder = new Rot13PasswordEncoder();
        assertEquals(encoded, encoder.encodePassword(message, null));
    }

    @Test
    public void testDecode() {
        Rot13PasswordEncoder encoder = new Rot13PasswordEncoder();
        assertEquals(message, encoder.decodePassword(encoded, null));
    }

}
