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
// The parts of this class are included in the tutorial docs. "main-empty" and "main-runtime" tags assemble the versions
// of this class at the different tutorial steps. The class is only compiled, but not run during the build, as it needs
// a MySQL database.
// tag::main-runtime[]
// tag::main-empty[]
package org.apache.cayenne.tutorial;

// end::main-empty[]
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.runtime.CayenneRuntime;
// end::main-runtime[]
import org.apache.cayenne.tutorial.persistent.Artist;
// tag::main-runtime[]

// tag::main-empty[]
public class Main {

    public static void main(String[] args) {
        // end::main-empty[]
        CayenneRuntime cayenneRuntime = CayenneRuntime.of()
                .defaultDataNode(CayenneDataSource
                        .of("jdbc:mysql://127.0.0.1:3306/cayenne_demo")
                        .userName("root") // TODO: change to your actual username and password
                        .password("your-password").build())
                .addConfig("cayenne-project.xml")
                .build();
        ObjectContext context = cayenneRuntime.newContext();
        // end::main-runtime[]

        // tag::new-artist[]
        Artist artist = context.newObject(Artist.class);
        artist.setName("Picasso");
        context.commitChanges();
        // end::new-artist[]
        // tag::main-runtime[]
        // tag::main-empty[]
    }
}
// end::main-empty[]
// end::main-runtime[]
