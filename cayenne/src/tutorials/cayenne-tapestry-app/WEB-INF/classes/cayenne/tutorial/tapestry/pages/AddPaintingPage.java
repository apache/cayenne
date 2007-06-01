package cayenne.tutorial.tapestry.pages;

import java.math.BigDecimal;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.event.PageEvent;
import org.objectstyle.cayenne.access.DataContext;

import cayenne.tutorial.tapestry.domain.Artist;
import cayenne.tutorial.tapestry.domain.Painting;

/**
 * Page to create a new Painting record, adding it to selected artist.
 * 
 * @author Eric Schneider
 */
public abstract class AddPaintingPage extends EditorPage {

    // properties are defined as abstract setters and getters
    // and are declared in AddPaintingPage.page file
    public abstract void setArtist(Artist value);
    public abstract Artist getArtist();

    public abstract void setPainting(Painting value);
    public abstract Painting getPainting();

    public void savePaintingAction(IRequestCycle cycle) {
        Painting painting = getPainting();
        Artist artist = getArtist();

        if (!assertNotNull(painting.getPaintingTitle())) {
            appendHtmlToErrorMessage("You must provide a painting title.");
            return;
        }

        DataContext ctxt = getVisitDataContext();

        // painting was created earlier, but before we can
        // start working with its relationships, and ultimately
        // save it to DB, it must be regsitered with DataContext
        ctxt.registerNewObject(painting);

        // establish relationship with artist via a simple method call
        // an equivalent of the line below would be "
        artist.addToPaintingArray(painting);

        // commit to the database
        ctxt.commitChanges();

        BrowseArtistsPage nextPage =
            (BrowseArtistsPage) cycle.getPage("BrowseArtistsPage");

        cycle.activate(nextPage);
    }

    public void pageBeginRender(PageEvent event) {
        Painting painting = new Painting();
        painting.setEstimatedPrice(new BigDecimal(0));
        setPainting(painting);
    }
}
