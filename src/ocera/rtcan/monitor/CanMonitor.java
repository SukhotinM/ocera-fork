/**
 * Created by IntelliJ IDEA.
 * User: Vacek
 * Date: Nov 1, 2002
 * Time: 9:30:53 AM
 * To change this template use Options | File Templates.
 */
package ocera.rtcan.monitor;

import ocera.util.xmlconfig.XmlConfig;
import ocera.util.*;
import ocera.msg.*;
import ocera.rtcan.CanMonClient;
import ocera.rtcan.msg.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import org.jx.xmlgui.XMLMenuBuilder;
import org.jx.xmlgui.XMLActionMapBuilder;
import org.jx.xmlgui.XMLAction;
import org.jx.xmlgui.XMLToolbarBuilder;
import org.flib.FLog;
import org.flib.FString;

class LogTextAreaDocumentFilter extends DocumentFilter
{
    private int maxChars;
    private int charGap;

    LogTextAreaDocumentFilter() {
        this(1000);
    }
    LogTextAreaDocumentFilter(int max_chars) {
        maxChars = max_chars;
        charGap = maxChars / 3;
    }

    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException
    {
        Document doc = fb.getDocument();
        if(doc.getLength() + str.length() > maxChars) {
            doc.remove(0, charGap);
        }
        super.insertString(fb, doc.getLength(), str, a);
    }
}

public class CanMonitor extends JFrame implements Runnable
{
    protected JTextField edCanID;
    protected JTextField edCanData0;
    protected JTextField edCanData1;
    protected JTextField edCanData2;
    protected JTextField edCanData3;
    protected JTextField edCanData4;
    protected JTextField edCanData5;
    protected JTextField edCanData6;
    protected JTextField edCanData7;
    protected JTextField[] edCanData = new JTextField[8];

    protected CanMonClient canConn = new CanMonClient();
    //protected int nodeNo = 1;  // number of the node to communicate with

    protected Container pane;
    protected ActionMap actions = new ActionMap();

    protected JPanel panSDOTable;

    protected JTabbedPane tabPane;
    protected JTextArea txtLog;

    protected XmlConfig xmlConfig = new XmlConfig();
    public static final String CONFIG_FILE_NAME = "CanMonitor.conf.xml";
    protected boolean configChanged = false;
    private ConfigLookup confLookup = new ConfigLookup(".canmonitor", CONFIG_FILE_NAME, this.getClass());
    private CanMonStatusBar statusBar = new CanMonStatusBar();
    private JButton btSend;
    //private LinkedList candeviceList = new LinkedList();
    private JButton btClearLog;

    protected JCheckBox cbxShowRoughMessages;
    boolean cbxShowRoughMessages_prev_state = false;

    private JTextField edRawMessagesId;
    private JTextField edRawMessagsMask;
    private JTextField edRawMessagesMask;
    private JPanel lblRawId;
    private JCheckBox cbxCanFlagRTR;
    private JCheckBox cbxCanFlagEXT;
    private JCheckBox cbxCanFlagOVR;
    private JCheckBox cbxCanFlagLOCAL;

    public static void main (String [] args)
    {
        FLog.logTreshold = FLog.LOG_ERR;
        //FLog.logTreshold = FLog.LOG_DEB;
        CanMonitor app = new CanMonitor(args);
        app.setDefaultCloseOperation(EXIT_ON_CLOSE);
        app.setSize(800, 600);
        app.setVisible(true);
//        app.pack();
    }

    private void openConfigDialog()
    {
        ConfigDialog xconf = new ConfigDialog(xmlConfig);
        xconf.show();
        configChanged = true;
    }

    private void processCmdLine(String[] args)
    {
        if(args == null) return;
        String edsFile = null;
        for(int i = 0; i < args.length; i++) {
            String s = args[i];
            if(FString.charAt(s, 0) == '-') {
                char c = FString.charAt(s, 1);
                if(c == 'a') {
                    // address
                    i++;
                    if(i < args.length) {
                        xmlConfig.setValue("/connection/host", args[i]);
                        //canProxyIP = args[i];
                    }
                }
                else if(c == 'n') {
                    // node
                    i++;
                    if(i < args.length) {
                        xmlConfig.setValue("/canopen/node", args[i]);
                        //xmlConfig.getRootElement().cd("/canopen/node").setValue(args[i]);
                        //nodeNo = FString.toInt(args[i]);
                    }
                }
                else if(c == 'e') {
                    // EDS file name
                    i++;
                    if(i < args.length) edsFile = args[i];
                }
                else if(c == 'v') {
                    // verbose
                    FLog.logTreshold = FLog.LOG_INF;
                }
                else if(c == 'g') {
                    // debug level
                    i++;
                    if(i < args.length) FLog.logTreshold = Integer.parseInt(args[i]);
                    System.err.println("logTreshold: " + FLog.logTreshold);
                }
                else if(c == 'h') {
                    // help
                    System.out.println("USAGE: cammonitor -a host -n node -e EDS_file_name -v -g debug_level\n");
                    System.exit(0);
                }
            }
        }
        if(edsFile != null) {
            openEDS(edsFile);
        }
    }

