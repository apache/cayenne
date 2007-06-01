package action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.BasicServletConfiguration;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

import webtest.Artist;

public class ArtistPageAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        DataContext ctxt =
            BasicServletConfiguration.getDefaultContext(request.getSession());

        SelectQuery query = new SelectQuery(Artist.class);
        Ordering ordering = new Ordering("artistName", Ordering.ASC);
        query.addOrdering(ordering);

        List artists = ctxt.performQuery(query);
        request.setAttribute("artists", artists);

        return mapping.findForward("success");
    }
}