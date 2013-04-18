package ocera.rtcan.msg;

import org.flib.FString;

/**
 * ocera.rtcan.monitor.SDOUploadRequestMsg
 * <p/>
 * (C) Copyright 1:54:04 PM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class SDOUploadConfirmMsg extends SDOConfirmMsg
{
    public byte[] data = new byte[0];

    public String toString()
    {
        String s = "UPLOAD ";
        s += super.toString();
        if(type == SDOConfirmMsg.MSG_OK) {
            s += "data: [";
            for(int i = 0; i < data.length; i++) {
                if(i > 0) s += " ";
                s += FString.byte2Hex(data[i]);
            }
            s += "]\n";
        }
        return s;
    }
}
