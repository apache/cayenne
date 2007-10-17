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


package cayenne.tutorial.client;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.hessian.HessianConnection;

public class Main {

	public static void main(String[] args) {

		ClientConnection connection = new HessianConnection(
				"http://localhost:8080/cayenne-rop-server-tutorial/cayenne-service", "cayenne-user",
				"secret", null);

		DataChannel channel = new ClientChannel(connection);

		ObjectContext context = new CayenneContext(channel);

		// cleans up all data, so that we start with empty database on each
		// tutorial run
		mappingQueriesChapter(context);

		// persists an artist, a gallery and a few paintings
		dataObjectsChapter(context);

		// selects previously saved data
		selectQueryChapter(context);

		// deletes objects
		deleteChapter(context);
	}

	static void dataObjectsChapter(ObjectContext context) {
		Artist picasso = (Artist) context.newObject(Artist.class);
		picasso.setName("Pablo Picasso");
		picasso.setDateOfBirthString("18811025");

		Gallery metropolitan = (Gallery) context.newObject(Gallery.class);
		metropolitan.setName("Metropolitan Museum of Art");

		Painting girl = (Painting) context.newObject(Painting.class);
		girl.setName("Girl Reading at a Table");

		Painting stein = (Painting) context.newObject(Painting.class);
		stein.setName("Gertrude Stein");

		picasso.addToPaintings(girl);
		picasso.addToPaintings(stein);

		girl.setGallery(metropolitan);
		stein.setGallery(metropolitan);

		context.commitChanges();
	}

	static void mappingQueriesChapter(ObjectContext context) {

		QueryChain chain = new QueryChain();
		chain.addQuery(new NamedQuery("DeleteAll", Collections.singletonMap(
				"table", "PAINTING")));
		chain.addQuery(new NamedQuery("DeleteAll", Collections.singletonMap(
				"table", "ARTIST")));
		chain.addQuery(new NamedQuery("DeleteAll", Collections.singletonMap(
				"table", "GALLERY")));

		context.performGenericQuery(chain);

	}

	static void selectQueryChapter(ObjectContext context) {

		// select all paintings
		SelectQuery select1 = new SelectQuery(Painting.class);
		List paintings1 = context.performQuery(select1);

		// select paintings that start with "Gi*"
		Expression qualifier2 = ExpressionFactory.likeIgnoreCaseExp(
				Painting.NAME_PROPERTY, "gi%");
		SelectQuery select2 = new SelectQuery(Painting.class, qualifier2);
		List paintings2 = context.performQuery(select2);

		// select all paintings done by artists who were born more than a 100
		// years ago
		Calendar c = new GregorianCalendar();
		c.set(c.get(Calendar.YEAR) - 100, 0, 1, 0, 0, 0);

		Expression qualifier3 = Expression
				.fromString("artist.dateOfBirth < $date");
		qualifier3 = qualifier3.expWithParameters(Collections.singletonMap(
				"date", c.getTime()));
		SelectQuery select3 = new SelectQuery(Painting.class, qualifier3);
		List paintings3 = context.performQuery(select3);
	}

	static void deleteChapter(ObjectContext context) {
		Expression qualifier = ExpressionFactory.matchExp(Artist.NAME_PROPERTY,
				"Pablo Picasso");
		SelectQuery select = new SelectQuery(Artist.class, qualifier);

		Artist picasso = (Artist) DataObjectUtils.objectForQuery(context,
				select);
		if (picasso != null) {

			context.deleteObject(picasso);
			context.commitChanges();
		}
	}
}
