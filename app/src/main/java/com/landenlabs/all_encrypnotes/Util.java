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

package com.landenlabs.all_encrypnotes;

/*
 * (c) 2009.-2010. Ivan Voras <ivoras@fer.hr>
 * Released under the 2-clause BSDL.
 */

import android.annotation.SuppressLint;
import android.util.Log;

import com.landenlabs.all_encrypnotes.ui.LogIt;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Original version by ivoras
 * @author ivoras
 *
 *
 * Updated and rewritten by Dennis Lang 2015/2016
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 *
 */
public class Util {

    /**
     * @return Current date.
     */
    public static Date getCurrentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * @param filename
     * @return {@code true} if file exists.
     */
    public static boolean fileExists(String filename) {
        return new File(filename).exists();
    }


    /**
     * Returns a binary MD5 hash of the given string.
     *
     * @param s
     * @return
     */
    public static byte[] md5hash(String s) {
        return hash(s, "MD5");
    }


    /**
     * Returns a binary MD5 hash of the given binary buffer.
     *
     * @param buf
     * @return
     */
    public static byte[] md5hash(byte[] buf) {
        return hash(buf, "MD5");
    }


    /**
     * Returns a binary SHA1 hash of the given string.
     *
     * @param s
     * @return
     */
    public static byte[] sha1hash(String s) {
        return hash(s, "SHA1");
    }


    /**
     * Returns a binary SHA1 hash of the given buffer.
     *
     * @param buf
     * @return
     */
    public static byte[] sha1hash(byte[] buf) {
        return hash(buf, "SHA1");
    }


    /**
     * Returns a binary hash calculated with the specified algorithm of the
     * given string.
     *
     * @param s
     * @param hashAlg
     * @return
     */
    public static byte[] hash(String s, String hashAlg) {
        byte b[] = null;
        try {
            b = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LogIt.log(Util.class, LogIt.ERROR, null, ex);
            System.exit(1);
        }
        return hash(b, hashAlg);
    }


    /**
     * Converts a binary buffer to a string of lowercase hexadecimal characters.
     *
     * @param h
     * @return
     */
    public static String bytea2hex(byte[] h) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h.length; i++)
            sb.append(String.format("%02x", h[i] & 0xff));
        return sb.toString();
    }


    /**
     * Returns a binary hash calculated with the specified algorithm of the
     * given input buffer.
     *
     * @param buf
     * @param hashAlg
     * @return
     */
    public static byte[] hash(byte[] buf, String hashAlg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(hashAlg);
        } catch (NoSuchAlgorithmException ex) {
            LogIt.log(Util.class, LogIt.ERROR, null, ex);
            System.exit(1);
        }
        return md.digest(buf);
    }


    /**
     * Concatenates two byte arrays and returns the result.
     *
     * @param src1
     * @param src2
     * @return
     */
    public static byte[] concat(byte[] src1, byte[] src2) {
        byte[] dst = new byte[src1.length + src2.length];
        System.arraycopy(src1, 0, dst, 0, src1.length);
        System.arraycopy(src2, 0, dst, src1.length, src2.length);
        return dst;
    }

    public static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private static final String APP_VERSION_INFO_ID_FORMAT = "%s; version info";
        private static final String ERROR_REPORT_FORMAT = "yyyy.MM.dd HH:mm:ss z";
        @SuppressLint("SimpleDateFormat")
        private SimpleDateFormat format = new SimpleDateFormat(ERROR_REPORT_FORMAT);

        private Thread.UncaughtExceptionHandler originalHandler;

        /**
         * Creates a reporter instance
         *
         * @throws NullPointerException if the parameter is null
         */
        public UncaughtExceptionHandler() throws NullPointerException {
            originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {

            String stackTrace = Log.getStackTraceString(ex);
            Log.d("UncaughtException", stackTrace);
            Log.e("UncaughtException", ex.getLocalizedMessage(), ex);

            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, ex);
            }
        }

    }
}
