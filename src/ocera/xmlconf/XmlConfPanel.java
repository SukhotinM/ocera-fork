package ocera.xmlconf;

/*
 * @(#)XmlConf.java	1.20 02/06/13
 */

import ocera.util.FFileFilter;
import ocera.util.FramedLayoutPanel;
import ocera.util.StringParser;
import ocera.util.xmlconfig.XmlConfig;
import ocera.util.xmlconfig.XmlConfElement;
import ocera.msg.InfoMsg;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jdom.Element;

/**
 * Sample application using the simple text editor component that
 * supports only one font.
 *
 * @author  Timothy Prinzing
 * @version 1.20 06/13/02
 */
public class XmlConfPanel extends JPanel {

    static
    {
        // we can do a initial config here
    }

    protected JTree tree;
    protected DefaultTreeModel treeModel;
    //protected JMenuBar menubar;
    protected ActionMap actions = new ActionMap();
    protected JToolBar toolbar;
    protected JComponent status;

    protected JLabel lblValue;
    protected JComboBox cbxValue;
    protected JButton btChoose;
    protected JButton btDefault;
    protected JTextArea txtHelp;

    protected XmlConfig config = null;
    protected DefaultMutableTreeNode lastSelected;
    protected String currentFileName = null;

    protected FileDialog fileDialog;
//    private DefaultMutableTreeNode treeRootNode;

    public XmlConfPanel()
    {
        super(true);
/*
        // Force SwingSet to come up in the Cross Platform L&F
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            // If you want the System L&F instead, comment out the above line and
            // uncomment the following:
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exc) {
            System.err.println("Error loading L&F: " + exc);
        }

        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout());
*/
        initActions();
        initGUI();
        initListeners();
        refreshEnabledActions();
    }

    /**
     * @return current configuration
     */
    public XmlConfig getConfig()
    {
        return config;
    }
    /**
     * assign config to show on the panel
     */
    public void setConfig(XmlConfig conf)
    {
        config = conf;
        if(config == null)
            throw new NullPointerException("assigned config is NULL");
        //tree.setShowsRootHandles(true);
        DefaultMutableTreeNode nd = new DefaultMutableTreeNode("config");
        addChildrenToTree(config.getRootElement().getElement(), nd);
        treeModel.setRoot(nd);
        refreshEnabledActions();
    }


    protected void initActions() {
        /* The image filename is relative to
         * the classpath (including the '.' directory if its a part of the
         * classpath), and may either be in a JAR file or a separate file.
         */
        //ImageIcon ico = new ImageIcon("./ocera/xmlconf/resources/file-open.gif");
        // IT DOES NOT WORK, I don't know why

        // file name is relative to package
        // this works even if class is in the jar
        URL url = this.getClass().getResource("resources/file-open.gif");
        //System.out.println("url: " + url);
        ImageIcon ico = new ImageIcon(url);
        Action act;
        act = new AbstractAction("open", ico) {
                    public void actionPerformed(ActionEvent e)
                    {
                        JFileChooser fc = new JFileChooser();
                        FFileFilter filter = new FFileFilter();
                        filter.addExtension("xml");
                        filter.setDescription("Xml Configurator files");
                        fc.setFileFilter(filter);
                        String cwd = System.getProperty("user.dir", "/");
                        fc.setCurrentDirectory(new File(cwd));

                        int ret = fc.showOpenDialog(getWindow());
                        if(ret == JFileChooser.APPROVE_OPTION) {
                            String fname = fc.getSelectedFile().getAbsolutePath();
                            //new InfoMsg(getWindow()).show("oteviram konfiguraci " + fname);
                            openConfigFile(fname);
                        }
                    }
                };
        act.putValue("toolTip", "Open configuration file");
        actions.put("open", act);

        url = this.getClass().getResource("resources/file-save-as.gif");
        ico = new ImageIcon(url);
        act = new AbstractAction("saveAs", ico) {
                    public void actionPerformed(ActionEvent e)
                    {
                        JFileChooser fc = new JFileChooser();
                        FFileFilter filter = new FFileFilter();
                        filter.addExtension("xml");
                        filter.setDescription("Xml Configurator files");
                        fc.setFileFilter(filter);
//                        String cwd = System.getProperty("user.dir", "/");
//                        fc.setCurrentDirectory(new File(cwd));

                        int ret = fc.showSaveDialog(getWindow());
                        if(ret == JFileChooser.APPROVE_OPTION) {
                            String fname = fc.getSelectedFile().getAbsolutePath();
                            saveConfigFile(fname);
                        }
                    }
                };
        act.putValue("toolTip", "Save configuration to the new file");
        actions.put("saveAs", act);

        url = this.getClass().getResource("resources/file-save.gif");
        ico = new ImageIcon(url);
        act = new AbstractAction("save", ico) {
                    public void actionPerformed(ActionEvent e)
                    {
                        saveConfigFile(currentFileName);
                    }
                };
        act.putValue("toolTip", "Save configuration file");
        act.setEnabled(false);
        actions.put("save", act);

        url = this.getClass().getResource("resources/unfold.gif");
        ico = new ImageIcon(url);
        act = new AbstractAction("unfold", ico) {
                    public void actionPerformed(ActionEvent e)
                    {
                        DefaultMutableTreeNode nd = (DefaultMutableTreeNode)treeModel.getRoot();
                        expandTreeNode(nd);
                    }
                };

        act.putValue("toolTip", "Expand whole tree");
        actions.put("unfold", act);
    }

