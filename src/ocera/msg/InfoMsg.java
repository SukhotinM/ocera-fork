package ocera.msg;

/**
 * @author vacek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

import javax.swing.*;
import java.awt.*;

public class InfoMsg 
{
	private Component parent = null;

	public InfoMsg(Component parent) {this.parent = parent;}

	//-------------------------------------------------------------
	/**
	 * Shows an message
	 *
	 * @param	msg String
	 *				the <code>String</code> that contains the message
	 *
	 */
	public void show(String msg) {
        if(parent != null)
            JOptionPane.showMessageDialog(parent, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
	//-------------------------------------------------------------
}

