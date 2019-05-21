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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.util.Map;
import java.util.Set;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExportedKeyLoaderIT extends BaseLoaderIT {

    @Inject
    DbAdapter adapter;

    @Test
    public void testExportedKeyLoad() throws Exception {
        boolean supportsFK = accessStackAdapter.supportsFKConstraints();
        if(!supportsFK) {
            return;
        }

        createEntity(nameForDb("ARTIST"));
        createEntity(nameForDb("GALLERY"));
        createEntity(nameForDb("PAINTING"));

        String catalog = connection.getCatalog();
        String schema = connection.getSchema();

        DbEntity artist = getDbEntity("ARTIST");
        if(adapter.supportsCatalogsOnReverseEngineering()) {
            artist.setCatalog(catalog);
        }
        artist.setSchema(schema);
        DbAttribute artistId = new DbAttribute("ARTIST_ID");
        artist.addAttribute(artistId);

        DbEntity gallery = getDbEntity("GALLERY");
        if(adapter.supportsCatalogsOnReverseEngineering()) {
            gallery.setCatalog(catalog);
        }
        gallery.setSchema(schema);
        DbAttribute galleryId = new DbAttribute("GALLERY_ID");
        gallery.addAttribute(galleryId);

        DbEntity painting = getDbEntity("PAINTING");
        if(adapter.supportsCatalogsOnReverseEngineering()) {
            painting.setCatalog(catalog);
        }
        painting.setSchema(schema);
        DbAttribute paintingId = new DbAttribute("PAINTING_ID");
        DbAttribute paintingArtistId = new DbAttribute("ARTIST_ID");
        DbAttribute paintingGalleryId = new DbAttribute("GALLERY_ID");
        painting.addAttribute(paintingId);
        painting.addAttribute(paintingArtistId);
        painting.addAttribute(paintingGalleryId);

        ExportedKeyLoader loader = new ExportedKeyLoader(EMPTY_CONFIG, new DefaultDbLoaderDelegate());
        loader.load(connection.getMetaData(), store);

        assertEquals(2, store.getExportedKeysEntrySet().size());

        ExportedKey artistIdFk = findArtistExportedKey();
        assertNotNull(artistIdFk);

        assertEquals("ARTIST", artistIdFk.getPk().getTable().toUpperCase());
        assertEquals("ARTIST_ID", artistIdFk.getPk().getColumn().toUpperCase());

        assertEquals("PAINTING", artistIdFk.getFk().getTable().toUpperCase());
        assertEquals("ARTIST_ID", artistIdFk.getFk().getColumn().toUpperCase());
    }

    private ExportedKey findArtistExportedKey() {
        for(Map.Entry<String, Set<ExportedKey>> entry : store.getExportedKeysEntrySet()) {
            if(entry.getKey().toUpperCase().endsWith(".ARTIST_ID")) {
                return entry.getValue().iterator().next();
            }
        }

        return null;
    }
}
