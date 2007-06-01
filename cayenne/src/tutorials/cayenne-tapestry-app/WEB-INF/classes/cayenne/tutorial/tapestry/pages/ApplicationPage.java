package cayenne.tutorial.tapestry.pages;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.engine.IPageLoader;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.event.PageRenderListener;
import org.apache.tapestry.html.BasePage;
import org.apache.tapestry.spec.IComponentSpecification;
import org.objectstyle.cayenne.access.DataContext;

import cayenne.tutorial.tapestry.Visit;

/**
 * A superclass of all application pages. Contains utility methods
 * to access DataContext and such.
 * 
 * @author Andrei Adamchik
 */
public class ApplicationPage extends BasePage implements PageRenderListener {

    public void finishLoad(
        IRequestCycle cycle,
        IPageLoader loader,
        IComponentSpecification specification) {

        // will listen for its own render events
        addPageRenderListener(this);
        super.finishLoad(cycle, loader, specification);
    }

    /**
     * Helper method to simplify obtaining Cayenne DataContext by subclasses.
     */
    protected DataContext getVisitDataContext() {
        Visit visit = (Visit) getPage().getVisit();
        return visit.getDataContext();
    }

    /**
     * Implementation of PageRenderListener. In Tapestry 3.0 implementing PageRenderListener
     * seems to be the only way to catch the last chance to reinitialize persistent variables
     * before page rendering starts. Default implementation of this method does nothing,
     * allowing subclasses to perform proper initialization.
     */
    // Is there a standard method to override instead?
    public void pageBeginRender(PageEvent event) {
        // do nothing...
    }
}
