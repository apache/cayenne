package cayenne.tutorial.tapestry.pages;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.event.PageEvent;
import org.objectstyle.cayenne.access.DataContext;

import cayenne.tutorial.tapestry.domain.Artist;

/**
 * A page to create a new Artist and save him/her in the database.
 * 
 * @author Eric Schneider
 */
public abstract class AddArtistPage extends EditorPage {

    // properties are defined as abstract setters and getters
    // and are declared in AddArtistPage.page file
    public abstract void setArtist(Artist value);
    public abstract Artist getArtist();

    public void saveArtistAction(IRequestCycle cycle) {
        DataContext context = getVisitDataContext();

        // obtain local reference to artist abstract property
        Artist artist = getArtist();

        if (!assertNotNull(artist.getArtistName())) {
            appendHtmlToErrorMessage("You must provide a name.");
        }

        if (!assertNotNull(artist.getDateOfBirth())) {
            appendHtmlToErrorMessage("You must provide a DOB.");
        }

        if (getHasErrorMessage()) {
            return;
        }

        // since new artist wasn't registered, register it before save
        context.registerNewObject(artist);

        // commit to the database
        context.commitChanges();

        BrowseArtistsPage nextPage =
            (BrowseArtistsPage) cycle.getPage("BrowseArtistsPage");

        // update the next page if it has cached artists
        // to avoid unneeded refetch
        if (nextPage.getArtistList() != null) {
            nextPage.getArtistList().add(artist);
        }

        cycle.activate(nextPage);
    }

    public void pageBeginRender(PageEvent event) {
        // create new artist when page is initialized. 
        // Do not intsert it into DataContext just yet.
        // Instead if we register it here, and the user abandons 
        // the page, we will have to find a way to rollback the context,
        // to avoid grabage carried over to other pages
        setArtist(new Artist());
    }
}