    // support funstion for unfold tree
    protected void expandTreeNode(DefaultMutableTreeNode nd)
    {
        for(Enumeration children = nd.children(); children.hasMoreElements();) {
            DefaultMutableTreeNode nd1 = (DefaultMutableTreeNode) children.nextElement();
            if (!nd1.isLeaf()) {
                TreePath tp = new TreePath( nd1.getPath() );
                tree.expandPath(tp);
                expandTreeNode(nd1);
            }
        }
    }

    /**
     * Save the edited value to XML<br>
     */
    public void saveEditedField()
    {
        // select root, this fires tree selection listener and it makes all the work
        //tree.setSelectionRow(0);
        TreePath tp = new TreePath(((DefaultMutableTreeNode)treeModel.getRoot()).getPath());
        tree.setSelectionPath(tp);
    }

    protected Action getAction(String cmd) {
	    return actions.get(cmd);
    }

    protected void initGUI()
    {
        setLayout(new BorderLayout());

//        menubar = createMenubar();
//        add("North", menubar);

        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("empty"));
        tree = new JTree(treeModel);
        tree.setShowsRootHandles(true);

        toolbar = createToolBar();

        JPanel p1 = new JPanel(new BorderLayout());
        p1.add("North", toolbar);
        p1.add("Center", tree);

        JScrollPane sc01 = new JScrollPane(p1);

        FramedLayoutPanel pf1 = new FramedLayoutPanel();
        //pf1.setFrameColor(Color.RED);
        pf1.setLayout(new GridBagLayout());

