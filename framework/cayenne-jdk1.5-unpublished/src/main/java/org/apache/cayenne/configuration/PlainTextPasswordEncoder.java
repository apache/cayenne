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
 * The plain text password encoder passes the text of the database password
 * straight-through without any alteration. This is identical to the behavior of pre-3.0
 * versions of Cayenne, where the password was stored in the XML model in clear text.
 * 
 * @since 3.0
 */
public class PlainTextPasswordEncoder implements PasswordEncoding {

    public String decodePassword(String encodedPassword, String key) {
        return encodedPassword;
    }

    public String encodePassword(String normalPassword, String key) {
        return normalPassword;
    }
}
