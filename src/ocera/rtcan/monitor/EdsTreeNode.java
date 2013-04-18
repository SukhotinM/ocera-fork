package ocera.rtcan.monitor;

/**
 * Created by IntelliJ IDEA.
 *
 * User: fanda
 * Date: 30.3.2003
 * Time: 15:03:09
 * To change this template use Options | File Templates.
 */
public interface EdsTreeNode
{
    String getName();
    int getAttrCnt();
    String getAttrName(int ix);
    String getAttrValue(int ix);
    String getAttrDescription(int ix);
    //void setAttrName(int ix, String s);
    void setAttrValue(int ix, String s);
}
