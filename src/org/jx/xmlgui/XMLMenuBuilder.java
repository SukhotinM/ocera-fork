/* -*- Mode: java; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is the Grendel mail/news client.
 *
 * The Initial Developer of the Original Code is Giao Nguyen
 * <grail@cafebabe.org>.  Portions created by Giao Nguyen are
 * Copyright (C) 1999 Giao Nguyen. All
 * Rights Reserved.
 *
 * Contributor(s): Morgan Schweers <morgan@vixen.com>
 */

package org.jx.xmlgui;

import org.flib.FLog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
//import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;

//import org.jx.ui.*;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Element;

import javax.swing.*;

/**
 * Build a menu bar from an XML data source. This builder supports:
 * <UL>
 * <LI>Text label cross referencing to a properties file.
 * <LI>Action lookups.
 * </UL>
 */
public class XMLMenuBuilder extends XMLWidgetBuilder
{
  static final String action_id_attr = "action_id";
  static final String menu_tag = "menu";
  static final String label_attr = "caption";
  static final String separator_attr = "separator";
  static final String menuitem_tag = "menuitem";
  static final String checkbox_attr = "checkbox";
  static final String radio_attr = "radio";
  static final String group_attr = "group";
  static final String mnemonic_attr = "mnemonic";
  static final String accel_attr = "accelerator";
  static final String type_attr = "type";

    /**
     * menu nesting counter
     * menus on menu bar do not use icons
     */
    static int nestCnt = 0;

  /**
   * The button group indexed by its name.
   * Groups of radio buttons
   */
  protected Hashtable button_group = new Hashtable(2);

  ActionMap actions;  // actions used in current build process
  //MenuBarCtrl component;


  /**
   * Build a menu builder which operates on XML formatted data.
   * 
   * @param frame reference point for properties location
   * @param actions array of UIAction objects to map to
   */
  public XMLMenuBuilder(JFrame frame, ActionMap actions)
  {
      ref = frame.getClass();
      this.actions = actions;
  }

  /**
   * Read the input stream and build a menubar from it
   * 
   * @param url location of menu file
   */
    public JMenuBar buildFrom(URL url)
    {
      if(url == null)
        FLog.log("XMLMenuBuilder", FLog.LOG_ERR, "buildFrom() - URL is NULL");
      FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildFrom() - build from URL: " + url.toString());
      JMenuBar menubar = null;
      SAXBuilder builder = new SAXBuilder();
      try {
          org.jdom.Document doc = null;
          doc = builder.build(url);
          org.jdom.Element root = doc.getRootElement(), el;

          // get the <headMagic><link> tag for this file
          el = root.getChild("head").getChild("link");
          // set the configuration contained in this node
          setConfiguration(el);

          // skip to the body
          menubar = buildFrom(root.getChild("body").getChild("menubar"));
      }
      catch (JDOMException e) {
        System.err.println(url + " is not well-formed.");
        System.err.println(e.getMessage());
      }
      catch (IOException e) {
        System.err.println("Could not check " + url);
        System.err.println(" because " + e.getMessage());
      }
      return menubar;
    }

    public JMenuBar buildFrom(String resource_file_name)
    {
        FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildFrom() - open URL '" + resource_file_name + "'");
        URL url = ref.getResource(resource_file_name);
        return buildFrom(url);
    }

    public JMenuBar buildFrom(Element element)
    {
        if(!element.getName().equals("menubar")) {
//          throw new Exception("XMLMenuBuilder.buildFrom() - element name should be <menubar>");
            FLog.log("XMLMenuBuilder", FLog.LOG_ERR, "buildFrom() - element name should be <menubar>");
        }
        JMenuBar menubar = new JMenuBar();
        java.util.List lst = element.getContent();
        Iterator it = lst.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if(o instanceof Element) {
                JMenu menu = buildMenu((Element)o);
                menubar.add(menu);
                //component.addItemByName(menu.getActionCommand(), menu);
            }
        }

