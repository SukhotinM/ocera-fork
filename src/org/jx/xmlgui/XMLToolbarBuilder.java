package org.jx.xmlgui;

import org.flib.FLog;

import javax.swing.*;
import java.net.URL;
import java.io.IOException;
import java.util.Iterator;
import java.awt.*;

import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: Apr 29, 2004
 * Time: 3:49:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLToolbarBuilder extends XMLWidgetBuilder
{
    private ActionMap actions = null;

    /**
     * Build a menu builder which operates on XML formatted data.
     *
     * @param frame reference point for properties location
     */
    public XMLToolbarBuilder(JFrame frame, ActionMap actions)
    {
        ref = frame.getClass();
        this.actions = actions;
    }

    /**
     * Read the input stream and build a ActionMap from it
     *
     * @param url location of menu file
     */
    public JToolBar buildFrom(URL url)
    {
        if(url == null)
            FLog.log("XMLToolbarBuilder", FLog.LOG_ERR, "buildFrom() - URL is NULL");
        FLog.log("XMLToolbarBuilder", FLog.LOG_DEB, "buildFrom() - build from URL: " + url.toString());
        JToolBar tb = null;
        SAXBuilder builder = new SAXBuilder();
        try {
            org.jdom.Document doc = null;
            doc = builder.build(url);
            org.jdom.Element root = doc.getRootElement();

            // skip to the body
            tb = buildFrom(root.getChild("body").getChild("toolbar"));
        }
        catch (JDOMException e) {
          System.err.println(url + " is not well-formed.");
          System.err.println(e.getMessage());
        }
        catch (IOException e) {
          System.err.println("Could not check " + url);
          System.err.println(" because " + e.getMessage());
        }
        return tb;
    }

    public JToolBar buildFrom(String resource_file_name)
    {
        FLog.log("XMLToolbarBuilder", FLog.LOG_DEB, "buildFrom() - open URL '" + resource_file_name + "'");
        URL url = ref.getResource(resource_file_name);
        return buildFrom(url);
    }

    public JToolBar buildFrom(Element element)
    {
        if(!element.getName().equals("toolbar")) {
            FLog.log("XMLActionMapBuilder", FLog.LOG_ERR, "buildFrom() - element name should be <toolbar>");
        }
        JToolBar tb = new JToolBar();
        tb.setRollover(true);
        java.util.List lst = element.getContent();
        Iterator it = lst.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if(o instanceof Element) {
                Component comp = buildComponent((Element)o);
                if(comp != null) tb.add(comp);
            }
        }

        return tb;
    }

    /**
    * Build a toolbar component.
    * @param el the element that describes the JMenuItem
    * @return the built component
    */
    protected Component buildComponent(Element el)
    {
        Component comp = null;
        String s = el.getName();

        if(s.equals("separator")) {
            comp = new JSeparator(SwingConstants.VERTICAL);
        }
        else if(s.equals("button")) {
            String actionid = el.getAttributeValue("action_id");
            if(actionid == null) {
                FLog.log("XMLToolbarBuilder", FLog.LOG_ERR, "buildComponent() - missing action_id attribute");
                return comp;
            }
            Action act = actions.get(actionid);
            if(act != null) {
                JButton bt = new JButton(act);
                ImageIcon ico = (ImageIcon)act.getValue(Action.SMALL_ICON);
                if(ico != null && ico.getImageLoadStatus() == MediaTracker.COMPLETE) {
                    //FLog.log("XMLToolbarBuilder", FLog.LOG_ERR, "buildComponent() - ICON: " + ico.getImageLoadStatus());
                    bt.setText("");
                }
                bt.setMargin(new Insets(0, 0, 0, 0));
                comp = bt;
            }
        }
        else {
            FLog.log("XMLToolbarBuilder", FLog.LOG_ERR, "buildComponent() - element name unsupported: " + s);
        }
        return comp;
    }
}
