/*
 * @(#)XmlConf.java	1.20 02/06/13
 */
package ocera.xmlconf;

//import ocera.util.FFileFilter;
//import ocera.util.FramedLayoutPanel;
//import ocera.util.StringParser;
//import ocera.util.xmlconfig.XmlConfig;
//import ocera.util.xmlconfig.XmlConfElement;
//import ocera.msg.InfoMsg;
import ocera.xmlconf.XmlConfPanel;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Properties;
//import java.util.Enumeration;
//import java.util.Iterator;
//import java.io.*;

import javax.swing.*;
//import javax.swing.border.Border;
//import javax.swing.event.TreeSelectionListener;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.tree.DefaultMutableTreeNode;
//import javax.swing.tree.DefaultTreeModel;
//import javax.swing.tree.TreePath;
//
//import org.jdom.Element;

/**
 * Sample application using the simple text editor component that
 * supports only one font.
 *
 * @author  Timothy Prinzing
 * @version 1.20 06/13/02
 */
public class XmlConf extends JFrame
{
    protected XmlConfPanel xconfPanel = null;

    XmlConf()
    {
        super();

        // Force SwingSet to come up in the Cross Platform L&F
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            // If you want the System L&F instead, comment out the above line and
            // uncomment the following:
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exc) {
            System.err.println("Error loading L&F: " + exc);
        }
    }

    public static void main(String[] args)
    {
        XmlConf app = new XmlConf();

        app.setTitle("Xml Configurator");
        app.setBackground(Color.lightGray);
        //app.getContentPane().setLayout(new GridLayout());
        XmlConfPanel xconfpan = new XmlConfPanel();
        app.xconfPanel = xconfpan;
        app.initGUI();

        if(args.length > 0) {
            xconfpan.openConfigFile(args[0]);
        }

        app.pack();
        app.setSize(600, 400);
        app.show();

        Properties prop = System.getProperties();
        for(Enumeration e = prop.keys(); e.hasMoreElements() ;) {
            String key = e.nextElement().toString();
            System.out.println(key + "=" + prop.getProperty(key));
        }


    }

    protected void initGUI()
    {
        setTitle("XML Configurator");

        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(createMenu(), BorderLayout.NORTH);
        p1.add(xconfPanel, BorderLayout.CENTER);
        getContentPane().add(p1);

        // application closing
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE );
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.out.println("collecting garbage & quit");
                System.gc();
                System.exit(0);
            }
        });

    }

    protected JMenuBar createMenu()
    {
        JMenuBar mb = new JMenuBar();

        String[] menuKeys = {"file"};
        String[] menuLabels = {"File"};
        for (int i = 0; i < menuKeys.length; i++) {
            JMenu m = createMenu(menuKeys[i], menuLabels[i]);
            if (m != null) {
                mb.add(m);
            }
        }
        return mb;
    }

    protected JMenu createMenu(String key, String label)
    {
        String[][] itemKeys = {{"file", "new", "open", "save", "saveAs", "-", "quit"}};
        JMenu menu = new JMenu(label);
        for (int j = 0; j < itemKeys.length; j++) {
            if (itemKeys[j][0].equals(key)) {
                for (int i = 1; i < itemKeys[j].length; i++) {
                    if (itemKeys[j][i].equals("-")) {
                        menu.addSeparator();
                    }
                    else {
                        Action act = xconfPanel.getAction(itemKeys[j][i]);
                        JMenuItem mi = null;
                        if(act != null) {
                            mi = new JMenuItem(act);
                        }
                        else {
                            mi = new JMenuItem(itemKeys[j][i]);
                            mi.setEnabled(false);
                        }
                        menu.add(mi);
                    }
                }
            }
            break;
        }
        return menu;
    }

}
