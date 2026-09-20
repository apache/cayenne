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
package org.apache.cayenne.docs;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Gallery;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A superclass of the tests that hold the code examples included in the documentation. The examples are the parts of
 * the test code between the "tag::xyz[]" and "end::xyz[]" comments. As they are compiled and run with the rest of the
 * tests, they can not get out of sync with the Cayenne API.
 */
public abstract class BaseTest {

    protected static CayenneRuntime runtime;

    protected ObjectContext context;

    @BeforeAll
    public static void startRuntime() {
        DataSource dataSource = CayenneDataSource.of("jdbc:hsqldb:mem:docs").pool(1, 3).build();

        DataNodeDescriptor dataNode = DataNodeDescriptor.of("docs")
                .dataSource(dataSource)
                .createSchemaIfNeeded()
                .build();

        // optional modules (crypto, jcache, etc.) are on the classpath for their own examples to compile, but should
        // not be a part of this runtime
        runtime = CayenneRuntime.of()
                .disableModulesAutoLoading()
                .addConfig("cayenne-project.xml")
                .defaultDataNode(dataNode)
                .build();
    }

    @AfterAll
    public static void stopRuntime() {
        runtime.shutdown();
        runtime = null;
    }

    @BeforeEach
    public void resetData() {
        context = runtime.newContext();

        for (String table : List.of("PAINTING", "ARTIST", "GALLERY", "COMPOUND_PK", "GENERIC_ENTITY")) {
            SQLExec.query("DELETE FROM " + table).update(context);
        }
    }

    protected Artist createArtist(String name, LocalDate dateOfBirth) {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName(name);
        artist.setDateOfBirth(dateOfBirth);
        return artist;
    }

    protected Gallery createGallery(String name) {
        Gallery gallery = context.newObject(Gallery.class);
        gallery.setGalleryName(name);
        return gallery;
    }

    protected Painting createPainting(String title, int price, Artist artist, Gallery gallery) {
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle(title);
        painting.setEstimatedPrice(BigDecimal.valueOf(price));
        painting.setToArtist(artist);
        painting.setToGallery(gallery);
        return painting;
    }

    /**
     * Creates and commits 3 artists, 2 galleries and 4 paintings.
     */
    protected void createArtistsDataSet() {
        Artist dali = createArtist("Dali", LocalDate.of(1904, 5, 11));
        Artist picasso = createArtist("Pablo Picasso", LocalDate.of(1881, 10, 25));
        createArtist("Aivazovsky", LocalDate.of(1817, 7, 29));

        Gallery moma = createGallery("MoMA");
        Gallery prado = createGallery("Prado");

        createPainting("P1", 100, dali, moma);
        createPainting("P2", 2000, dali, prado);
        createPainting("Guernica", 300000, picasso, prado);
        createPainting("Gertrude Stein", 5000, picasso, null);

        context.commitChanges();
    }

    /**
     * Returns a non-Java example (such as a query String) from a test resource file. The example is the text between
     * the "tag::xyz[]" and "end::xyz[]" lines. This allows the tests to check the same text that goes in the docs.
     */
    protected static String example(String resource, String tag) {

        List<String> lines;
        try (InputStream in = BaseTest.class.getResourceAsStream(resource)) {
            lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        int from = indexOfLineWith(lines, "tag::" + tag + "[]");
        int to = indexOfLineWith(lines, "end::" + tag + "[]");
        return String.join("\n", lines.subList(from + 1, to));
    }

    private static int indexOfLineWith(List<String> lines, String marker) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(marker)) {
                return i;
            }
        }

        throw new IllegalArgumentException("No such marker: " + marker);
    }
}
