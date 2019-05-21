/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.tutorial;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.rop.client.ClientConstants;
import org.apache.cayenne.configuration.rop.client.ClientJettyHttp2Module;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.configuration.rop.client.ProtostuffModule;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.rop.JettyHttp2ClientConnectionProvider;
import org.apache.cayenne.rop.http.JettyHttpROPConnector;
import org.apache.cayenne.rop.protostuff.ProtostuffROPSerializationService;
import org.apache.cayenne.tutorial.persistent.client.Artist;
import org.apache.cayenne.tutorial.persistent.client.Gallery;
import org.apache.cayenne.tutorial.persistent.client.Painting;

import javax.net.ssl.HttpsURLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This example uses {@link ProtostuffROPSerializationService} which is bound in {@link ProtostuffModule}
 * and {@link JettyHttpROPConnector} initialized by {@link JettyHttp2ClientConnectionProvider}, which is bound
 * in {@link ClientJettyHttp2Module}. It works without ALPN by default.
 * <p>
 * In order to run it with ALPN, you have to set {@link ClientConstants#ROP_SERVICE_USE_ALPN_PROPERTY} to true
 * and provide the alpn-boot-XXX.jar into the bootstrap classpath.
 */
public class Http2Client {

    public static void main(String[] args) throws Exception {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> hostname.equals("localhost"));
        System.setProperty("javax.net.ssl.trustStore", Http2Client.class.getResource("/keystore").getPath());

        // Setting Protostuff properties
        System.setProperty("protostuff.runtime.collection_schema_on_repeated_fields", "true");
        System.setProperty("protostuff.runtime.morph_collection_interfaces", "true");
        System.setProperty("protostuff.runtime.morph_map_interfaces", "true");
        System.setProperty("protostuff.runtime.pojo_schema_on_collection_fields", "true");
        System.setProperty("protostuff.runtime.pojo_schema_on_map_fields", "true");

        Map<String, String> properties = new HashMap<>();
        properties.put(ClientConstants.ROP_SERVICE_URL_PROPERTY, "https://localhost:8443/");
        properties.put(ClientConstants.ROP_SERVICE_USE_ALPN_PROPERTY, "false");
        properties.put(ClientConstants.ROP_SERVICE_USERNAME_PROPERTY, "cayenne-user");
        properties.put(ClientConstants.ROP_SERVICE_PASSWORD_PROPERTY, "secret");
        properties.put(ClientConstants.ROP_SERVICE_REALM_PROPERTY, "Cayenne Realm");

        ClientRuntime runtime = ClientRuntime.builder()
                            .properties(properties)
                            .addModule(new ClientJettyHttp2Module())
                            .build();

        ObjectContext context = runtime.newContext();

        newObjectsTutorial(context);
        selectTutorial(context);
        deleteTutorial(context);

        runtime.shutdown();
    }

    static void newObjectsTutorial(ObjectContext context) {

        // creating new Artist
        Artist picasso = context.newObject(Artist.class);
        picasso.setName("Pablo Picasso");

        // Creating other objects
        Gallery metropolitan = context.newObject(Gallery.class);
        metropolitan.setName("Metropolitan Museum of Art");

        Painting girl = context.newObject(Painting.class);
        girl.setName("Girl Reading at a Table");

        Painting stein = context.newObject(Painting.class);
        stein.setName("Gertrude Stein");

        // connecting objects together via relationships
        picasso.addToPaintings(girl);
        picasso.addToPaintings(stein);

        girl.setGallery(metropolitan);
        stein.setGallery(metropolitan);

        // saving all the changes above
        context.commitChanges();
    }

    static void selectTutorial(ObjectContext context) {
        // ObjectSelect examples
        List<Painting> paintings1 = ObjectSelect.query(Painting.class).select(context);

        List<Painting> paintings2 = ObjectSelect.query(Painting.class)
                .where(Painting.NAME.likeIgnoreCase("gi%")).select(context);
    }

    static void deleteTutorial(ObjectContext context) {
        // Delete object example
        Artist picasso = ObjectSelect.query(Artist.class).where(Artist.NAME.eq("Pablo Picasso")).selectOne(context);

        if (picasso != null) {
            context.deleteObjects(picasso);
            context.commitChanges();
        }
    }
}
