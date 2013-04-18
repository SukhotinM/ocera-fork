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
public class ServiceSetRawMsgParamsRequest extends ServiceMsgBase
{
    public static final int NONE = 0;
    public static final int ADD = 1;
    public static final int REMOVE_ALL = 3;

    public int command = NONE;
    public int id = 0;
    public int mask = 0;

    public String toString()
    {
        String s = "Service set CAN raw message parameters:\n";
        s += "\tcommand: " + command + "\n";
        s += "\tid: " + id + "\n";
        s += "\tmask: " + mask + "\n";
        s += super.toString();
        return s;
    }
}
