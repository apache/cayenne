package org.apache.cayenne.query;

import static java.util.Collections.singletonMap;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class IdSelectTest extends ServerCase {

	@Inject
	protected DBHelper dbHelper;

	protected TableHelper tArtist;

	@Inject
	private ObjectContext context;

	@Override
	protected void setUpAfterInjection() throws Exception {
		dbHelper.deleteAll("PAINTING_INFO");
		dbHelper.deleteAll("PAINTING");
		dbHelper.deleteAll("ARTIST_EXHIBIT");
		dbHelper.deleteAll("ARTIST_GROUP");
		dbHelper.deleteAll("ARTIST");
		dbHelper.deleteAll("COMPOUND_FK_TEST");
		dbHelper.deleteAll("COMPOUND_PK_TEST");
		dbHelper.deleteAll("CHAR_PK_TEST");

		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
	}

	private void createTwoArtists() throws Exception {
		tArtist.insert(2, "artist2");
		tArtist.insert(3, "artist3");
	}

	public void testIntPk() throws Exception {
		createTwoArtists();

		Artist a3 = IdSelect.query(Artist.class, 3).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.getArtistName());

		Artist a2 = IdSelect.query(Artist.class, 2).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.getArtistName());
	}

	public void testMapPk() throws Exception {
		createTwoArtists();

		Artist a3 = IdSelect.query(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 3)).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.getArtistName());

		Artist a2 = IdSelect.query(Artist.class, singletonMap(Artist.ARTIST_ID_PK_COLUMN, 2)).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.getArtistName());
	}

	public void testObjectIdPk() throws Exception {
		createTwoArtists();

		ObjectId oid3 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
		Artist a3 = IdSelect.query(Artist.class, oid3).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.getArtistName());

		ObjectId oid2 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
		Artist a2 = IdSelect.query(Artist.class, oid2).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.getArtistName());
	}
	
	public void testDataRowIntPk() throws Exception {
		createTwoArtists();

		DataRow a3 = IdSelect.dataRowQuery(Artist.class, 3).selectOne(context);
		assertNotNull(a3);
		assertEquals("artist3", a3.get("ARTIST_NAME"));

		DataRow a2 = IdSelect.dataRowQuery(Artist.class, 2).selectOne(context);
		assertNotNull(a2);
		assertEquals("artist2", a2.get("ARTIST_NAME"));
	}
}
