package test;

import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

public class Main {

    private DataContext ctxt;

    /** 
     * Runs tutorial.
     * Usage:
     *     java test.Main galleryPattern
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage:");
            System.err.println("    java test.Main galleryPattern");
            System.exit(1);
        }

        Main tutorialObj = new Main();
        tutorialObj.runTutorial(args[0]);
    }

    public Main() {
        this.ctxt = createContext();
    }

    public void runTutorial(String galleryPattern) {
        Gallery gallery = findGallery(galleryPattern);
        if (gallery != null) {
            addArtist(gallery);
        }
    }

    /** Creates and returns DataContext object. */
    private DataContext createContext() {
        Configuration.bootstrapSharedConfiguration(this.getClass());
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /** 
     * Searches for matching galleries in the database. 
     * If one and only one matching gallery is found, it is returned, 
     * otherwise null is returned.
     */
    private Gallery findGallery(String galleryPattern) {
        String likePattern = "%" + galleryPattern + "%";
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("galleryName", likePattern);

        SelectQuery query = new SelectQuery(Gallery.class, qual);
        // using log level of WARN to make sure that query 
        // execution is logged to STDOUT
        query.setLoggingLevel(Level.WARN);

        List galleries = ctxt.performQuery(query);
        if (galleries.size() == 1) {
            Gallery gallery = (Gallery) galleries.get(0);
            System.out.println("\nFound gallery '" + gallery.getGalleryName() + "'.\n");
            return gallery;
        }
        else if (galleries.size() == 0) {
            System.out.println("No matching galleries found.");
            return null;
        }
        else {
            System.out.println("Found more than one matching gallery. Be more specific.");
            return null;
        }
    }

    /** Adds new artist and his paintings to the gallery. */
    private void addArtist(Gallery gallery) {
        // create new Artist object
        Artist dali = (Artist) ctxt.createAndRegisterNewObject("Artist");
        dali.setArtistName("Salvador Dali");

        // create new Painting object
        Painting painting = (Painting) ctxt.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle("Sleep");

        // establish relationship between artist and painting
        dali.addToPaintingArray(painting);
        
        // establish relationship between painting and gallery
        gallery.addToPaintingArray(painting);

        // commit to the database
        // using log level of WARN to show the query execution
        ctxt.commitChanges(Level.WARN);
    }
}