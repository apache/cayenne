package action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.BasicServletConfiguration;

import webtest.Artist;
import formbean.ArtistForm;

public class SaveArtistAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        ArtistForm artistForm = (ArtistForm) form;

        // Validate the user form information
        ActionErrors errors = new ActionErrors();
        errors = artistForm.validate(mapping, request);

        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            saveToken(request);
            return (new ActionForward(mapping.getInput()));
        }

        DataContext ctxt =
            BasicServletConfiguration.getDefaultContext(request.getSession());

        Artist anArtist = (Artist) ctxt.createAndRegisterNewObject("Artist");
        anArtist.setArtistName(artistForm.getArtistName());
        anArtist.setDateOfBirth(new java.sql.Date(System.currentTimeMillis()));

        // commit to the database
        ctxt.commitChanges();

        return (mapping.findForward("success"));
    }
}
