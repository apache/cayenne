package formbean;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public final class ArtistForm extends ActionForm {

    private String artistName = null;
    private String dateOfBirth = null;

    public void setArtistName(String value) {
        artistName = value;
    }
    public String getArtistName() {
        return artistName;
    }

    public void setDateOfBirth(String value) {
        dateOfBirth = value;
    }
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    /**
      * Reset all properties to their default values.
      *
      * @param mapping The mapping used to select this instance
      * @param request The servlet request we are processing
      */
    public void reset(ActionMapping mapping, HttpServletRequest request) {

        this.artistName = null;
        this.dateOfBirth = null;

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
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

        System.err.println("****Inside ArtistForm.validate()");
        ActionErrors errors = new ActionErrors();
        if ((artistName == null) || (artistName.length() < 1))
            errors.add("artistName", new ActionError("error.artistname.required"));

        if (dateOfBirth == null)
            errors.add("dateOfBirth", new ActionError("No date of birth provided"));

        return errors;

    }

}
