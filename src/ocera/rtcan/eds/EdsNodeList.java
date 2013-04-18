package ocera.rtcan.eds;

import java.util.*;

/**
 * @author vacek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class EdsNodeList
{
    protected ArrayList nodeList = new ArrayList();

    public EdsNodeList() {}

    public EdsNode getNode(int ix) {
        return (EdsNode)nodeList.get(ix);
    }

    public void addNode(EdsNode nd) {
        nodeList.add(nd);
    }
}
