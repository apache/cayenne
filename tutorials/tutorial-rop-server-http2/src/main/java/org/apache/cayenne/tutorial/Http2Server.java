/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.tutorial;

import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import static org.eclipse.jetty.util.resource.Resource.newClassPathResource;

/**
 * Based on the example org.eclipse.jetty.embedded.Http2Server included in the jetty-project distribution.
 * <p>
 * This server works without ALPN and could handle only HTTP/2 protocol.
 */
public class Http2Server {

    public static void main(String... args) throws Exception {
        // Setting Protostuff properties
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
        System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
        System.setProperty("protostuff.runtime.pojo_schema_on_collection_fields", "true");
        System.setProperty("protostuff.runtime.pojo_schema_on_map_fields", "true");

        Server server = new Server();

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder("cayenne-project", new Http2ROPServlet()), "/");
        context.setSecurityHandler(basicAuth("cayenne-user", "secret", "Cayenne Realm"));
        server.setHandler(context);

        // HTTPS Configuration
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(8443);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // SSL Context Factory for HTTPS and HTTP/2
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreResource(newClassPathResource("keystore"));
        sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

        // SSL Connection Factory
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, "h2");

        // HTTP/2 Connector
        ServerConnector http2Connector = new ServerConnector(server, ssl, new HTTP2ServerConnectionFactory(httpsConfig));
        http2Connector.setPort(8443);
        server.addConnector(http2Connector);

        server.start();
        server.join();
    }

    private static SecurityHandler basicAuth(String username, String password, String realm) {
        HashLoginService loginService = new HashLoginService();
        UserStore userStore = new UserStore();
        userStore.addUser(username, Credential.getCredential(password), new String[]{"cayenne-service-user"});
        loginService.setUserStore(userStore);
        loginService.setName(realm);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"cayenne-service-user"});
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();
        constraintSecurityHandler.setAuthenticator(new BasicAuthenticator());
        constraintSecurityHandler.setRealmName(realm);
        constraintSecurityHandler.addConstraintMapping(constraintMapping);
        constraintSecurityHandler.setLoginService(loginService);

        return constraintSecurityHandler;
    }
}