    protected void connect()
    {
        String ip = xmlConfig.getRootElement().cd("/connection/host").getValue("localhost").trim();
        String port = xmlConfig.getValue("/connection/port", "1001").trim();
        int p = FString.toInt(port);
        if(ip.length() > 0) {
            txtLog.append("\nconnecting to " + ip + " ...\n");
            canConn.connect(ip, p);
            txtLog.append(canConn.getErrMsg() + "\n");
        }
        refreshForm();
    }

    protected void disConnect()
    {
        canConn.disconnect();
        refreshForm();
    }

    public CanMonitor(String[] cmd_line_args)
    {
        super("CANopen monitor - Beta 0.9");
        boolean asserts_enabled = false;
        assert asserts_enabled = true;
        
    
        edCanData[0] = edCanData0;
        edCanData[1] = edCanData1;
        edCanData[2] = edCanData2;
        edCanData[3] = edCanData3;
        edCanData[4] = edCanData4;
        edCanData[5] = edCanData5;
        edCanData[6] = edCanData6;
        edCanData[7] = edCanData7;

        loadConfig();
        processCmdLine(cmd_line_args);
        System.err.println("asserts enabled: " + asserts_enabled);
        System.err.println("Logging level: " + FLog.logTreshold);
        initActions();
        initGui();
        init();
        connect();
//        setBounds(50, 50, 800, 600);
    }

