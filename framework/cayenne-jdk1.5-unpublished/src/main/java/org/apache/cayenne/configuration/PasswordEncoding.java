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

/**
 * Password encoders are used to translate the text of the database password, on loading
 * and on saving, from one form to another. It can facilitate the obscuring of the
 * password text to make database connection information less obvious to someone who
 * stumbles onto the password. Cayenne only includes facilities to obscure, not encrypt,
 * the database password. The mechanism is user-extensible, though, so should stronger
 * security features be required, they can be added and integrated into both the modeler
 * and framework.
 * 
 * @since 3.0
 */
public interface PasswordEncoding {

    final String[] standardEncoders = new String[] {
            PlainTextPasswordEncoder.class.getName(),
            Rot13PasswordEncoder.class.getName(), Rot47PasswordEncoder.class.getName()
    };

    /**
     * Decodes an encoded database password.
     * 
     * @param encodedPassword - The encoded password to be decoded
     * @param key - An optional data element which can be used to unlock the password.
     *            Some encoders may require the key.
     * @return The decoded normal/plain plassword.
     */
    String decodePassword(String encodedPassword, String key);

    /**
     * Encodes a normal/plain database password.
     * 
     * @param normalPassword - The normal/plain password to be encoded
     * @param key - An optional data element which can be used to lock the password. Some
     *            encoders may require the key.
     * @return The encoded password.
     */
    String encodePassword(String normalPassword, String key);
}
