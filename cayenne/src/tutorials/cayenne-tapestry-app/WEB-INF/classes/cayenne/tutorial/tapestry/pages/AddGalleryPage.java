package cayenne.tutorial.tapestry.pages;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.event.PageEvent;
import org.objectstyle.cayenne.access.DataContext;

import cayenne.tutorial.tapestry.domain.Gallery;

/**
 * A page to add new Art Galleries to the system.
 * 
 * @author Eric Schneider
 */
public abstract class AddGalleryPage extends EditorPage {

    // properties are defined as abstract setters and getters
    // and are declared in AddGalleryPage.page file
    public abstract void setGallery(Gallery value);
    public abstract Gallery getGallery();

    public void saveGalleryAction(IRequestCycle cycle) {
        Gallery gallery = getGallery();

        if (!assertNotNull(gallery.getGalleryName())) {
            appendHtmlToErrorMessage("You must provide a gallery name.");
            return;
        }

        DataContext ctxt = getVisitDataContext();
        ctxt.registerNewObject(gallery);

        // commit to the database
        ctxt.commitChanges();

        BrowseGalleriesPage nextPage =
            (BrowseGalleriesPage) cycle.getPage("BrowseGalleriesPage");

        // update the next page if it has cached galleries
        // to avoid unneeded refreshing
        if (nextPage.getGalleryList() != null) {
            nextPage.getGalleryList().add(gallery);
        }

        cycle.activate(nextPage);
    }

    public void pageBeginRender(PageEvent event) {

        // create new Gallery when page is initialized. 
        // Do not intsert it into DataContext just yet.
        // Instead if we register it here, and the user abandons 
        // the page, we will have to find a way to rollback the context,
        // to avoid grabage carried over to other pages
        setGallery(new Gallery());
    }

}
