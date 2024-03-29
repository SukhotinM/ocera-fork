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

import javax.swing.*;
import java.util.Properties;
import java.net.URL;
import java.io.IOException;

public abstract class XMLWidgetBuilder
{
    /**
    * The properties bundle that the builder will reference to for
    * things like getReferencedLabel.
    */
    protected Properties properties = new Properties();

    /**
    * Reference point into the CLASSPATH for locating the XML file.
    */
    protected Class ref;

    /**
    * Set the reference point for URL location.
    *
    * @param ref the reference point for local urls to be loaded from.
    */
    public void setReference(Class ref) {
        this.ref = ref;
    }

    /**
    * Empty icon to keep space in menus withouth icon
    */
    static public ImageIcon icoEmpty = loadEmptyIcon();

    static ImageIcon loadEmptyIcon()
    {
        Class c;
        try {
            c = Class.forName("org.jx.xmlgui.XMLActionMapBuilder");
        } catch (ClassNotFoundException e) {
            c = null;
        }
        if(c == null) {
            //FLog.log("XMLActionMapBuilder", FLog.LOG_DEB, "loadEmptyIcon() - class is null");
            return null;
        }
        //FLog.log("XMLActionMapBuilder", FLog.LOG_DEB, "loadEmptyIcon() - class: " + c.toString());

        URL url = c.getResource("resources/empty18.png");
        ImageIcon ico = null;
        if(url != null) ico = new ImageIcon(url);
        FLog.log("XMLActionMapBuilder", FLog.LOG_DEB, "loadEmptyIcon() - icon: " + ico);
        return ico;
    }

    /**
     * Set the element as the item containing configuration for the
     * builder. This would usually be the link tag in the headMagic.
     *
     * @param config the element containing configuration data
     */
    public void setConfiguration(org.jdom.Element config)
    {
        if(config == null) return;
        try {
    //      Class local = ((ref == null) ? getClass() : ref);
    //      Class local=getClass();
            // get the string properties
            if (config.getAttributeValue("href", "").length() != 0
                    && config.getAttributeValue("role", "").equals("stringprops")
                    && config.getName().equals("link")) {
                // this should never return null
                URL linkURL;
                String resource = config.getAttributeValue("href", "NOT_SET");
                linkURL = ref.getResource(resource);
                //FLog.log("XMLWidgetBuilder", FLog.LOG_DEB, "ref package: '" + ref.getPackage() + "'");
                //FLog.log("XMLWidgetBuilder", FLog.LOG_DEB, "setConfiguration() - read: '" + resource + "'");
                if (linkURL == null) {
                    FLog.log("XMLWidgetBuilder", FLog.LOG_ERR, "setConfiguration() - properties '" + resource + "' not found");
                } else {
                    FLog.log("XMLWidgetBuilder", FLog.LOG_DEB, "setConfiguration() - loading properties: '" + linkURL.toExternalForm() + "'");
                    properties.load(linkURL.openStream());
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /**
     * Get a label referenced by the string properties file.
     *
     * @param current the element to process
     * @param attr    the attribute to look up
     * @return the string as post lookup, or the string pre lookup if
     *         the lookup failed
     */
    public String getReferencedLabel(org.jdom.Element current, String attr)
    {
        String label = current.getAttributeValue(attr, attr + " NOT_FOUND");
//      FLog.log("XMLWidgetBuilder", FLog.LOG_DEB, "getReferencedLabel() - looking for attr: " + attr + " found: " + label);
        // if it starts with a '$' we crossreference to properties
        if (label != null && label.length() > 0 && label.charAt(0) == '$') {
            String key = label.substring(1);
            label = properties.getProperty(key, label);
        }

        return label;
    }
}
