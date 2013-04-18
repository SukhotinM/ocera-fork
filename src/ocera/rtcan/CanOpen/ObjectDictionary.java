package ocera.rtcan.CanOpen;

import org.flib.FLog;
import org.flib.FString;

import java.util.Arrays;

/**
 * Class representin CANopen Object Dictionary
 */
public class ObjectDictionary
{
    protected ODNode[] od = null;
    public ODNode[] getOd()
    {
        return od;
    }

    public void setOd(ODNode[] od)
    {
        this.od = od;
    }

    /**
     * finds object in OD
     * @param index
     * @param subindex ignored if object does not have subindexes
     * @return object on index, subindex or null
     */
    public ODNode findObject(int index, int subindex)
    {
        ODNode ret = null;
        if(od == null) {
            FLog.log("ODNode.findObject(" + FString.int2HexStr(index) + ", " + FString.int2HexStr(subindex) + ")", FLog.LOG_ERR, "Object dictionary is empty.");
            return ret;
        }

        // find object
        ODNode odkey = new ODNode();
        odkey.index = index;
        odkey.subIndex = 0;
        // find index
        int ix = Arrays.binarySearch(od, odkey);
        if(ix < 0) {
            FLog.log("ODNode.findObject(" + FString.int2HexStr(index) + ", " + FString.int2HexStr(subindex) + ")", FLog.LOG_ERR, "index not found.");
            return null;
        }
        ret = od[ix];
        if(ret.subObjectCnt() == 0) return ret;
        ODNode subnodes[] = ret.subNodes;
        if(subnodes == null) {
            FLog.log("ODNode.findObject(" + FString.int2HexStr(index) + ", " + FString.int2HexStr(subindex) + ")", FLog.LOG_ERR, "subnodes == NULL.");
            return null;
        }
        for (int i = 0; i < subnodes.length; i++) {
            ret = subnodes[i];
            if(ret.subIndex == subindex) break;
            ret = null;
        }
        if(ret == null) {
            FLog.log("ODNode.findObject(" + FString.int2HexStr(index) + ", " + FString.int2HexStr(subindex) + ")", FLog.LOG_ERR, "subindex not found..");
            return null;
        }
        return ret;
    }

    /**
     * sets content of OD object
     * @param index
     * @param subindex ignored if object does not have subindexes
     * @param data byte array containing object data in CAN endianing (little endian)
     * @return true if success
     */
    public boolean setValue(int index, int subindex, byte[] data)
    {
        ODNode nd = findObject(index, subindex);
        if(nd == null) return false;
        nd.value = data;
        return true;
    }

    /**
     * get content of OD object
     * @param index
     * @param subindex subindex ignored if object does not have subindexes
     * @return byte array containing object data in CAN endianing (little endian) or null
     */
    public byte[] getValue(int index, int subindex)
    {
        ODNode nd = findObject(index, subindex);
        if(nd == null) return null;
        return nd.getValue();
    }
}
