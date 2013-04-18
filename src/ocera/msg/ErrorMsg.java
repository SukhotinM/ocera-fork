package ocera.msg;

import javax.swing.*;
import java.awt.*;

/**
 * @author vacek
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

public class ErrorMsg
{
	/**
	 * Shows an message
	 * @param	msg String the <code>String</code> that contains the message
	 */
	public static void show(String msg)
    {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Shows an message
	 * @param	msg String the <code>String</code> that contains the message
	 */
	public static void show(Component parent, String msg)
    {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}
}

