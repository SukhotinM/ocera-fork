package ocera.rtcan.msg;

import org.flib.FString;

/**
 * ocera.rtcan.SDOConfirmMsg
 * <p/>
 * (C) Copyright 9:03:55 AM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class SDOConfirmMsg extends SDOMsgBase
{
    public static final int MSG_OK = 0;
    public static final int MSG_ERROR = 1;
    public static final int MSG_ABORT = 2;

    public int type = MSG_OK;
    public int code = 0;
    public String errmsg = "";

    public String toString()
    {
        String s = "SDO confirmation - ";
        s += super.toString();
        if(type == SDOConfirmMsg.MSG_ABORT) {
            s += "ABORT: " + code + " - '" + errmsg + "'\n";
        }
        else if(type == SDOConfirmMsg.MSG_ERROR) {
            s += "ERROR: " + code + " - '" + errmsg + "'\n";
        }
        return s;
    }
}
