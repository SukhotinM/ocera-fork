package ocera.rtcan.msg;


/**
 * ocera.rtcan.monitor.SDODownloadConfirmMsg
 * <p/>
 * (C) Copyright 10:58:32 AM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class SDODownloadConfirmMsg extends SDOConfirmMsg
{
    public String toString()
    {
        String s = "DOWNLOAD ";
        s += super.toString();
        if(type == SDOConfirmMsg.MSG_OK) {
            s += "OK";
        }
        s += "\n";
        return s;
    }
}