        return menubar;
    }

    public JMenu buildMenu(org.jdom.Element el)
    {
        nestCnt++;
        JMenu menu = new JMenu();
        finishMenuItem(menu, el);
        if(nestCnt == 1) menu.setIcon(null); // menus on menu bar don't have icons

        java.util.List lst = el.getContent();
        Iterator it = lst.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if(o instanceof Element) {
                JComponent comp = buildComponent((Element)o);
    //            FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildMenu() - adding menu item: " + comp.getClass().toString());
                menu.add(comp);
            }
        }
        nestCnt--;
        return menu;
    }

    /**
    * Build the component at the current XML element and add to the parent.
    * @param current the current element
    * @return built component
    */
    protected JComponent buildComponent(Element current)
    {
        String tag = current.getName();
//        FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildMenuItem() - element: " + current.toString()
               // + " label: " + getReferencedLabel(current, "label"));
        JComponent comp = null;

        // menu tag
        if(tag.equals(menu_tag)) {
            comp = buildMenu(current);
        }
        else if(tag.equals("separator")) {
            comp = buildSeparator(current);
        }
        else if(tag.equals(menuitem_tag)) { // menuitem tag
            String type = current.getAttributeValue(type_attr, "");
            // which type of menuitem?
            if (type.equals("")) {
                // no type ? it's a regular menuitem
                comp = buildMenuItem(current);
            }
            else if (type.equals(checkbox_attr)) { // checkboxes
                comp = buildCheckBoxMenuItem(current);
            }
            else if (type.equals(radio_attr)) { // radio
                comp = buildRadioMenuItem(current);
            }
        }

        comp.setFont(new Font("Helvetica", Font.PLAIN, 12));
        return comp;
    }
  
  /**
   * Build a JRadioMenuItem
   * @param current the element that describes the JRadioMenuItem
   * @return the built component
   */
  protected JRadioButtonMenuItem buildRadioMenuItem(Element current)
  {
    String group = current.getAttributeValue(group_attr, "");
    ButtonGroup bg;
    JRadioButtonMenuItem comp = new JRadioButtonMenuItem();
    finishMenuItem(comp, current);
    
    // do we add to a button group?
    if (button_group.containsKey(group)) {
      bg = (ButtonGroup)button_group.get(group);
    } else {
      bg = new ButtonGroup();
      button_group.put(group, bg);
    }
    bg.add((JRadioButtonMenuItem)comp);
    
    comp.setFont(new Font("Helvetica", Font.PLAIN, 12));
    return comp;
  }

  /**
   * Build a JCheckBoxMenuItem.
   * @param current the element that describes the JCheckBoxMenuItem
   * @return the built component
   */
  protected JCheckBoxMenuItem buildCheckBoxMenuItem(Element current) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem();
    finishMenuItem(item, current);
    item.setFont(new Font("Helvetica", Font.PLAIN, 12));
    return item;
  }

  /**
   * Build a JSeparator.
   * @param current the element that describes the JSeparator
   * @return the built component
   */
  protected JSeparator buildSeparator(Element current) {
    return new JSeparator();
  }

    /**
    * Build a JMenuItem.
    * @param el the element that describes the JMenuItem
    * @return the built component
    */
    protected JMenuItem buildMenuItem(Element el)
    {
        JMenuItem item = null;
        Action action = null;
        String act_id = el.getAttributeValue(action_id_attr);
        if(act_id != null) {
            if ((action = (Action)actions.get(act_id)) != null) {
                item = new JMenuItem(action);
            }
            else {
                item = new JMenuItem(act_id + " ACTION_NOT_FOUND");
                item.setEnabled(false);
            }
        }
        if(item == null) {
            item = new JMenuItem();

            finishMenuItem(item, el);
            //item.setFont(new Font("Helvetica", Font.PLAIN, 12));
        }
        if(item.getIcon() == null) item.setIcon(XMLWidgetBuilder.icoEmpty);
        item.setMargin(new Insets(0, 0, 0, 0));
        item.setIconTextGap(0);
        //FLog.log("XMLMenuBuilder", FLog.LOG_DEB, "buildMenuItem() - new menu item label: " + item.getText() + " icon: " + item.getIcon());
        return item;
    }

    private void finishMenuItem(JMenuItem item, Element el)
    {
        String s = getReferencedLabel(el, label_attr);
        if(s.length() > 0) {
            item.setText(s);
            item.setName(s);
        }
        // set mnemonic
        s = el.getAttributeValue("mnemonic_attr", "");
        if(s.length() > 0) {
            s = s.toUpperCase();
            char c = s.charAt(0);
            int offset = c - 'A';
            item.setMnemonic(KeyEvent.VK_A + offset);
        }
        // set icon
        ImageIcon ico = null;
        s = el.getAttributeValue("icon");
        URL url = null;
        if(s != null) url = ref.getResource(s);
        if(url != null) ico = new ImageIcon(url);
        if(ico == null || ico.getImageLoadStatus() != MediaTracker.COMPLETE) ico = XMLWidgetBuilder.icoEmpty;
        if(ico != null) item.setIcon(ico);
    }

    public static void main(String[] args) throws Exception
    {
        FLog.logTreshold = FLog.LOG_ERR;
        javax.swing.JFrame frame = new javax.swing.JFrame("Foo bar");
        ActionMap actions = new ActionMap();
        XMLMenuBuilder builder = new XMLMenuBuilder(frame, actions);
        Action act = new AbstractAction("Quit") {
                    public void actionPerformed(ActionEvent e) {
                        System.gc();
                        System.exit(0);
                    }
                };
        actions.put("appExit", act);
        frame.setJMenuBar(builder.buildFrom("menu1.xml"));
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
