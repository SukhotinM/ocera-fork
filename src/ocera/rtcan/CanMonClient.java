/**
 * Created by IntelliJ IDEA.
 * Date: Dec 10, 2002
 * Time: 12:19:53 PM
 * @author F. Vacek, vacek@rtime.felk.cvut.cz
 */
package ocera.rtcan;

import ocera.util.*;

import javax.swing.*;
import java.net.*;
import java.io.*;

import org.flib.FLog;
import org.flib.FString;
import org.flib.net.CPickle;
import org.flib.net.CPickleException;

public class CanMonClient implements Runnable
{
    protected Runnable guiUpdate = null;
    // queue of red CAN messages
    public static final int RQUEUE_LEN_MAX = 50;
    public RoundQueue readQueue = new RoundQueue(RQUEUE_LEN_MAX);
    public static final int DEFAULT_PORT = 1001;
    protected Socket canSocket = null;
    protected String errMsg;
    protected BufferedInputStream inSockStream = null;
    protected BufferedOutputStream outSockStream = null;
    public static final int CAN_DATAGRAM_LEN_MAX = 26;
    private Thread readThread = null;
    private boolean terminateReadThread = false;

    /**
     * @param guiUpdate thread running GUI (swing)
     */
    public void setGuiUpdate(Runnable guiUpdate) {
        this.guiUpdate = guiUpdate;
    }

    /**
     * @return the description of the last CanMonClient error.
     */
    public String getErrMsg() {
        return errMsg;
    }

    /**
     * Close the socket to canmond server openned by connect()<br><br>
     * @return 0 if succeed.<br>
     * Use getErrMsg() to find out error description.
     * @see ocera.rtcan.CanMonClient#getErrMsg
     */
    public int disconnect() {
        int ret = 0;
        terminateReadThread = true;
        try {
            if(inSockStream != null) inSockStream.close();
        } catch(IOException e) { }
        inSockStream = null;
        outSockStream = null;
        errMsg = "OK";
        if(canSocket != null) {
            FLog.logcont("CanMonClient", FLog.LOG_INF,  "disconnecting from " + canSocket.getInetAddress());
            try {
                canSocket.close();
            } catch(IOException e) {
                ret = -1;
                errMsg = "Failed to disconnect from server.";
            }
            canSocket = null;
            FLog.logcont(FLog.LOG_INF,  errMsg);
        }
        return ret;
    }

    public Socket getSocket() {
        return canSocket;
    }

    /**
     * Opens socket to canmond server<br><br>
     * @return 0 if succeed.<br>
     * Use getErrMsg() to find out error description.

     * @param  ip   the URL of server
     * @param  port the port number, if 0 the default value DEFAULT_PORT is taken.<br><br>

     * @see ocera.rtcan.CanMonClient#getErrMsg
     */
    public int connect(String ip, int port) {
        int ret = -1;
        disconnect();
        errMsg = "";
        try {
            InetAddress addr = InetAddress.getByName(ip);
            FLog.log("CanMonClient", FLog.LOG_INF,  "connecting to " + addr);
            try {
                if(port == 0) port = DEFAULT_PORT;
                canSocket = new Socket(addr, port);
                errMsg = "connected OK";
                inSockStream = new BufferedInputStream(canSocket.getInputStream());
                outSockStream = new BufferedOutputStream(canSocket.getOutputStream());
                ret = 0;

                // launch read thread
                readThread = new Thread(this);
                terminateReadThread = false;
                readThread.start();
                ret = 0;
            } catch(IOException e) {
                errMsg = "ERROR: connection failed";
                canSocket = null;
            }
        } catch(UnknownHostException e) {
            errMsg = "ERROR: unknown host.";
        }
        FLog.log("CanMonClient", FLog.LOG_INF,  errMsg);
        return ret;
    }

    public boolean connected() {
        return getSocket() != null;
    }

    /**
     * Send message to canmond server<br><br>
     * Use getErrMsg() to find out error description.

     * @return length of msg if succeed, -1 else.<br>

     * @param  o   object to send over IP

     * @see ocera.rtcan.CanMonClient#getErrMsg
     */
    public int send(Object o) {
        errMsg = "";
        int ret = -1;
        if(outSockStream == null) return ret;
        try {
            CPickle pck = new CPickle();
            byte[] data = pck.toNet(o);
            outSockStream.write(data);
            outSockStream.flush();
            ret = data.length;
        } catch(CPickleException e) {
            errMsg = "ERROR: PICKLE - " + e.getMessage();
            FLog.log("CanMonClient", FLog.LOG_INF,  errMsg);
            e.printStackTrace();
        }
        catch(Exception e) {
            errMsg = "ERROR: unsuccesfull write to the socket - " + e.getMessage();
            FLog.log("CanMonClient", FLog.LOG_INF,  errMsg);
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Communication thread body
     */
    public void run()
    {
        //CANDtgMsg workingmsg = new CANDtgMsg();
        CPickle pck = new CPickle();
        while(!terminateReadThread) {
            try {
                byte packet[] = pck.readObjectPacket(inSockStream);
                FLog.log("CanMonClient", FLog.LOG_DEB, "New data from socket received: " + FString.bytes2String(packet));
                FLog.log("CanMonClient", FLog.LOG_DEB, "--------------------------------");
                Object o = pck.fromNet(packet);
                FLog.log("CanMonClient", FLog.LOG_INF, "\t" + o.toString());
                if (guiUpdate != null) {
                    // send object to the monitor msg queue
                    readQueue.append(o);
                }
            }
            catch (CPickleException e) {
                FLog.log("CanMonClient", FLog.LOG_ERR, "ERROR: invalid data received - " + e.getMessage());
            }
            catch (IOException e) {
                if(!terminateReadThread) FLog.log(getClass().getName(), FLog.LOG_ERR, e.getMessage());
            }
            if (guiUpdate != null) {
                SwingUtilities.invokeLater(guiUpdate);
            }
        }
        FLog.log("CanMonClient", FLog.LOG_MSG, "socket closed.");
    }
}

