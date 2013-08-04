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

package org.apache.cayenne;

import java.util.Date;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class CayenneDataObjectValidationTest extends ServerCase {

    @Inject
    private ObjectContext context;

    public void testValidateForSaveMandatoryToOneMissing() throws Exception {

        Exhibit exhibit = context.newObject(Exhibit.class);
        exhibit.setOpeningDate(new Date());
        exhibit.setClosingDate(new Date());

        ValidationResult result = new ValidationResult();
        exhibit.validateForSave(result);

        assertTrue("Validation of 'toGallery' should've failed.", result.hasFailures());
        assertTrue(result.hasFailures(exhibit));

        List<ValidationFailure> failures = result.getFailures();
        assertEquals(1, failures.size());

        BeanValidationFailure failure = (BeanValidationFailure) failures.get(0);
        assertEquals(Exhibit.TO_GALLERY_PROPERTY, failure.getProperty());

        // fix the problem and see if it goes away
        Gallery gallery = context.newObject(Gallery.class);
        exhibit.setToGallery(gallery);
        result = new ValidationResult();
        exhibit.validateForSave(result);
        assertFalse("No failures expected: " + result, result.hasFailures());
    }

    public void testValidateForSaveMandatoryAttributeMissing() throws Exception {

        Artist artist = context.newObject(Artist.class);

        ValidationResult result = new ValidationResult();
        artist.validateForSave(result);

        assertTrue("Validation of 'artistName' should've failed.", result.hasFailures());
        assertTrue(result.hasFailures(artist));

        List<ValidationFailure> failures = result.getFailures();
        assertEquals(1, failures.size());

        BeanValidationFailure failure = (BeanValidationFailure) failures.get(0);
        assertEquals(Artist.ARTIST_NAME_PROPERTY, failure.getProperty());

        // fix the problem and see if it goes away
        artist.setArtistName("aa");
        result = new ValidationResult();
        artist.validateForSave(result);
        assertFalse(result.hasFailures());
    }

    public void testValidateForSaveAttributeTooLong() throws Exception {

        Artist artist = context.newObject(Artist.class);

        DbEntity entity = context.getEntityResolver().getObjEntity(artist).getDbEntity();
        int len = entity.getAttribute("ARTIST_NAME").getMaxLength();
        StringBuffer buf = new StringBuffer(len);
        for (int i = 0; i < len + 1; i++) {
            buf.append("c");
        }
        artist.setArtistName(buf.toString());

        ValidationResult result = new ValidationResult();
        artist.validateForSave(result);

        assertTrue(result.hasFailures());
        assertTrue(result.hasFailures(artist));

        List<ValidationFailure> failures = result.getFailures();
        assertEquals(1, failures.size());

        BeanValidationFailure failure = (BeanValidationFailure) failures.get(0);
        assertEquals(Artist.ARTIST_NAME_PROPERTY, failure.getProperty());

        // fix the problem and see if it goes away
        artist.setArtistName("aa");
        result = new ValidationResult();
        artist.validateForSave(result);
        assertFalse(result.hasFailures());
    }
}
