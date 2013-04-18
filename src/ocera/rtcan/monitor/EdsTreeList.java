package ocera.rtcan.monitor;

import java.util.*;

/**
 * @author vacek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class EdsTreeList
{
    protected ArrayList nodeList = new ArrayList();

    public EdsTreeList() {}

    public EdsTreeNode getNode(int ix) {
        return (EdsTreeNode)nodeList.get(ix);
    }

    public void addNode(EdsTreeNode nd) {
        nodeList.add(nd);
    }
}
