package org.objectstyle.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;

import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.ObjectBinding;
import org.objectstyle.cayenne.util.Util;

/**
 * A controller for stream encoding picker component.
 * 
 * @author Andrei Adamchik
 */
public class EncodingSelector extends CayenneController {

    public static final String ENCODING_PROPERTY_BINDING = "encoding";

    protected PropertyChangeListener encodingChangeListener;
    protected ObjectBinding defaultEncodingBinding;
    protected ObjectBinding otherEncodingBinding;
    protected ObjectBinding selectedEncodingBinding;

    protected EncodingSelectorView view;
    protected String systemEncoding;
    protected String encoding;
    protected boolean defaultEncoding;

    /**
     * Creates new EncodingPicker.
     */
    public EncodingSelector(CayenneController parent) {
        this(parent, new EncodingSelectorView());
    }

    public EncodingSelector(CayenneController parent, EncodingSelectorView view) {
        super(parent);
        this.view = view;

        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        // init static models...
        this.systemEncoding = detectPlatformEncoding();

        Vector allEncodings = supportedEncodings(systemEncoding);
        view.getEncodingChoices().setModel(new DefaultComboBoxModel(allEncodings));
        view.getDefaultEncodingLabel().setText("Default (" + systemEncoding + ")");
        view.getDefaultEncoding().setSelected(true);

        // create bindings...
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        this.defaultEncodingBinding = builder
                .bindToStateChange(view.getDefaultEncoding(), "defaultEncoding");

        this.otherEncodingBinding = builder.bindToStateChange(view.getOtherEncoding(),
                "otherEncoding");

        this.selectedEncodingBinding = builder.bindToComboSelection(view
                .getEncodingChoices(), "encoding");
    }

    /**
     * Returns default encoding on the current platform.
     */
    protected String detectPlatformEncoding() {
        // this info is private until 1.5, so have to hack it...
        return new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
    }

    /**
     * Returns an array of charsets that all JVMs must support cross-platform combined
     * with a default platform charset. See Javadocs for java.nio.charset.Charset for the
     * list of "standard" charsets.
     */
    protected Vector supportedEncodings(String platformEncoding) {
        String[] defaultCharsets = new String[] {
                "US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16"
        };

        Vector charsets = new Vector(Arrays.asList(defaultCharsets));
        if (!charsets.contains(platformEncoding)) {
            charsets.add(platformEncoding);
        }

        Collections.sort(charsets);
        return charsets;
    }

    public void bindingUpdated(String expression, Object newValue) {
        if (ENCODING_PROPERTY_BINDING.equals(expression)) {
            this.encoding = (newValue != null) ? newValue.toString() : null;
            this.defaultEncoding = encoding == null || encoding.equals(systemEncoding);

            selectedEncodingBinding.updateView();
            if (defaultEncoding) {
                defaultEncodingBinding.updateView();
                view.getEncodingChoices().setEnabled(false);
                view.getDefaultEncodingLabel().setEnabled(true);
            }
            else {
                otherEncodingBinding.updateView();
                view.getEncodingChoices().setEnabled(true);
                view.getDefaultEncodingLabel().setEnabled(false);
            }
        }
    }

    // ===============
    //    Properties
    // ===============

    public void setEncoding(String encoding) {
        if (!Util.nullSafeEquals(this.encoding, encoding)) {
            Object old = this.encoding;

            this.encoding = encoding;
            firePropertyChange("encoding", old, encoding);
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isDefaultEncoding() {
        return defaultEncoding;
    }

    public void setDefaultEncoding(boolean b) {
        if (b != defaultEncoding) {
            this.defaultEncoding = b;

            if (b) {
                setEncoding(systemEncoding);
                view.getEncodingChoices().setEnabled(false);
                view.getDefaultEncodingLabel().setEnabled(true);
            }
            else {
                view.getEncodingChoices().setEnabled(true);
                view.getDefaultEncodingLabel().setEnabled(false);
            }

        }
    }

    public boolean isOtherEncoding() {
        return !isDefaultEncoding();
    }

    public void setOtherEncoding(boolean b) {
        setDefaultEncoding(!b);
    }
}