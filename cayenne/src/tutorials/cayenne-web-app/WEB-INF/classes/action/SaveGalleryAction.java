package action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.BasicServletConfiguration;

import webtest.Gallery;
import formbean.GalleryForm;

public class SaveGalleryAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        DataContext ctxt =
            BasicServletConfiguration.getDefaultContext(request.getSession());

        GalleryForm galleryForm = (GalleryForm) form;

        Gallery aGallery = (Gallery) ctxt.createAndRegisterNewObject("Gallery");
        aGallery.setGalleryName(galleryForm.getGalleryName());

        // commit to the database
        ctxt.commitChanges();

        return (mapping.findForward("success"));
    }

}