    /**
     * Opens configuration file, triing next locations:<br>
     */
    protected void loadConfig()
    {
        String conf_file = confLookup.findConfigFile();
        System.out.println("loading config from '" + conf_file + "'");
        if(conf_file != null) xmlConfig.fromURI(conf_file);
        else FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " not found.");
    }

    protected void saveConfig()
    {
        if(!configChanged) return;
        String conf_file = confLookup.createConfigFile();
        if(conf_file == null) {
            FLog.log("CanMonitor", FLog.LOG_ERR, "Configuration file " + CONFIG_FILE_NAME + " cann't be written.");
            return;
        }

        String s = xmlConfig.toString();
        FFile ff = new FFile(conf_file);
        try {
            ff.writeString(s, FFile.MODE_OVERWRITE, FFile.OVERWRITE_FILE);
        } catch (IOException e) {
            ErrorMsg.show("Error writing config file: " + e);
        } catch (FFileException e) {
            ErrorMsg.show("Error writing config file: " + e);
        }
    }

    public void initActions()
    {
        //final CanMonitor app = this;

        XMLActionMapBuilder ab = new XMLActionMapBuilder(this);
        actions = ab.buildFrom("resources/menu.xml");

        XMLAction act;
        act = (XMLAction)actions.get("EdsOpen");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                FFileFilter filter = new FFileFilter();
                filter.addExtension("EDS");
                filter.addExtension("eds");
                filter.setDescription("EDS Electronic Data Sheets");
                fc.setFileFilter(filter);
                String pwd = System.getProperty("user.dir", ".");
                fc.setCurrentDirectory(new File(pwd));

                int ret = fc.showOpenDialog(CanMonitor.this);
                if(ret == JFileChooser.APPROVE_OPTION) {
                    String fname = fc.getSelectedFile().getAbsolutePath();
                    openEDS(fname);
                }
            }
        };

        act = (XMLAction)actions.get("EdsClose");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                int ix = tabPane.getSelectedIndex();
                if(ix > 0) {
                    CANopenDevicePanel candev = (CANopenDevicePanel)tabPane.getComponentAt(ix);
                    tabPane.remove(candev);
                    //candeviceList.remove(candev);
                }
            }
        };

        act = (XMLAction)actions.get("Connect");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        };

        act = (XMLAction)actions.get("Disconnect");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                disConnect();
            }
        };

        act = (XMLAction)actions.get("Config");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                openConfigDialog();
            }
        };

        act = (XMLAction)actions.get("DevicesReset");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                edCanID.setText("0");
                edCanData[0].setText("1");
                edCanData[1].setText("0");
                btSend.getActionListeners()[0].actionPerformed(null);
            }
        };

        act = (XMLAction)actions.get("Quit");
        if(act != null) act.actionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                cleanUp();
                System.gc();
                System.exit(0);
            }
        };
    }

    private void openEDS(String fname)
    {
        int ix = tabPane.getTabCount();
        // find free node number
        int node = 0;
        for(int i=1; i<ix; i++) {
            CANopenDevicePanel p =  (CANopenDevicePanel)tabPane.getComponentAt(i);
            int nd = p.getNodeID();
            if(nd > node) node = nd;
        }
        CANopenDevicePanel candev = new CANopenDevicePanel(CanMonitor.this, ix);
        tabPane.add(candev);
        tabPane.setSelectedIndex(ix);    // select tab EDS
        candev.openEDS(fname);
        candev.setNodeID(++node);
    }

    public void initGui()
    {
        pane = getContentPane();

        //==========================================================
        // menu bar
        //==========================================================
        XMLMenuBuilder builder = new XMLMenuBuilder(this, actions);
        setJMenuBar(builder.buildFrom("resources/menu.xml"));

        //==========================================================
        // tool bar
        //==========================================================
        XMLToolbarBuilder tbb = new XMLToolbarBuilder(this, actions);
        JToolBar tb = tbb.buildFrom("resources/menu.xml");
        pane.add(tb, BorderLayout.NORTH);

        //==========================================================
        //        layout
        //==========================================================
        tabPane.setSelectedIndex(0);    // select tab CAN
        pane.add(tabPane, BorderLayout.CENTER);

        pane.add(statusBar.panel, BorderLayout.SOUTH);
        enableControls();

        //==========================================================
        //        btSend listenner
        //==========================================================
        btSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CANDtgMsg msg = new CANDtgMsg();
                msg.id = FString.toInt(edCanID.getText());
                for(msg.length=0; msg.length<CANDtgMsg.DATA_LEN_MAX; ) {
                    String s = edCanData[msg.length].getText().trim();
                    if(s.length() == 0) break;
                    msg.data[msg.length++] = (byte) FString.toInt(s, 10);
                }
                if(cbxCanFlagRTR.isSelected()) msg.flags |= CANDtgMsg.MSG_RTR;
                if(cbxCanFlagEXT.isSelected()) msg.flags |= CANDtgMsg.MSG_EXT;
                if(cbxCanFlagOVR.isSelected()) msg.flags |= CANDtgMsg.MSG_OVR;
                if(cbxCanFlagLOCAL.isSelected()) msg.flags |= CANDtgMsg.MSG_LOCAL;
                txtLog.append("SENDING:\t" + msg);
                canConn.send(msg);
            }
        });

        btClearLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtLog.setText("");
            }
        });

        //==========================================================
        //        cbxShowRoughMessages listenner
        //==========================================================
        cbxShowRoughMessages.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JCheckBox cb = (JCheckBox)e.getSource();
                ServiceSetRawMsgParamsRequest rq = new ServiceSetRawMsgParamsRequest();
                boolean state = cb.isSelected();
                // one click on checkbox generate 5 listenner calls
                if(state != cbxShowRoughMessages_prev_state) {
                    cbxShowRoughMessages_prev_state = state;
                    if(state == true) {
                        rq.command = ServiceSetRawMsgParamsRequest.ADD;
                        rq.id = FString.toInt(edRawMessagesId.getText(), 16);
                        rq.mask = FString.toInt(edRawMessagesMask.getText(), 16);
                        cb.setBackground(Color.ORANGE);
                    }
                    else {
                        rq.command = ServiceSetRawMsgParamsRequest.REMOVE_ALL;
                    }
                    //txtLog.append("event " + cb.isSelected() + "\n");
                    txtLog.append("SENDING:\t" + rq);
                    canConn.send(rq);
                }
            }
        });

        //==========================================================
        //        JTextArea logs listenners
        //==========================================================
        int logsize = FString.toInt(xmlConfig.getValue("/logging/logscreensize", "1000000"));
        ((AbstractDocument)txtLog.getDocument()).setDocumentFilter(new LogTextAreaDocumentFilter(logsize));

        //==========================================================
        //        JFrame listenners
        //==========================================================
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actions.get("Quit").actionPerformed(null);
            }
        });
    }

    private void refreshForm()
    {
        boolean connected  = (canConn.getSocket() != null);
        XMLAction act;
        act = (XMLAction)actions.get("Connect");
        act.setEnabled(!connected);
        act = (XMLAction)actions.get("Disconnect");
        act.setEnabled(connected);
        if(!connected) {
            statusBar.lbl1.setText("disconnected");
        }
        else {
            statusBar.lbl1.setText(canConn.getSocket().toString());
        }
        statusBar.lbl2.setText("");
        statusBar.lbl3.setText("");
    }

    public void setTabLabel(int tabix, String lbl)
    {
        tabPane.setTitleAt(tabix, lbl);
    }

    private void enableControls()
    {
    }

    public void init()
    {
        canConn.setGuiUpdate(this);
//        canConn.connect(serverAddr, 0);

        Thread t = new Thread(this);
        t.start();
    }

    public void cleanUp()
    {
//        if(canReaderThread != null)
//            try {canReaderThread.join();} catch(InterruptedException e) {}
        disConnect();
        saveConfig();
    }

    /**
     * sends msg to the socket if it is opened
     */
    void sendMessage(Object o)
    {
        if(canConn == null) return;
        if(!canConn.connected()) return;
        canConn.send(o);
    }

    private int msgCount = 0;

    /**
     * code refreshing GUI controls from variables set by other thread
     * mainly CanMonClient thread
     */
    public void run()
    {
        // read all received messages if any
        while(true) {
            Object o = canConn.readQueue.remove(RoundQueue.NO_BLOCKING);
            if(o == null) break;
            FLog.log("CanMonitor", FLog.LOG_DEB, "received object " + o);

            if(o instanceof CANDtgMsg) {
                msgCount++;
                if(cbxShowRoughMessages.isSelected()) txtLog.append("RECEIVE[" + msgCount + "]:\t" + o);
            }
            else if(o instanceof ServiceSetRawMsgParamsConfirm) {
                cbxShowRoughMessages.setBackground(lblRawId.getBackground());
                ServiceSetRawMsgParamsConfirm spc = (ServiceSetRawMsgParamsConfirm)o;
                if(spc.code != ServiceSetRawMsgParamsConfirm.OK) {
                    ErrorMsg.show(tabPane, spc.errmsg);
                }
            }
            else {
                // scan all CANopen devices
                for(int i=1; i<tabPane.getTabCount(); i++) {
                    CANopenDevicePanel p =  (CANopenDevicePanel)tabPane.getComponentAt(i);
                    if(p.tasteObject(o)) break;
                }
            }
        }
        // refresh form
        refreshForm();
        //FLog.log(getClass().getName(), FLog.LOG_TRASH, "run(): GUI updated");
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$()
    {
        final JTabbedPane _1;
        _1 = new JTabbedPane();
        tabPane = _1;
        _1.setTabLayoutPolicy(0);
        _1.setTabPlacement(1);
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(3, 3, 0, 0), 3, -1));
        _1.addTab("CAN", _2);
        final JScrollPane _3;
        _3 = new JScrollPane();
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTextArea _4;
        _4 = new JTextArea();
        txtLog = _4;
        _3.setViewportView(_4);
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 11, new Insets(0, 0, 0, 0), 5, 0));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 0, 0, null, null, null));
        final JTextField _7;
        _7 = new JTextField();
        edCanID = _7;
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 1, 6, 6, new Dimension(50, -1), null, null));
        final JTextField _8;
        _8 = new JTextField();
        edCanData0 = _8;
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 6, 0, null, null, null));
        final JTextField _9;
        _9 = new JTextField();
        edCanData4 = _9;
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _10;
        _10 = new JTextField();
        edCanData6 = _10;
        _6.add(_10, new com.intellij.uiDesigner.core.GridConstraints(1, 7, 1, 1, 8, 1, 6, 0, null, null, null));
        final JLabel _11;
        _11 = new JLabel();
        _11.setText("ID");
        _11.setIconTextGap(0);
        _6.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _12;
        _12 = new JLabel();
        _12.setText("byte[0]");
        _12.setIconTextGap(0);
        _6.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _13;
        _13 = new JLabel();
        _13.setText("byte[4]");
        _13.setIconTextGap(0);
        _6.add(_13, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _14;
        _14 = new JLabel();
        _14.setText("byte[6]");
        _14.setIconTextGap(0);
        _6.add(_14, new com.intellij.uiDesigner.core.GridConstraints(0, 7, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _15;
        _15 = new JLabel();
        _15.setText("byte[2]");
        _15.setIconTextGap(0);
        _6.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _16;
        _16 = new JTextField();
        edCanData2 = _16;
        _6.add(_16, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 8, 1, 6, 0, null, null, null));
        final JLabel _17;
        _17 = new JLabel();
        _17.setText("byte[1]");
        _17.setIconTextGap(0);
        _6.add(_17, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _18;
        _18 = new JLabel();
        _18.setText("byte[3]");
        _18.setIconTextGap(0);
        _6.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _19;
        _19 = new JLabel();
        _19.setText("byte[5]");
        _19.setIconTextGap(0);
        _6.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 6, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _20;
        _20 = new JLabel();
        _20.setText("byte[7]");
        _20.setIconTextGap(0);
        _6.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 8, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _21;
        _21 = new JTextField();
        edCanData1 = _21;
        _6.add(_21, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _22;
        _22 = new JTextField();
        edCanData3 = _22;
        _6.add(_22, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _23;
        _23 = new JTextField();
        edCanData5 = _23;
        _6.add(_23, new com.intellij.uiDesigner.core.GridConstraints(1, 6, 1, 1, 8, 1, 6, 0, null, null, null));
        final JTextField _24;
        _24 = new JTextField();
        edCanData7 = _24;
        _6.add(_24, new com.intellij.uiDesigner.core.GridConstraints(1, 8, 1, 1, 8, 1, 6, 0, null, null, null));
        final JCheckBox _25;
        _25 = new JCheckBox();
        cbxCanFlagRTR = _25;
        _25.setText("RTR");
        _6.add(_25, new com.intellij.uiDesigner.core.GridConstraints(0, 9, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _26;
        _26 = new JCheckBox();
        cbxCanFlagEXT = _26;
        _26.setText("EXT");
        _6.add(_26, new com.intellij.uiDesigner.core.GridConstraints(1, 9, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _27;
        _27 = new JCheckBox();
        cbxCanFlagOVR = _27;
        _27.setText("OVR");
        _6.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 10, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _28;
        _28 = new JCheckBox();
        cbxCanFlagLOCAL = _28;
        _28.setText("LOCAL");
        _6.add(_28, new com.intellij.uiDesigner.core.GridConstraints(1, 10, 1, 1, 8, 0, 3, 0, null, null, null));
        final JButton _29;
        _29 = new JButton();
        btSend = _29;
        _29.setText("Send");
        _29.setMnemonic(83);
        _29.setDisplayedMnemonicIndex(0);
        _5.add(_29, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _30;
        _30 = new com.intellij.uiDesigner.core.Spacer();
        _5.add(_30, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 6, 1, null, null, null));
        final JPanel _31;
        _31 = new JPanel();
        lblRawId = _31;
        _31.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 11, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_31, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JCheckBox _32;
        _32 = new JCheckBox();
        cbxShowRoughMessages = _32;
        _32.setText("Show rough messages");
        _32.setMnemonic(83);
        _32.setDisplayedMnemonicIndex(0);
        _32.setSelected(false);
        _32.setAutoscrolls(false);
        _32.setEnabled(true);
        _32.setContentAreaFilled(true);
        _32.setBorderPaintedFlat(false);
        _32.setBorderPainted(false);
        _31.add(_32, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JButton _33;
        _33 = new JButton();
        btClearLog = _33;
        _33.setText("Clear log");
        _33.setVerticalAlignment(0);
        _31.add(_33, new com.intellij.uiDesigner.core.GridConstraints(0, 10, 1, 1, 1, 1, 3, 1, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _34;
        _34 = new com.intellij.uiDesigner.core.Spacer();
        _31.add(_34, new com.intellij.uiDesigner.core.GridConstraints(0, 9, 1, 1, 0, 1, 6, 1, null, null, null));
        final JTextField _35;
        _35 = new JTextField();
        edRawMessagesId = _35;
        _31.add(_35, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 8, 1, 2, 0, null, new Dimension(70, -1), null));
        final JLabel _36;
        _36 = new JLabel();
        _36.setText("ID");
        _31.add(_36, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _37;
        _37 = new com.intellij.uiDesigner.core.Spacer();
        _31.add(_37, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 2, 1, new Dimension(30, -1), null, null));
        final JLabel _38;
        _38 = new JLabel();
        _38.setText("Mask");
        _31.add(_38, new com.intellij.uiDesigner.core.GridConstraints(0, 6, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _39;
        _39 = new JTextField();
        edRawMessagesMask = _39;
        _31.add(_39, new com.intellij.uiDesigner.core.GridConstraints(0, 7, 1, 1, 8, 1, 2, 0, null, new Dimension(70, -1), null));
        final JLabel _40;
        _40 = new JLabel();
        _40.setText("hex");
        _31.add(_40, new com.intellij.uiDesigner.core.GridConstraints(0, 8, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _41;
        _41 = new JLabel();
        _41.setText("hex");
        _31.add(_41, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _42;
        _42 = new com.intellij.uiDesigner.core.Spacer();
        _31.add(_42, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 0, 1, 2, 1, new Dimension(30, -1), null, null));
    }

}
