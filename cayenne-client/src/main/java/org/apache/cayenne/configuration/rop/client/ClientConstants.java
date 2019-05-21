/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.configuration.rop.client;

/**
 * Defines the names of runtime properties and named collections used in DI modules related to ROP client.
 *
 * @since 4.0
 */
public class ClientConstants {

    public static final String ROP_SERVICE_URL_PROPERTY = "cayenne.rop.service_url";

    public static final String ROP_SERVICE_USERNAME_PROPERTY = "cayenne.rop.service_username";

    public static final String ROP_SERVICE_PASSWORD_PROPERTY = "cayenne.rop.service_password";

    public static final String ROP_SERVICE_REALM_PROPERTY = "cayenne.rop.service_realm";

    /**
     * A boolean property that defines whether ALPN should be used. Possible values are "true" or "false".
     */
    public static final String ROP_SERVICE_USE_ALPN_PROPERTY = "cayenne.rop.service_use_alpn";

    public static final String ROP_SERVICE_SHARED_SESSION_PROPERTY = "cayenne.rop.shared_session_name";

    public static final String ROP_SERVICE_TIMEOUT_PROPERTY = "cayenne.rop.service_timeout";

    public static final String ROP_CHANNEL_EVENTS_PROPERTY = "cayenne.rop.channel_events";

    public static final String ROP_CONTEXT_CHANGE_EVENTS_PROPERTY = "cayenne.rop.context_change_events";

    public static final String ROP_CONTEXT_LIFECYCLE_EVENTS_PROPERTY = "cayenne.rop.context_lifecycle_events";
}
