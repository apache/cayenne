package formbean;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public final class PaintingForm extends ActionForm  {
    
    private String paintingTitle = null; 
    private BigDecimal estimatedPrice = null;
    private String artistName = null;
    
    public void setEstimatedPrice(BigDecimal value) {
        estimatedPrice = value;
    }
    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }
        
    public void setPaintingTitle(String value) {
        paintingTitle = value;
    }
    public String getPaintingTitle() {
        return paintingTitle;
    }
    
    public void setArtistName(String value) {
        artistName = value;
    }
    public String getArtistName() {
        return artistName;
    }
    
       /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {

        this.estimatedPrice = null;
        this.paintingTitle = null;
        this.artistName = null;

    }


    /**
     * Validate the properties that have been set from this HTTP request,
     * and return an <code>ActionErrors</code> object that encapsulates any
     * validation errors that have been found.  If no errors are found, return
     * <code>null</code> or an <code>ActionErrors</code> object with no
     * recorded error messages.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {

        ActionErrors errors = new ActionErrors();
        if ((paintingTitle == null) || (paintingTitle.length() < 1))
            errors.add("paintingTitle", new ActionError("error.paintingtitle.required"));

        return errors;
    }
}