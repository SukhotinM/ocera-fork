package org.jx.xmlgui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: Apr 29, 2004
 * Time: 4:17:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLAction extends AbstractAction
{
    public ActionListener actionListener = null;

    public void actionPerformed(ActionEvent e)
    {
        if(actionListener != null) actionListener.actionPerformed(e);
    }
}
