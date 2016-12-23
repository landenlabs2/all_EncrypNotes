/*
 *  Copyright (c) 2015 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dennis Lang  (Dec-2015)
 *  @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 *
 */

package com.landenlabs.all_encrypnotes.ui;

import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.landenlabs.all_encrypnotes.BuildConfig;

/**
 * Created by Dennis Lang on 7/3/2015.
 *
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 *
 * Code lifted from:
 * https://developer.android.com/samples/MediaBrowserService/src/com.example.android.mediabrowserservice/utils/LogHelper.html
 *
 * Notes:
 *   Use adb shell setprop log.tag.<YOUR_LOG_TAG> <LEVEL>
 *   to enable specific logging where LEVEL is
 *      VERBOSE, DEBUG, INFO, WARN, ERROR
 *   default is INFO for all TAGs.
 *
 */
public class LogIt {

    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;


    static boolean s_debugMode = BuildConfig.DEBUG;

    public static void setDebugMode(ApplicationInfo appInfo) {
        s_debugMode = ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
    }

    /**
     * Don't use this when obfuscating class names!
     */
    public static String getTag(Class cls) {
        return cls.getSimpleName();
    }

    public static void v(String tag, String message) {
        log(tag, Log.VERBOSE, message);
    }

    public static void d(String tag, String message) {
        log(tag, Log.DEBUG, message);
    }

    public static void i(String tag, String message) {
        log(tag, Log.INFO, message);
    }

    public static void w(String tag, String message) {
        log(tag, Log.WARN, message);
    }

    public static void w(String tag, String message, Throwable t) {
        log(tag, Log.WARN, message, t);
    }

    public static void e(String tag, String message) {
        log(tag, Log.ERROR, message);
    }

    public static void e(String tag, String message, Throwable t) {
        log(tag, Log.ERROR, message, t);
    }

    public static void log(String tag, int level, String message) {
        log(tag, level, message, null);
    }

    public static void log(Class cls, int level, String message, Throwable t) {
        log(getTag(cls), level, message, t);
    }

    public static void log(String tag, int level, String message, Throwable t) {
        // Only log if build type is DEBUG
        if (s_debugMode && Log.isLoggable(tag, level)) {
            if (t != null) {
                StringBuilder sb = new StringBuilder(message);
                if (t != null) {
                    sb.append("\n").append(Log.getStackTraceString(t));
                }
                message = sb.toString();
            }

            Log.println(level, tag, message);
        }
    }
}
