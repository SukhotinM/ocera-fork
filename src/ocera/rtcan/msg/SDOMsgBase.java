package ocera.rtcan.msg;

import org.flib.FString;

/**
 * ocera.rtcan.monitor.SDOMsgBase
 * <p/>
 * (C) Copyright 2:04:17 PM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class SDOMsgBase
{
    public int srvcliCobId = 0;
    public int clisrvCobId = 0;
    public int node = 0;
    public int index = 0;
    public int subindex = 0;

    public String toString()
    {
        String s = "";
        s += "node: " + node + "\n";
        s += "object: " + FString.int2HexStr(index, 4) + ":" + FString.int2HexStr(subindex, 2) + "\n";
        s += "srvcli cobid: " + FString.int2HexStr(srvcliCobId) + "\n";
        s += "clisrv cobid: " + FString.int2HexStr(clisrvCobId) + "\n";
        return s;
    }
}
