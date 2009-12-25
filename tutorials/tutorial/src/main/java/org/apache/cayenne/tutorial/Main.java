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
package org.apache.cayenne.tutorial;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.tutorial.persistent.Artist;
import org.apache.cayenne.tutorial.persistent.Gallery;
import org.apache.cayenne.tutorial.persistent.Painting;

public class Main {

	public static void main(String[] args) {

		// getting a hold of ObjectContext
		ObjectContext context = DataContext.createDataContext();

		newObjectsTutorial(context);
		selectTutorial(context);
		deleteTutorial(context);
	}

	static void newObjectsTutorial(ObjectContext context) {

		// creating new Artist
		Artist picasso = context.newObject(Artist.class);
		picasso.setName("Pablo Picasso");
		picasso.setDateOfBirthString("18811025");

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
		// SelectQuery examples
		SelectQuery select1 = new SelectQuery(Painting.class);
		List<Painting> paintings1 = context.performQuery(select1);

		Expression qualifier2 = ExpressionFactory.likeIgnoreCaseExp(
				Painting.NAME_PROPERTY, "gi%");
		SelectQuery select2 = new SelectQuery(Painting.class, qualifier2);
		List<Painting> paintings2 = context.performQuery(select2);

		Calendar c = new GregorianCalendar();
		c.set(c.get(Calendar.YEAR) - 100, 0, 1, 0, 0, 0);

		Expression qualifier3 = Expression
				.fromString("artist.dateOfBirth < $date");
		qualifier3 = qualifier3.expWithParameters(Collections.singletonMap(
				"date", c.getTime()));
		SelectQuery select3 = new SelectQuery(Painting.class, qualifier3);
		List<Painting> paintings3 = context.performQuery(select3);
	}

	static void deleteTutorial(ObjectContext context) {
		// Delete object examples
		Expression qualifier = ExpressionFactory.matchExp(Artist.NAME_PROPERTY,
				"Pablo Picasso");
		SelectQuery selectToDelete = new SelectQuery(Artist.class, qualifier);
		Artist picasso = (Artist) DataObjectUtils.objectForQuery(context,
				selectToDelete);

		if (picasso != null) {
			context.deleteObject(picasso);
			context.commitChanges();
		}
	}
}
