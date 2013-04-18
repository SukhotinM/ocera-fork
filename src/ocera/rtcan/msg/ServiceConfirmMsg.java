package ocera.rtcan.msg;

/**
 * ocera.rtcan.ServiceSetRawMsgParamConfirm
 * <p/>
 * (C) Copyright 10:48:17 AM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class ServiceConfirmMsg extends ServiceMsgBase
{
    public static final int OK = 0;
    public static final int ERROR = 1;

    public int code = OK;
    public String errmsg = "";
}