        JSplitPane splpan = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sc01, pf1);
        splpan.setDividerLocation(200);
        splpan.setDividerSize(8);
        splpan.setResizeWeight(1); // zvetsuje se leva cast

        GridBagConstraints gbc = new GridBagConstraints();

        // mezera vlevo
        gbc.gridx = 0;
        gbc.gridy = 0;
        pf1.add(Box.createRigidArea(new Dimension(5, 5)));

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        lblValue = new JLabel("value name");
        pf1.add(lblValue, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 100;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cbxValue = new JComboBox();
        cbxValue.setBackground(Color.WHITE);
        pf1.add(cbxValue, gbc);
        gbc.weightx = 0;

        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.NONE;
        btChoose = new JButton("...");
        btChoose.setMargin(new Insets(1,3,1,3));
        pf1.add(btChoose, gbc);

        btDefault = new JButton("Default");
        btDefault.setMargin(new Insets(1,3,1,3));
        pf1.add(btDefault, gbc);

        gbc.gridx = 1;
        gbc.gridy = GridBagConstraints.RELATIVE;
        pf1.add(Box.createRigidArea(new Dimension(5, 5)), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        pf1.add(new JLabel("Help"), gbc);

        gbc.gridx = 1;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 100;
        gbc.weighty = 100;
        gbc.fill = GridBagConstraints.BOTH;
        txtHelp = new JTextArea("Help");
        txtHelp.setBackground(new Color(255,255,215));
        txtHelp.setBorder(BorderFactory.createLineBorder(Color.black));
        txtHelp.setMinimumSize(new Dimension(50, 50)); // tak tohla je zlaty, jinak se txtHelp neda zmensit pomoci spliteru
        // spliter totiz nezmensi komponentu pod jeji minimumSize
        // a kdyz minimumSize neni nastaveno, urcuje ji layout manager a to je pruser
        txtHelp.setLineWrap(true);
        pf1.add(txtHelp, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
//        toolbar = createToolBar();
//        panel.add("North", toolbar);
        panel.add("Center", splpan);

        add("Center", panel);
        add("South", createStatusbar());
    }

    protected void initListeners()
    {
        //==========================================================
        //        JTree listenners
        //==========================================================
        tree.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
//                selectedObject = null;
                if(e.isAddedPath()) {
//                    System.out.println(path.getLastPathComponent().getClass());
                    try {
                        if(lastSelected != null) {
                            // save value
                            Element el = (Element)lastSelected.getUserObject();
                            XmlConfElement xel = new XmlConfElement(el);
                            String new_val;
                            if(cbxValue.isEditable()) {
                                new_val = (String)cbxValue.getEditor().getItem();
                            }
                            else {
                                new_val = (String)cbxValue.getSelectedItem();
                            }
//                            System.out.println("new value: '" + new_val + "'");
                            xel.setValue(new_val);
                        }

                        DefaultMutableTreeNode nd = (DefaultMutableTreeNode)path.getLastPathComponent();
                        Element el = (Element)nd.getUserObject();
                        XmlConfElement xel = new XmlConfElement(el);
                        // supported types: edit, file, dir, bool, list, combo, user
                        String type = xel.getAttributeValue("type", "edit");
                        if(type.equalsIgnoreCase("edit") || type.equalsIgnoreCase("file") || type.equalsIgnoreCase("dir")) {
                            cbxValue.setEditable(true);
                            // store original value in first combo item
                            cbxValue.setModel(new DefaultComboBoxModel(new String[] {xel.getValue("")}));
                            cbxValue.setSelectedIndex(0);
//                            cbxValue.removeAllItems();
                        }
                        else {
                            cbxValue.setEditable(false);
                            if(type.equalsIgnoreCase("bool")) {
                                cbxValue.setModel(new DefaultComboBoxModel(new String[] {"no", "yes"}));
                                if(xel.getValue("no").equalsIgnoreCase("yes")) cbxValue.setSelectedIndex(1);
                                else cbxValue.setSelectedIndex(0);
                            }
                            else if(type.equalsIgnoreCase("list") || type.equalsIgnoreCase("combo")) {
                                // load items
                                StringParser sp = new StringParser(',', '\'');
                                String[] items = sp.split(xel.getAttributeValue("items", ""));
                                DefaultComboBoxModel model = new DefaultComboBoxModel(items);
                                cbxValue.setModel(model);
                                int ix = model.getIndexOf(xel.getValue(""));
                                cbxValue.setSelectedIndex(ix);
                            }
                        }
                        btChoose.setEnabled(type.equalsIgnoreCase("file") || type.equalsIgnoreCase("dir"));

                        String caption = el.getName();
                        lblValue.setText(xel.getAttributeValue("caption", caption));
                        cbxValue.setSelectedItem(xel.getValue("N/A"));
                        txtHelp.setText(xel.getAttributeValue("help", ""));
                        //btDefault.setEnabled(xel.getAttributeValue("default", "").length() > 0);
                        lastSelected = nd;
//                        if(nd.isLeaf()) System.out.println("Vybrany list " + el.getName());
//                        else  System.out.println("Vybrana vetev " + el);
                    } catch (ClassCastException ex) {
                    }
//                    enableControls();
//                    refreshForm();
                }
            }
        });
        //==========================================================
        //        buttons listenners
        //==========================================================
        btDefault.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(lastSelected == null) return;
                Element el = (Element)lastSelected.getUserObject();
                XmlConfElement xel = new XmlConfElement(el);
                String defval = xel.getAttributeValue("default", "");
                cbxValue.setSelectedItem(defval);
            }
        });

        btChoose.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(lastSelected == null) return;
                Element el = (Element)lastSelected.getUserObject();
                XmlConfElement xel = new XmlConfElement(el);
                String type = xel.getAttributeValue("type", "edit");
                if(type.equalsIgnoreCase("file") || type.equalsIgnoreCase("dir")) {
                    JFileChooser fc = new JFileChooser();
                    if(type.equalsIgnoreCase("dir")) fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    else if(type.equalsIgnoreCase("file")) fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    String cwd = xel.getValue(System.getProperty("user.dir", "/"));
                    fc.setCurrentDirectory(new File(cwd));
                    int ret = fc.showOpenDialog(getWindow());
                    if(ret == JFileChooser.APPROVE_OPTION) {
                        String fname = fc.getSelectedFile().getAbsolutePath();
                        cbxValue.setSelectedItem(fname);
                    }
                }
            }
        });

        //==========================================================
        //        combo listenners
        //==========================================================
    }

    protected Component createStatusbar() {
	    // need to do something reasonable here
	    status = new StatusBar();
	    return status;
    }

    /**
     * Hook through which every toolbar item is created.
     */
    protected JToolBar createToolBar()
    {
        JToolBar toolb = new JToolBar();
        String[] itemKeys = {"open", "saveAs", "unfold"};
        for (int j = 0; j < itemKeys.length; j++) {
            JButton bt = createToolBarButton(itemKeys[j]);
            toolb.add(bt);
        }
        return toolb;
    }

    /**
     * Create a button to go inside of the toolbar.  By default this
     * will load an image resource.
     *
     * @param key The key in the resource file to serve as the basis
     *  of lookups.
     */
    protected JButton createToolBarButton(String key)
    {
	    Action act = actions.get(key);
        if(act != null) {
            JButton b = new JButton(act) {
                public float getAlignmentY() { return 0.5f; }
            };
            b.setRequestFocusEnabled(false);
            b.setMargin(new Insets(1,1,1,1));
            b.setText("");
            b.setToolTipText((String) act.getValue("toolTip"));
            return b;
        }
        return null;
    }

    /**
     * FIXME - I'm not very useful yet
     */
    class StatusBar extends JComponent
    {
        public StatusBar() {
	        super();
	        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	    }

        public void paint(Graphics g)
        {
	        super.paint(g);
	    }
    }

    /**
     * Find the hosting frame, for the file-chooser dialog.
     */
    protected java.awt.Window getWindow()
    {
	    for (Container p = getParent(); p != null; p = p.getParent()) {
	        if (java.awt.Window.class.isInstance(p)) {
    		    return (java.awt.Window) p;
	        }
	    }
    	throw new NullPointerException("Container does not contain a JWindow.");
    }

    void refreshEnabledActions()
    {
        boolean can_save_as = config != null;
        boolean can_save = currentFileName != null && can_save_as;
        actions.get("save").setEnabled(can_save);
        actions.get("saveAs").setEnabled(can_save_as);

    }

    //===================================== not GUI functions ========================
    public void openConfigFile(String fname)
    {
        XmlConfig cfg =  new XmlConfig();
        cfg.fromURI(fname);
        setConfig(cfg);
        currentFileName = fname;
//        getWindow().setTitle("Xml Configurator - " + fname);
    }

    /**
     * invoke SaveFile dialog and save config file
     */
    public void saveConfigFileAs(String fname)
    {
        getAction("saveAs").actionPerformed(null);
    }

    void saveConfigFile(String fname)
    {
        if(config == null) return;

        // save edited field by moving selection to the root node
        saveEditedField();

        String s = config.toString();
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fname));
            out.write(s.getBytes());
        } catch (FileNotFoundException e1) {
            new InfoMsg(getWindow()).show("Save file error: " + e1);
        } catch (IOException e1) {
            new InfoMsg(getWindow()).show("Save file error: " + e1);
        }

        refreshEnabledActions();
    }

    void addChildrenToTree(Element el, DefaultMutableTreeNode nd)
    {
        java.util.List children = el.getChildren();
        Iterator it = children.iterator();
        while (it.hasNext()) {
            Element child = (Element) it.next();
            if(child.getAttributeValue("hidden", "no").toLowerCase().equals("yes")) continue;
            DefaultMutableTreeNode chnd = new DefaultMutableTreeNode(child) {
                public String toString() {
                    XmlConfElement xel = new XmlConfElement((Element)getUserObject());
                    String caption = xel.getElement().getName();
                    caption = xel.getAttributeValue("caption", caption);
                    return caption;
                }
            };
            addChildrenToTree(child, chnd);
            nd.add(chnd);
        }
    }
}
