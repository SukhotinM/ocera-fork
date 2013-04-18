package org.flib;

/**
 * Created by IntelliJ IDEA.
 * User: fanda
 * Date: Feb 20, 2004
 * Time: 4:11:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class FLog
{
    /**
     * bit signalizing log() function to continue in previous logging (not to write domain and level information)
     */
    private static int LOG_IS_CONT = (1<<16);

    public static int LOG_FATAL = 0;
    public static int LOG_ERR = 1;
    public static int LOG_MSG = 2;
    public static int LOG_INF = 3;
    public static int LOG_DEB = 4;
    public static int LOG_TRASH = 5;

    /**
     * only messages with level lower or equal than logTreshold will be logged
     */
    public static int logTreshold = LOG_MSG;

    /**
     * Same as log() but does not append newline
     * @see org.flib#log(String, int, String)
     */
    static public void logcont(String domain, int level, String msg) {
        log(domain, level | LOG_IS_CONT, msg);
    }
    /**
     * Same as logcont() but does not prepend domain and log level information
     * @see org.flib#log(String, int, String)
     */
    static public void logcont(int level, String msg) {
        log("", level | LOG_IS_CONT, msg);
    }

    /**
     * log message msg in form '&lt;level&gt; domain: msg'
     * @param domain
     * @param level only messages with level lower or equal than logTreshold will be logged
     * @param msg message to log
     */
    static public void log(String domain, int level, String msg)
    {
        if(level <= logTreshold) {
            if((level & LOG_IS_CONT) == 0)
                System.err.println("<" + level + "> " + domain + ": " + msg);
            else
                System.err.print(msg);
        }
    }
}
