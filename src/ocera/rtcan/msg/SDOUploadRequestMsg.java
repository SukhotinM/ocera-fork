package ocera.rtcan.msg;

/**
 * ocera.rtcan.SDOUploadRequestMsg
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
public class SDOUploadRequestMsg extends SDOMsgBase
{
    public String toString()
    {
        String s = "SDO upload request - ";
        s += super.toString();
        return s;
    }
}
