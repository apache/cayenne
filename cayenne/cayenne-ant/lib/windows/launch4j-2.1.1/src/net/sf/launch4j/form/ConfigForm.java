package net.sf.launch4j.form;

import com.jeta.forms.components.separator.TitledSeparator;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public abstract class ConfigForm extends JPanel
{
   protected final JTabbedPane _tab = new JTabbedPane();
   protected final JButton _outfileButton = new JButton();
   protected final JLabel _outfileLabel = new JLabel();
   protected final JLabel _errorTitleLabel = new JLabel();
   protected final JTextField _outfileField = new JTextField();
   protected final JTextField _errorTitleField = new JTextField();
   protected final JCheckBox _customProcNameCheck = new JCheckBox();
   protected final JCheckBox _stayAliveCheck = new JCheckBox();
   protected final JLabel _iconLabel = new JLabel();
   protected final JTextField _iconField = new JTextField();
   protected final JTextField _jarField = new JTextField();
   protected final JLabel _jarLabel = new JLabel();
   protected final JButton _jarButton = new JButton();
   protected final JButton _iconButton = new JButton();
   protected final JLabel _jarArgsLabel = new JLabel();
   protected final JTextField _jarArgsField = new JTextField();
   protected final JLabel _optionsLabel = new JLabel();
   protected final JLabel _chdirLabel = new JLabel();
   protected final JTextField _chdirField = new JTextField();
   protected final JCheckBox _dontWrapJarCheck = new JCheckBox();
   protected final JLabel _headerTypeLabel = new JLabel();
   protected final JRadioButton _guiHeaderRadio = new JRadioButton();
   protected final ButtonGroup _headerButtonGroup = new ButtonGroup();
   protected final JRadioButton _consoleHeaderRadio = new JRadioButton();
   protected final JTextArea _headerObjectsTextArea = new JTextArea();
   protected final JTextArea _libsTextArea = new JTextArea();
   protected final JCheckBox _headerObjectsCheck = new JCheckBox();
   protected final JCheckBox _libsCheck = new JCheckBox();
   protected final TitledSeparator _linkerOptionsSeparator = new TitledSeparator();
   protected final JLabel _jrePathLabel = new JLabel();
   protected final JLabel _jreMinLabel = new JLabel();
   protected final JLabel _jreMaxLabel = new JLabel();
   protected final JLabel _jvmArgsTextLabel = new JLabel();
   protected final JTextField _jrePathField = new JTextField();
   protected final JTextField _jreMinField = new JTextField();
   protected final JTextField _jreMaxField = new JTextField();
   protected final JTextArea _jvmArgsTextArea = new JTextArea();
   protected final JLabel _initialHeapSizeLabel = new JLabel();
   protected final JLabel _maxHeapSizeLabel = new JLabel();
   protected final JTextField _initialHeapSizeField = new JTextField();
   protected final JTextField _maxHeapSizeField = new JTextField();
   protected final JRadioButton _otherVarRadio = new JRadioButton();
   protected final ButtonGroup _buttongroup1 = new ButtonGroup();
   protected final JTextField _otherVarField = new JTextField();
   protected final JButton _addVarButton = new JButton();
   protected final JRadioButton _exeDirRadio = new JRadioButton();
   protected final JRadioButton _exeFileRadio = new JRadioButton();
   protected final JLabel _addVarsLabel = new JLabel();
   protected final JLabel _splashFileLabel = new JLabel();
   protected final JLabel _waitForWindowLabel = new JLabel();
   protected final JLabel _timeoutLabel = new JLabel();
   protected final JCheckBox _timeoutErrCheck = new JCheckBox();
   protected final JTextField _splashFileField = new JTextField();
   protected final JTextField _timeoutField = new JTextField();
   protected final JButton _splashFileButton = new JButton();
   protected final JCheckBox _splashCheck = new JCheckBox();
   protected final JCheckBox _waitForWindowCheck = new JCheckBox();
   protected final JCheckBox _versionInfoCheck = new JCheckBox();
   protected final JLabel _fileVersionLabel = new JLabel();
   protected final JTextField _fileVersionField = new JTextField();
   protected final TitledSeparator _addVersionInfoSeparator = new TitledSeparator();
   protected final JLabel _productVersionLabel = new JLabel();
   protected final JTextField _productVersionField = new JTextField();
   protected final JLabel _fileDescriptionLabel = new JLabel();
   protected final JTextField _fileDescriptionField = new JTextField();
   protected final JLabel _copyrightLabel = new JLabel();
   protected final JTextField _copyrightField = new JTextField();
   protected final JLabel _txtFileVersionLabel = new JLabel();
   protected final JTextField _txtFileVersionField = new JTextField();
   protected final JLabel _txtProductVersionLabel = new JLabel();
   protected final JTextField _txtProductVersionField = new JTextField();
   protected final JLabel _productNameLabel = new JLabel();
   protected final JTextField _productNameField = new JTextField();
   protected final JLabel _originalFilenameLabel = new JLabel();
   protected final JTextField _originalFilenameField = new JTextField();
   protected final JLabel _internalNameLabel = new JLabel();
   protected final JTextField _internalNameField = new JTextField();
   protected final JLabel _companyNameLabel = new JLabel();
   protected final JTextField _companyNameField = new JTextField();
   protected final JTextArea _logTextArea = new JTextArea();
   protected final TitledSeparator _logSeparator = new TitledSeparator();

   /**
    * Default constructor
    */
   public ConfigForm()
   {
      initializePanel();
   }

   /**
    * Adds fill components to empty cells in the first row and first column of the grid.
    * This ensures that the grid spacing will be the same as shown in the designer.
    * @param cols an array of column indices in the first row where fill components should be added.
    * @param rows an array of row indices in the first column where fill components should be added.
    */
   void addFillComponents( Container panel, int[] cols, int[] rows )
   {
      Dimension filler = new Dimension(10,10);

      boolean filled_cell_11 = false;
      CellConstraints cc = new CellConstraints();
      if ( cols.length > 0 && rows.length > 0 )
      {
         if ( cols[0] == 1 && rows[0] == 1 )
         {
            /** add a rigid area  */
            panel.add( Box.createRigidArea( filler ), cc.xy(1,1) );
            filled_cell_11 = true;
         }
      }

      for( int index = 0; index < cols.length; index++ )
      {
         if ( cols[index] == 1 && filled_cell_11 )
         {
            continue;
         }
         panel.add( Box.createRigidArea( filler ), cc.xy(cols[index],1) );
      }

      for( int index = 0; index < rows.length; index++ )
      {
         if ( rows[index] == 1 && filled_cell_11 )
         {
            continue;
         }
         panel.add( Box.createRigidArea( filler ), cc.xy(1,rows[index]) );
      }

   }

   /**
    * Helper method to load an image file from the CLASSPATH
    * @param imageName the package and name of the file to load relative to the CLASSPATH
    * @return an ImageIcon instance with the specified image file
    * @throws IllegalArgumentException if the image resource cannot be loaded.
    */
   public ImageIcon loadImage( String imageName )
   {
      try
      {
         ClassLoader classloader = getClass().getClassLoader();
         java.net.URL url = classloader.getResource( imageName );
         if ( url != null )
         {
            ImageIcon icon = new ImageIcon( url );
            return icon;
         }
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
      throw new IllegalArgumentException( "Unable to load image: " + imageName );
   }

   public JPanel createPanel()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:3DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:3DLU:NONE","CENTER:3DLU:NONE,FILL:DEFAULT:NONE,CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,FILL:DEFAULT:GROW(1.0),CENTER:3DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _tab.setName("tab");
      _tab.addTab("Basic",null,createPanel1());
      _tab.addTab("Header",null,createPanel2());
      _tab.addTab("JRE",null,createPanel3());
      _tab.addTab("Splash",null,createPanel5());
      _tab.addTab("Version Info",null,createPanel6());
      jpanel1.add(_tab,cc.xy(2,2));

      _logTextArea.setName("logTextArea");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_logTextArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xy(2,6));

      _logSeparator.setName("logSeparator");
      _logSeparator.setText("Log");
      jpanel1.add(_logSeparator,cc.xy(2,4));

      addFillComponents(jpanel1,new int[]{ 1,2,3 },new int[]{ 1,2,3,4,5,6,7 });
      return jpanel1;
   }

   public JPanel createPanel1()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:7DLU:NONE,RIGHT:MAX(65DLU;DEFAULT):NONE,FILL:3DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:3DLU:NONE,FILL:26PX:NONE,FILL:7DLU:NONE","CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:9DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _outfileButton.setIcon(loadImage("images/open16.png"));
      _outfileButton.setName("outfileButton");
      jpanel1.add(_outfileButton,cc.xy(6,2));

      _outfileLabel.setName("outfileLabel");
      _outfileLabel.setText("<html><b>Output file</b></html>");
      jpanel1.add(_outfileLabel,cc.xy(2,2));

      _errorTitleLabel.setName("errorTitleLabel");
      _errorTitleLabel.setText("Error title");
      jpanel1.add(_errorTitleLabel,cc.xy(2,14));

      _outfileField.setName("outfileField");
      _outfileField.setToolTipText("Output executable file.");
      jpanel1.add(_outfileField,cc.xy(4,2));

      _errorTitleField.setName("errorTitleField");
      _errorTitleField.setToolTipText("Launch4j signals errors using a message box, you can set it's title to the application's name.");
      jpanel1.add(_errorTitleField,cc.xy(4,14));

      _customProcNameCheck.setActionCommand("Custom process name");
      _customProcNameCheck.setName("customProcNameCheck");
      _customProcNameCheck.setText("Custom process name");
      jpanel1.add(_customProcNameCheck,cc.xy(4,16));

      _stayAliveCheck.setActionCommand("Stay alive after launching a GUI application");
      _stayAliveCheck.setName("stayAliveCheck");
      _stayAliveCheck.setText("Stay alive after launching a GUI application");
      jpanel1.add(_stayAliveCheck,cc.xy(4,18));

      _iconLabel.setName("iconLabel");
      _iconLabel.setText("Icon");
      jpanel1.add(_iconLabel,cc.xy(2,8));

      _iconField.setName("iconField");
      _iconField.setToolTipText("Application icon.");
      jpanel1.add(_iconField,cc.xy(4,8));

      _jarField.setName("jarField");
      _jarField.setToolTipText("Application jar.");
      jpanel1.add(_jarField,cc.xy(4,4));

      _jarLabel.setName("jarLabel");
      _jarLabel.setText("<html><b>Jar</b></html>");
      jpanel1.add(_jarLabel,cc.xy(2,4));

      _jarButton.setIcon(loadImage("images/open16.png"));
      _jarButton.setName("jarButton");
      jpanel1.add(_jarButton,cc.xy(6,4));

      _iconButton.setIcon(loadImage("images/open16.png"));
      _iconButton.setName("iconButton");
      jpanel1.add(_iconButton,cc.xy(6,8));

      _jarArgsLabel.setName("jarArgsLabel");
      _jarArgsLabel.setText("Jar arguments");
      jpanel1.add(_jarArgsLabel,cc.xy(2,12));

      _jarArgsField.setName("jarArgsField");
      _jarArgsField.setToolTipText("Constant command line arguments passed to the application.");
      jpanel1.add(_jarArgsField,cc.xy(4,12));

      _optionsLabel.setName("optionsLabel");
      _optionsLabel.setText("Options");
      jpanel1.add(_optionsLabel,cc.xy(2,16));

      _chdirLabel.setName("chdirLabel");
      _chdirLabel.setText("Change dir");
      jpanel1.add(_chdirLabel,cc.xy(2,10));

      _chdirField.setName("chdirField");
      _chdirField.setToolTipText("Change current directory to a location relative to the executable. Empty field has no effect, . - changes directory to the exe location.");
      jpanel1.add(_chdirField,cc.xy(4,10));

      _dontWrapJarCheck.setActionCommand("Don't wrap the jar, launch it only");
      _dontWrapJarCheck.setName("dontWrapJarCheck");
      _dontWrapJarCheck.setText("Don't wrap the jar, launch only");
      jpanel1.add(_dontWrapJarCheck,cc.xy(4,6));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19 });
      return jpanel1;
   }

   public JPanel createPanel2()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:7DLU:NONE,RIGHT:MAX(65DLU;DEFAULT):NONE,FILL:3DLU:NONE,FILL:DEFAULT:NONE,FILL:7DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:7DLU:NONE","CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,FILL:DEFAULT:GROW(0.2),CENTER:3DLU:NONE,FILL:DEFAULT:GROW(1.0),CENTER:9DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _headerTypeLabel.setName("headerTypeLabel");
      _headerTypeLabel.setText("Header type");
      jpanel1.add(_headerTypeLabel,cc.xy(2,2));

      _guiHeaderRadio.setActionCommand("GUI");
      _guiHeaderRadio.setName("guiHeaderRadio");
      _guiHeaderRadio.setText("GUI");
      _headerButtonGroup.add(_guiHeaderRadio);
      jpanel1.add(_guiHeaderRadio,cc.xy(4,2));

      _consoleHeaderRadio.setActionCommand("Console");
      _consoleHeaderRadio.setName("consoleHeaderRadio");
      _consoleHeaderRadio.setText("Console");
      _headerButtonGroup.add(_consoleHeaderRadio);
      jpanel1.add(_consoleHeaderRadio,cc.xy(6,2));

      _headerObjectsTextArea.setName("headerObjectsTextArea");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_headerObjectsTextArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(4,6,4,1));

      _libsTextArea.setName("libsTextArea");
      JScrollPane jscrollpane2 = new JScrollPane();
      jscrollpane2.setViewportView(_libsTextArea);
      jscrollpane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane2,cc.xywh(4,8,4,1));

      _headerObjectsCheck.setActionCommand("Object files");
      _headerObjectsCheck.setName("headerObjectsCheck");
      _headerObjectsCheck.setText("Object files");
      jpanel1.add(_headerObjectsCheck,new CellConstraints(2,6,1,1,CellConstraints.DEFAULT,CellConstraints.TOP));

      _libsCheck.setActionCommand("w32api");
      _libsCheck.setName("libsCheck");
      _libsCheck.setText("w32api");
      jpanel1.add(_libsCheck,new CellConstraints(2,8,1,1,CellConstraints.DEFAULT,CellConstraints.TOP));

      _linkerOptionsSeparator.setName("linkerOptionsSeparator");
      _linkerOptionsSeparator.setText("Custom header - linker options");
      jpanel1.add(_linkerOptionsSeparator,cc.xywh(2,4,6,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8 },new int[]{ 1,2,3,4,5,6,7,8,9 });
      return jpanel1;
   }

   public JPanel createPanel3()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:7DLU:NONE,RIGHT:MAX(65DLU;DEFAULT):NONE,FILL:3DLU:NONE,FILL:60DLU:NONE,FILL:3DLU:NONE,FILL:DEFAULT:NONE,FILL:DEFAULT:GROW(1.0),FILL:7DLU:NONE","CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,FILL:DEFAULT:GROW(1.0),CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:9DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _jrePathLabel.setName("jrePathLabel");
      _jrePathLabel.setText("<html><b>Emb. JRE path</b></html>");
      jpanel1.add(_jrePathLabel,cc.xy(2,2));

      _jreMinLabel.setName("jreMinLabel");
      _jreMinLabel.setText("<html><b>Min JRE version</b></html>");
      jpanel1.add(_jreMinLabel,cc.xy(2,4));

      _jreMaxLabel.setName("jreMaxLabel");
      _jreMaxLabel.setText("Max JRE version");
      jpanel1.add(_jreMaxLabel,cc.xy(2,6));

      _jvmArgsTextLabel.setName("jvmArgsTextLabel");
      _jvmArgsTextLabel.setText("JVM arguments");
      jpanel1.add(_jvmArgsTextLabel,new CellConstraints(2,12,1,1,CellConstraints.DEFAULT,CellConstraints.TOP));

      _jrePathField.setName("jrePathField");
      _jrePathField.setToolTipText("Embedded JRE path relative to the executable.");
      jpanel1.add(_jrePathField,cc.xywh(4,2,4,1));

      _jreMinField.setName("jreMinField");
      jpanel1.add(_jreMinField,cc.xy(4,4));

      _jreMaxField.setName("jreMaxField");
      jpanel1.add(_jreMaxField,cc.xy(4,6));

      _jvmArgsTextArea.setName("jvmArgsTextArea");
      _jvmArgsTextArea.setToolTipText("Accepts everything you would normally pass to java/javaw launcher: assertion options, system properties and X options.");
      JScrollPane jscrollpane1 = new JScrollPane();
      jscrollpane1.setViewportView(_jvmArgsTextArea);
      jscrollpane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      jscrollpane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jpanel1.add(jscrollpane1,cc.xywh(4,12,4,1));

      _initialHeapSizeLabel.setName("initialHeapSizeLabel");
      _initialHeapSizeLabel.setText("Initial heap size");
      jpanel1.add(_initialHeapSizeLabel,cc.xy(2,8));

      _maxHeapSizeLabel.setName("maxHeapSizeLabel");
      _maxHeapSizeLabel.setText("Max heap size");
      jpanel1.add(_maxHeapSizeLabel,cc.xy(2,10));

      JLabel jlabel1 = new JLabel();
      jlabel1.setText("MB");
      jpanel1.add(jlabel1,cc.xy(6,8));

      JLabel jlabel2 = new JLabel();
      jlabel2.setText("MB");
      jpanel1.add(jlabel2,cc.xy(6,10));

      _initialHeapSizeField.setName("initialHeapSizeField");
      jpanel1.add(_initialHeapSizeField,cc.xy(4,8));

      _maxHeapSizeField.setName("maxHeapSizeField");
      jpanel1.add(_maxHeapSizeField,cc.xy(4,10));

      jpanel1.add(createPanel4(),cc.xywh(4,14,4,1));
      _addVarsLabel.setName("addVarsLabel");
      _addVarsLabel.setText("Add variables");
      jpanel1.add(_addVarsLabel,cc.xy(2,14));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 });
      return jpanel1;
   }

   public JPanel createPanel4()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:DEFAULT:NONE,FILL:7DLU:NONE,FILL:DEFAULT:NONE,FILL:7DLU:NONE,FILL:DEFAULT:NONE,FILL:3DLU:NONE,FILL:60DLU:GROW(1.0),FILL:3DLU:NONE,FILL:DEFAULT:NONE","CENTER:DEFAULT:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _otherVarRadio.setActionCommand("Other");
      _otherVarRadio.setName("otherVarRadio");
      _otherVarRadio.setText("Other");
      _buttongroup1.add(_otherVarRadio);
      jpanel1.add(_otherVarRadio,cc.xy(5,1));

      _otherVarField.setName("otherVarField");
      jpanel1.add(_otherVarField,cc.xy(7,1));

      _addVarButton.setActionCommand("Add");
      _addVarButton.setIcon(loadImage("images/edit_add16.png"));
      _addVarButton.setName("addVarButton");
      _addVarButton.setText("Add");
      jpanel1.add(_addVarButton,cc.xy(9,1));

      _exeDirRadio.setActionCommand("%EXEPATH%");
      _exeDirRadio.setName("exeDirRadio");
      _exeDirRadio.setText("EXEDIR");
      _buttongroup1.add(_exeDirRadio);
      jpanel1.add(_exeDirRadio,cc.xy(1,1));

      _exeFileRadio.setActionCommand("%EXEFILE%");
      _exeFileRadio.setName("exeFileRadio");
      _exeFileRadio.setText("EXEFILE");
      _buttongroup1.add(_exeFileRadio);
      jpanel1.add(_exeFileRadio,cc.xy(3,1));

      addFillComponents(jpanel1,new int[]{ 2,4,6,8 },new int[0]);
      return jpanel1;
   }

   public JPanel createPanel5()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:7DLU:NONE,RIGHT:MAX(65DLU;DEFAULT):NONE,FILL:3DLU:NONE,FILL:60DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:3DLU:NONE,FILL:26PX:NONE,FILL:7DLU:NONE","CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:9DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _splashFileLabel.setName("splashFileLabel");
      _splashFileLabel.setText("Splash file");
      jpanel1.add(_splashFileLabel,cc.xy(2,4));

      _waitForWindowLabel.setName("waitForWindowLabel");
      _waitForWindowLabel.setText("Wait for window");
      jpanel1.add(_waitForWindowLabel,cc.xy(2,6));

      _timeoutLabel.setName("timeoutLabel");
      _timeoutLabel.setText("Timeout [s]");
      jpanel1.add(_timeoutLabel,cc.xy(2,8));

      _timeoutErrCheck.setActionCommand("Signal error on timeout");
      _timeoutErrCheck.setName("timeoutErrCheck");
      _timeoutErrCheck.setText("Signal error on timeout");
      _timeoutErrCheck.setToolTipText("True signals an error on splash timeout, false closes the splash screen quietly.");
      jpanel1.add(_timeoutErrCheck,cc.xywh(4,10,2,1));

      _splashFileField.setName("splashFileField");
      _splashFileField.setToolTipText("Splash screen file in BMP format.");
      jpanel1.add(_splashFileField,cc.xywh(4,4,2,1));

      _timeoutField.setName("timeoutField");
      _timeoutField.setToolTipText("Number of seconds after which the splash screen must close. Splash timeout may cause an error depending on splashTimeoutErr property.");
      jpanel1.add(_timeoutField,cc.xy(4,8));

      _splashFileButton.setIcon(loadImage("images/open16.png"));
      _splashFileButton.setName("splashFileButton");
      jpanel1.add(_splashFileButton,cc.xy(7,4));

      _splashCheck.setActionCommand("Enable splash screen");
      _splashCheck.setName("splashCheck");
      _splashCheck.setText("Enable splash screen");
      jpanel1.add(_splashCheck,cc.xywh(4,2,2,1));

      _waitForWindowCheck.setActionCommand("Close splash screen when an application window appears");
      _waitForWindowCheck.setName("waitForWindowCheck");
      _waitForWindowCheck.setText("Close splash screen when an application window appears");
      jpanel1.add(_waitForWindowCheck,cc.xywh(4,6,2,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11 });
      return jpanel1;
   }

   public JPanel createPanel6()
   {
      JPanel jpanel1 = new JPanel();
      FormLayout formlayout1 = new FormLayout("FILL:7DLU:NONE,RIGHT:MAX(65DLU;DEFAULT):NONE,FILL:3DLU:NONE,FILL:60DLU:NONE,FILL:7DLU:NONE,RIGHT:DEFAULT:NONE,FILL:3DLU:NONE,FILL:DEFAULT:GROW(1.0),FILL:7DLU:NONE","CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:9DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:3DLU:NONE,CENTER:DEFAULT:NONE,CENTER:9DLU:NONE");
      CellConstraints cc = new CellConstraints();
      jpanel1.setLayout(formlayout1);

      _versionInfoCheck.setActionCommand("Add version information");
      _versionInfoCheck.setName("versionInfoCheck");
      _versionInfoCheck.setText("Add version information");
      jpanel1.add(_versionInfoCheck,cc.xywh(4,2,5,1));

      _fileVersionLabel.setName("fileVersionLabel");
      _fileVersionLabel.setText("File version");
      jpanel1.add(_fileVersionLabel,cc.xy(2,4));

      _fileVersionField.setName("fileVersionField");
      _fileVersionField.setToolTipText("Version number 'x.x.x.x'");
      jpanel1.add(_fileVersionField,cc.xy(4,4));

      _addVersionInfoSeparator.setName("addVersionInfoSeparator");
      _addVersionInfoSeparator.setText("Additional information");
      jpanel1.add(_addVersionInfoSeparator,cc.xywh(2,10,7,1));

      _productVersionLabel.setName("productVersionLabel");
      _productVersionLabel.setText("Product version");
      jpanel1.add(_productVersionLabel,cc.xy(2,12));

      _productVersionField.setName("productVersionField");
      _productVersionField.setToolTipText("Version number 'x.x.x.x'");
      jpanel1.add(_productVersionField,cc.xy(4,12));

      _fileDescriptionLabel.setName("fileDescriptionLabel");
      _fileDescriptionLabel.setText("File description");
      jpanel1.add(_fileDescriptionLabel,cc.xy(2,6));

      _fileDescriptionField.setName("fileDescriptionField");
      _fileDescriptionField.setToolTipText("File description presented to the user.");
      jpanel1.add(_fileDescriptionField,cc.xywh(4,6,5,1));

      _copyrightLabel.setName("copyrightLabel");
      _copyrightLabel.setText("Copyright");
      jpanel1.add(_copyrightLabel,cc.xy(2,8));

      _copyrightField.setName("copyrightField");
      jpanel1.add(_copyrightField,cc.xywh(4,8,5,1));

      _txtFileVersionLabel.setName("txtFileVersionLabel");
      _txtFileVersionLabel.setText("Free form");
      jpanel1.add(_txtFileVersionLabel,cc.xy(6,4));

      _txtFileVersionField.setName("txtFileVersionField");
      _txtFileVersionField.setToolTipText("Free form file version, for example '1.20.RC1'.");
      jpanel1.add(_txtFileVersionField,cc.xy(8,4));

      _txtProductVersionLabel.setName("txtProductVersionLabel");
      _txtProductVersionLabel.setText("Free form");
      jpanel1.add(_txtProductVersionLabel,cc.xy(6,12));

      _txtProductVersionField.setName("txtProductVersionField");
      _txtProductVersionField.setToolTipText("Free form product version, for example '1.20.RC1'.");
      jpanel1.add(_txtProductVersionField,cc.xy(8,12));

      _productNameLabel.setName("productNameLabel");
      _productNameLabel.setText("Product name");
      jpanel1.add(_productNameLabel,cc.xy(2,14));

      _productNameField.setName("productNameField");
      jpanel1.add(_productNameField,cc.xywh(4,14,5,1));

      _originalFilenameLabel.setName("originalFilenameLabel");
      _originalFilenameLabel.setText("Original filename");
      jpanel1.add(_originalFilenameLabel,cc.xy(2,20));

      _originalFilenameField.setName("originalFilenameField");
      _originalFilenameField.setToolTipText("Original name of the file without the path. Allows to determine whether a file has been renamed by a user.");
      jpanel1.add(_originalFilenameField,cc.xywh(4,20,5,1));

      _internalNameLabel.setName("internalNameLabel");
      _internalNameLabel.setText("Internal name");
      jpanel1.add(_internalNameLabel,cc.xy(2,18));

      _internalNameField.setName("internalNameField");
      _internalNameField.setToolTipText("Internal name without extension, original filename or module name for example.");
      jpanel1.add(_internalNameField,cc.xywh(4,18,5,1));

      _companyNameLabel.setName("companyNameLabel");
      _companyNameLabel.setText("Company name");
      jpanel1.add(_companyNameLabel,cc.xy(2,16));

      _companyNameField.setName("companyNameField");
      jpanel1.add(_companyNameField,cc.xywh(4,16,5,1));

      addFillComponents(jpanel1,new int[]{ 1,2,3,4,5,6,7,8,9 },new int[]{ 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21 });
      return jpanel1;
   }

   /**
    * Initializer
    */
   protected void initializePanel()
   {
      setLayout(new BorderLayout());
      add(createPanel(), BorderLayout.CENTER);
   }


}
