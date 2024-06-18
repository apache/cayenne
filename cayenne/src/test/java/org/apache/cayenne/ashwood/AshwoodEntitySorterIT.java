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
package org.apache.cayenne.ashwood;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class AshwoodEntitySorterIT extends RuntimeCase {

	@Inject
	protected ObjectContext context;

	private EntityResolver resolver;
	private AshwoodEntitySorter sorter;

	private DbEntity artist;
	private DbEntity artistExhibit;
	private DbEntity exhibit;
	private DbEntity gallery;
	private DbEntity painting;
	private DbEntity paintingInfo;

	@Before
	public void before() {

		this.resolver = context.getEntityResolver();
		this.sorter = new AshwoodEntitySorter();
		sorter.setEntityResolver(resolver);

		this.artist = resolver.getDbEntity("ARTIST");
		this.artistExhibit = resolver.getDbEntity("ARTIST_EXHIBIT");
		this.exhibit = resolver.getDbEntity("EXHIBIT");
		this.gallery = resolver.getDbEntity("GALLERY");
		this.painting = resolver.getDbEntity("PAINTING");
		this.paintingInfo = resolver.getDbEntity("PAINTING_INFO");
	}

	@Test
	public void testSortDbEntities() {

		List<DbEntity> entities = Arrays.asList(artist, artistExhibit, exhibit, gallery, painting, paintingInfo);
		Collections.shuffle(entities);

		sorter.sortDbEntities(entities, false);

		assertTrue(entities.indexOf(artist) < entities.indexOf(artistExhibit));
		assertTrue(entities.indexOf(artist) < entities.indexOf(painting));
		assertTrue(entities.indexOf(gallery) < entities.indexOf(exhibit));
		assertTrue(entities.indexOf(exhibit) < entities.indexOf(artistExhibit));
		assertTrue(entities.indexOf(painting) < entities.indexOf(paintingInfo));
	}

}
