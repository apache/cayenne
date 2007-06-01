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
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

import webtest.Artist;
import webtest.Painting;
import formbean.PaintingForm;

public class SavePaintingAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        PaintingForm paintingForm = (PaintingForm) form;

        DataContext ctxt =
            BasicServletConfiguration.getDefaultContext(request.getSession());

        String anArtistName = paintingForm.getArtistName();

        Expression qual = ExpressionFactory.matchExp("artistName", anArtistName);

        SelectQuery query = new SelectQuery(Artist.class, qual);

        List artists = ctxt.performQuery(query);
        System.err.println("artists: " + artists);
        Artist artist = (Artist) artists.get(0);

        Painting aPainting = (Painting) ctxt.createAndRegisterNewObject("Painting");
        aPainting.setPaintingTitle(paintingForm.getPaintingTitle());
        aPainting.setEstimatedPrice(paintingForm.getEstimatedPrice());

        artist.addToPaintingArray(aPainting);

        // commit to the database
        ctxt.commitChanges();

        return (mapping.findForward("success"));
    }
}
