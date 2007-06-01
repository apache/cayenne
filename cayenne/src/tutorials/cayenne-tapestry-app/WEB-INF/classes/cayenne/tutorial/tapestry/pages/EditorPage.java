package cayenne.tutorial.tapestry.pages;


/**
 * @author Eric Schneider
 *
 * A wedge class that handles and displays all user input
 * validation errors.
 */
public class EditorPage extends ApplicationPage {

    /** A human presentable error message.  Should never be null. */
    protected String errorMessage;

    public void initialize() {
        super.initialize();

        setErrorMessage("");
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Sets the error message.  If the argument is null, 
     * it will set the error message to the empty string.
     *
     * @param The new error message.  Should not be null.
     */
    public void setErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            this.errorMessage = "";
        }
        else {
            this.errorMessage = errorMessage;
        }
    }

    /** Appends the argument to the error message.  Ensures that 
      * the argument does not already exist in the error message.
      *
      * @param The string to be appended to the error message.
      */
    public void appendToErrorMessage(String appendix) {
        if ((appendix != null) && (errorMessageContainsString(appendix) == false)) {
            setErrorMessage(getErrorMessage().concat(appendix));
        }
    }

    /** Appends the argument to the error message.  Assumes it is HTML
      * and prepends an HTML break tag to the appendix.
      *
      * @param The string to be appended to the error message, assumed to be HTML.
      */
    public void appendHtmlToErrorMessage(String htmlAppendix) {
        if ((htmlAppendix != null)
            && (errorMessageContainsString(htmlAppendix) == false)) {
            StringBuffer newAppendix;

            newAppendix = new StringBuffer();
            if (getErrorMessage().length() != 0) {
                newAppendix.append("<BR>");
            }

            newAppendix.append(htmlAppendix);

            appendToErrorMessage(newAppendix.toString());
        }
    }

    /** Checks to see if the argument is already in the error message.
      * If the argument is null, returns true.  Trims the argument for 
      * matching purposes.
      *
      * @param The String to check the error message to see if it contains it.
      * @return true if the string is already in the error message, false otherwise.
      */
    public boolean errorMessageContainsString(String text) {
        if (text == null) {
            return true;
        }

        if (getErrorMessage().indexOf(text.trim()) == -1) {
            return false;
        }
        else {
            return true;
        }
    }

    /** Returns true if the error message is non-empty (i.e., length > 0).
      *
      * @return true if the error message is not empty.
      */
    public boolean getHasErrorMessage() {
        if (getErrorMessage().length() > 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean assertNotNull(Object anObject) {
        if (anObject == null) {
            return false;
        }

        if ((anObject instanceof String) && ("".equals(anObject))) {
            return false;
        }

        return true;
    }
}
