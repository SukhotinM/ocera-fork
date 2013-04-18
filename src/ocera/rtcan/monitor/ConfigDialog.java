/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: Jun 18, 2003
 * Time: 11:09:51 AM
 * To change this template use Options | File Templates.
 */
package ocera.rtcan.monitor;

import javax.swing.*;
import ocera.xmlconf.*;
import ocera.util.xmlconfig.XmlConfig;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.net.URL;

public class ConfigDialog extends JDialog
{
    protected XmlConfPanel xcPanel = null;

    ConfigDialog(XmlConfig xconf)
    {
        xcPanel = new XmlConfPanel();
        if(xconf == null) throw new NullPointerException("XML cofig is NULL");
        xcPanel.setConfig(xconf);
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(xcPanel, BorderLayout.CENTER);
        URL url = this.getClass().getResource("resources/ok.png");
        JButton btExit = new JButton("Save & Exit", new ImageIcon(url));
        btExit.setAlignmentX(1);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(btExit);
        c.add(p, BorderLayout.SOUTH);

        btExit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                xcPanel.saveEditedField();
                dispose();
            }
        });

        // this makes me the oportunity to save currently edited field before closing dialog
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE );
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                xcPanel.saveEditedField();
            }
        });

        setSize(500, 400);
//        pack();
    }
}
