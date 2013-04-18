package org.jx.xmlgui;

import org.flib.FLog;

import javax.swing.*;
import java.net.URL;
import java.io.IOException;
import java.util.Iterator;
import java.awt.event.KeyEvent;
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
public class XMLActionMapBuilder extends XMLWidgetBuilder
{
    /**
     * Build a menu builder which operates on XML formatted data.
     *
     * @param frame reference point for properties location
     */
    public XMLActionMapBuilder(JFrame frame)
    {
        ref = frame.getClass();
    }

    /**
     * Read the input stream and build a ActionMap from it
     *
     * @param url location of menu file
     */
    public ActionMap buildFrom(URL url)
    {
        if(url == null)
            FLog.log("XMLActionMapBuilder", FLog.LOG_ERR, "buildFrom() - URL is NULL");
        FLog.log("XMLActionMapBuilder", FLog.LOG_DEB, "buildFrom() - build from URL: " + url.toString());
        ActionMap amap = null;
        SAXBuilder builder = new SAXBuilder();
        try {
            org.jdom.Document doc = null;
            doc = builder.build(url);
            org.jdom.Element root = doc.getRootElement();

            // skip to the body
            amap = buildFrom(root.getChild("body").getChild("actionmap"));
        }
        catch (JDOMException e) {
          System.err.println(url + " is not well-formed.");
          System.err.println(e.getMessage());
        }
        catch (IOException e) {
          System.err.println("Could not check " + url);
          System.err.println(" because " + e.getMessage());
        }
        return amap;
    }

    public ActionMap buildFrom(String resource_file_name)
    {
        FLog.log("XMLActionMapBuilder", FLog.LOG_DEB, "buildFrom() - open URL '" + resource_file_name + "'");
        URL url = ref.getResource(resource_file_name);
        return buildFrom(url);
    }

    public ActionMap buildFrom(Element element)
    {
        if(!element.getName().equals("actionmap")) {
            FLog.log("XMLActionMapBuilder", FLog.LOG_ERR, "buildFrom() - element name should be <actionmap>");
        }
        ActionMap amap = new ActionMap();
        java.util.List lst = element.getContent();
        Iterator it = lst.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if(o instanceof Element) {
                Action act = buildAction((Element)o);
                amap.put(act.getValue("id"), act);
            }
        }

        return amap;
    }

    /**
    * Build a JMenuItem.
    * @param el the element that describes the JMenuItem
    * @return the built XMLAction
    */
    protected XMLAction buildAction(Element el)
    {
        XMLAction act = null;
        if(!el.getName().equals("action")) {
            FLog.log("XMLActionMapBuilder", FLog.LOG_ERR, "buildAction() - element name should be <action>");
            return act;
        }
        String id = el.getAttributeValue("id");
        if(id == null) {
            FLog.log("XMLActionMapBuilder", FLog.LOG_ERR, "buildAction() - missing id attribute");
            return act;
        }
        act = new XMLAction();
        act.putValue("id", id);
        ImageIcon ico = null;
        URL url = ref.getResource(el.getAttributeValue("icon", ""));
        if(url != null) ico = new ImageIcon(url);
        if(ico.getImageLoadStatus() != MediaTracker.COMPLETE) ico = null;
        if(ico != null) act.putValue(Action.SMALL_ICON, ico);

        act.putValue(Action.NAME, el.getAttributeValue("caption", id));
        act.putValue(Action.SHORT_DESCRIPTION, el.getAttributeValue("tooltip", id));
        // set mnemonic
        String s = el.getAttributeValue("mnemonic", "");
        if(s.length() > 0) {
            s = s.toUpperCase();
            char c = s.charAt(0);
            int offset = c - 'A';
            act.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A + offset));
        }
        // set accelerator
        s = el.getAttributeValue("accelerator", "");
        if(s.length() > 0) {
            act.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(s));
        }

        return act;
    }
}
