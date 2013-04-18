package ocera.rtcan.msg;

import org.flib.FString;

/**
 * ocera.rtcan.monitor.SDODownloadRequestMsg
 * <p/>
 * (C) Copyright 10:56:53 AM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class SDODownloadRequestMsg extends SDOMsgBase
{
    public byte[] data = new byte[0];

    public String toString()
    {
        String s = "SDO download request - ";
        s += super.toString();
        s += "data: [";
        for(int i = 0; i < data.length; i++) {
            if(i > 0) s += " ";
            s += FString.int2HexStr(data[i], 2);
        }
        s += "]\n";
        return s;
    }
}