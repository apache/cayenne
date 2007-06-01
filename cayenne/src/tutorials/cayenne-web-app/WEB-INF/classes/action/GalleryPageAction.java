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
import org.objectstyle.cayenne.query.SelectQuery;

import webtest.Gallery;

public class GalleryPageAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        DataContext ctxt =
            BasicServletConfiguration.getDefaultContext(request.getSession());

        SelectQuery query = new SelectQuery(Gallery.class);

        List galleries = ctxt.performQuery(query);
        request.setAttribute("galleries", galleries);

        return mapping.findForward("success");
    }

}
