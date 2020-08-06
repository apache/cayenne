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
package org.apache.cayenne.tutorial.persistent.client;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.rop.client.ClientConstants;
import org.apache.cayenne.configuration.rop.client.ClientJettyHttp2Module;
import org.apache.cayenne.configuration.rop.client.ClientJettyHttpModule;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.query.ObjectSelect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        Map<String, String> properties = new HashMap<>();
        properties.put(ClientConstants.ROP_SERVICE_URL_PROPERTY, "http://localhost:8080/cayenne-service");
        properties.put(ClientConstants.ROP_SERVICE_USERNAME_PROPERTY, "cayenne-user");
        properties.put(ClientConstants.ROP_SERVICE_PASSWORD_PROPERTY, "secret");
        properties.put(ClientConstants.ROP_SERVICE_REALM_PROPERTY, "Cayenne Realm");

        ClientRuntime runtime = ClientRuntime.builder()
                                .properties(properties)
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
