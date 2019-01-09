package org.apache.cayenne.access;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CAY2509IT extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @Before
    public void before() {
        this.tArtist = new TableHelper(dbHelper, "ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH")
                .setColumnTypes(Types.BIGINT, Types.CHAR, Types.DATE);
        this.tPainting = new TableHelper(dbHelper, "PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
                .setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
    }

    @Test
    public void testSelectionProblem() throws SQLException {
        tArtist.insert(1, "A1", null);
        tPainting.insert(1, 1, "P1");
        ObjectContext context1 = runtime.newContext();
        List<Artist> artists1 = ObjectSelect.query(Artist.class)
                .select(context1);
        assertEquals("P1", artists1.get(0).getPaintingArray().get(0).getPaintingTitle());

        tPainting.update()
                .set("PAINTING_TITLE", "P2")
                .where("PAINTING_ID", 1)
                .execute();

        ObjectContext context2 = runtime.newContext();
        List<Artist> artists2 = ObjectSelect.query(Artist.class)
                .select(context2);
        assertEquals("P2", artists2.get(0).getPaintingArray().get(0).getPaintingTitle());
    }

    @Test
    public void testChangesInTwoContexts() throws SQLException {
        tArtist.insert(1, "A1", null);
        ObjectContext context1 = runtime.newContext();
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("A1"))
                .selectFirst(context1);
        assertEquals("A1", artist.getArtistName());
        artist.setArtistName("A2");
        assertEquals("A2", artist.getArtistName());

        ObjectContext context2 = runtime.newContext();
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(context2);
        assertEquals(1, artists.size());
        assertEquals("A1", artists.get(0).getArtistName());
    }

    @Test
    public void testChangesInChildContext() throws SQLException {
        tArtist.insert(1, "A1", null);
        ObjectContext parentContext1 = runtime.newContext();
        ObjectContext context1 = runtime.newContext(parentContext1);
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("A1"))
                .selectFirst(context1);
        assertEquals("A1", artist.getArtistName());
        artist.setArtistName("A2");
        context1.commitChangesToParent();

        ObjectContext parentContext2 = runtime.newContext();
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(parentContext2);
        assertEquals(1, artists.size());
        assertEquals("A1", artists.get(0).getArtistName());
    }

}
