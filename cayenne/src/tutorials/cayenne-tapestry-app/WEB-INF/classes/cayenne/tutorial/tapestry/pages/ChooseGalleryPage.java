package cayenne.tutorial.tapestry.pages;

import java.util.List;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.event.PageEvent;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

import cayenne.tutorial.tapestry.domain.Gallery;
import cayenne.tutorial.tapestry.domain.Painting;

/**
 * @author Eric Schneider
 */
public abstract class ChooseGalleryPage extends ApplicationPage {

    // properties are defined as abstract setters and getters
    // and are declared in ChooseGalleryPage.page file
    public abstract void setGallery(Gallery value);
    public abstract Gallery getGallery();

    public abstract void setPainting(Painting value);
    public abstract Painting getPainting();

    public abstract void setGalleryList(List value);
    public abstract List getGalleryList();

    public void savePaintingToGalleryAction(IRequestCycle cycle) {

        getGallery().addToPaintingArray(getPainting());

        // commit to the database
        getVisitDataContext().commitChanges();

        BrowseArtistsPage nextPage =
            (BrowseArtistsPage) cycle.getPage("BrowseArtistsPage");
        cycle.activate(nextPage);
    }

    public void pageBeginRender(PageEvent event) {
        SelectQuery query = new SelectQuery(Gallery.class);
        Ordering ordering = new Ordering(Gallery.GALLERY_NAME_PROPERTY, Ordering.ASC);
        query.addOrdering(ordering);

        setGalleryList(getVisitDataContext().performQuery(query));
    }
}
