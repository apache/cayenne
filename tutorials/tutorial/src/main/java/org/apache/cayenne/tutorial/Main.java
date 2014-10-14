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

import static org.apache.cayenne.exp.ExpressionFactory.exp;
import static org.apache.cayenne.exp.ExpressionFactory.or;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.tutorial.persistent.Artist;
import org.apache.cayenne.tutorial.persistent.Gallery;
import org.apache.cayenne.tutorial.persistent.Painting;

public class Main {

	public static void main(String[] args) {

		// starting Cayenne
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-project.xml");

		// getting a hold of ObjectContext
		ObjectContext context = cayenneRuntime.newContext();

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
		List<Painting> paintings1 = SelectQuery.query(Painting.class).select(context);

		Expression qualifier2 = Painting.NAME.likeInsensitive("gi%");
		List<Painting> paintings2 = SelectQuery.query(Painting.class, qualifier2).select(context);

		Calendar c = new GregorianCalendar();
		c.set(c.get(Calendar.YEAR) - 100, 0, 1, 0, 0, 0);

		// static import for org.apache.cayenne.exp.ExpressionFactory allows us
		// to use 'exp'
		Expression qualifier3 = exp("artist.dateOfBirth < $date", "date", c.getTime());
		List<Painting> paintings3 = SelectQuery.query(Painting.class, qualifier3).select(context);

		// static import for org.apache.cayenne.exp.ExpressionFactory allows us
		// to use 'or'
		List<Painting> paintings4 = SelectQuery.query(Painting.class, or(qualifier2, qualifier3)).select(context);
	}

	static void deleteTutorial(ObjectContext context) {
		// Delete object examples
		Expression qualifier = Artist.NAME.eq("Pablo Picasso");
		Artist picasso = SelectQuery.query(Artist.class, qualifier).selectOne(context);

		if (picasso != null) {
			context.deleteObjects(picasso);
			context.commitChanges();
		}
	}
}
