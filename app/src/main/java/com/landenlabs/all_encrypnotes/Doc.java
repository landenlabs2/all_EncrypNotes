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
 * (c) 2009.-2014. Ivan Voras <ivoras@fer.hr>
 * Released under the 2-clause BSDL.
 *
 * Updated and rewritten by Dennis Lang 2015/2016.
 */

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.landenlabs.all_encrypnotes.ui.LogIt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Document abstraction; contains load and save encrypted file routines.
 *
 * @author Ivan Voras
 * <br>WebSite: {@link http://sourceforge.net/projects/enotes/}
 *
 * Updated and rewritten by Dennis Lang 2015/2016
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 * 
 */
public class Doc {

    static final byte[] SIGNATURE = {0x00, (byte) 0xff, (byte) 0xed, (byte) 0xed};
    static final byte VERSION_FORMAT = 3;
    static final byte VERSION_FORMAT_HAS_HINT = 2;
    static final byte VERSION_FORMAT_HAS_HASH_LEN = 3;
    static final byte VERSION_MINOR = 5;
    static final byte VERSION_MINOR_HAS_ENC = 2;
    static final String ENC = "UTF-8"; //   Charsets.UTF_8 Java 1.7
    static final int HISTORY_SAME_EDIT_MINUTES = 15;    // Same edit if changes made within these minutes
    static final int HISTORY_MAX_ENTRIES = 10;
    static final int HINT_MAX_LEN = 16;

    public static class DocException extends Exception {

        private static final long serialVersionUID = -8618217887516753754L;

        /**
         * Creates a new instance of <code>DocException</code> without detail message.
         */
        public DocException() {
        }

        /**
         * Constructs an instance of <code>DocException</code> with the specified detail message.
         *
         * @param msg the detail message.
         */
        public DocException(String msg) {
            super(msg);
        }
    }

    public static class DocPasswordException extends DocException {

        private static final long serialVersionUID = 8526674767186417815L;

        /**
         * Creates a new instance of <code>DocPasswordException</code> without detail message.
         */
        public DocPasswordException() {
        }

        /**
         * Constructs an instance of <code>DocPasswordException</code> with the specified detail message.
         *
         * @param msg the detail message.
         */
        public DocPasswordException(String msg) {
            super(msg);
        }
    }

    public static class SaveMetadata implements Serializable {

        private static final long serialVersionUID = 1L;
        public long     timestamp;
        public String   username;

        public SaveMetadata(long timestamp, String username) {
            this.timestamp = timestamp;
            this.username = username;
        }
    }
    
    /**
     * @author ivoras (original author, modified by Dennis Lang)
     */
    public static class DocMetadata implements Serializable {

        private static final long serialVersionUID = 1L;

        public ArrayList<Doc.SaveMetadata> saveHistory = new ArrayList<Doc.SaveMetadata>();
        public boolean modified = false;
        public String filename;
        public String hint;
        public int caretPosition;
        public byte[] key;

        void saveMetadata(DataOutputStream oStrm) throws IOException {
            oStrm.writeInt(caretPosition);
            oStrm.writeUTF(filename);

            oStrm.writeInt(saveHistory.size());
            for (SaveMetadata sm : saveHistory) {
                oStrm.writeLong(sm.timestamp);
                oStrm.writeUTF(sm.username);
            }

            // oStrm.writeUTF(hint);
        }

        void loadMetadata(DataInputStream iStrm, int minVer) throws IOException {
            caretPosition = iStrm.readInt();
            filename = iStrm.readUTF();

            int nSave = iStrm.readInt();
            saveHistory = new ArrayList<SaveMetadata>(nSave+1);
            for (int idx = 0; idx < nSave; idx++)
                saveHistory.add(new Doc.SaveMetadata(iStrm.readLong(), iStrm.readUTF()));

            // hint = iStrm.readUTF();
        }

        void copyTo(DocMetadata other) {
            other.caretPosition = caretPosition;
            other.filename = filename;
            other.hint = hint;
            other.modified = modified;
            other.key = key.clone();
            other.saveHistory = new ArrayList<Doc.SaveMetadata>(saveHistory);
        }
        
        public void setKey(String pwd) {
            key = Util.sha1hash(pwd);
        }
    }

    /**
     * Crypto mode to use while writing the file
     */
    public static final String CRYPTO_MODE = "AES/CBC/PKCS5Padding";

    /**
     * The short name of the crypt algorithm used on the files
     */
    public static final String CRYPTO_ALG = "AES";

    private byte m_verFormat = 0;
    private byte m_verMinor = 0;
    private String m_hint;
    private String m_text;
    private DocMetadata m_docMeta;

    public Doc(String text, DocMetadata docm) {
        m_text = text;
        m_docMeta = docm;
    }

    public Doc() {
        m_text = "";
        m_docMeta = new DocMetadata();
    }

    public String getHint() {
        return m_hint;
    }

    @SuppressLint("DefaultLocale")
    public String getVersion() {
        return String.format("v%d.%d", m_verFormat, m_verMinor);
    }

    public static String getInfoStr(Doc.DocMetadata docMetaData, DateFormat dataFormat) {
        StringBuilder sb = new StringBuilder();

        if (docMetaData != null ) {

            try {
                if (!TextUtils.isEmpty(docMetaData.filename)) {
                    sb.append("\nFile: ").append(docMetaData.filename);
                    File file = new File(DocFileDlg.getDir(), docMetaData.filename);
                    sb.append("\nLastMod: ").append(dataFormat.format(file.lastModified()));
                    sb.append(String.format("\nSaved Length: %,d", file.length()));

                    Doc doc = new Doc();
                    doc.doOpen(file, null);
                    sb.append("\nVersion: ").append(doc.getVersion());
                    if (!TextUtils.isEmpty(doc.getHint()))
                        sb.append("\nHint: ").append(doc.getHint());
                } else {
                    sb.append("\nNew Note\nNot saved");
                }
            } catch (Exception ex) {
            }

            if (docMetaData != null && docMetaData.saveHistory.size() != 0) {
                sb.append("\nHistory: ");
                for (Doc.SaveMetadata saveMetaData : docMetaData.saveHistory) {
                    sb.append("\n ");
                    sb.append(dataFormat.format(saveMetaData.timestamp));
                    // sb.append(" ");
                    // sb.append(saveMetaData.username);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Saves the currently edited document to the given file.
     *
     * @param outFile
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     * @throws #DocPasswordException
     */
    @SuppressLint("TrulyRandom")
    public boolean doSave(File outFile, String hint) throws FileNotFoundException, IOException, DocPasswordException {
        assert (m_docMeta.key != null);

        String current_user = System.getProperty("user.name");
        if (m_docMeta.saveHistory.size() != 0) {
            SaveMetadata saveMetaData = m_docMeta.saveHistory.get(m_docMeta.saveHistory.size() - 1);
            if (!saveMetaData.username.equalsIgnoreCase(current_user) ||
                    TimeUnit.MICROSECONDS.toMinutes(System.currentTimeMillis() - saveMetaData.timestamp)
                            > Doc.HISTORY_SAME_EDIT_MINUTES) {
                saveMetaData = new SaveMetadata(System.currentTimeMillis(), current_user);
                m_docMeta.saveHistory.add(saveMetaData);
            } else
                saveMetaData.timestamp = System.currentTimeMillis();
        } else
            m_docMeta.saveHistory.add(new SaveMetadata(System.currentTimeMillis(), current_user));

        while (m_docMeta.saveHistory.size() > Doc.HISTORY_MAX_ENTRIES)
            m_docMeta.saveHistory.remove(0);

        if (m_docMeta.key == null)
            throw new DocPasswordException("Key not set in DocMetadata");

        FileOutputStream fout = new FileOutputStream(outFile);
        BufferedOutputStream bout = new BufferedOutputStream(fout);

        // 1. Save prefix
        bout.write(Doc.SIGNATURE);
        bout.write(Doc.VERSION_FORMAT);
        bout.write(Doc.VERSION_MINOR);

        // 2. Save Hint in clear text.
        hint = (hint == null) ? "" : hint.substring(0, Math.min(hint.length(), HINT_MAX_LEN));
        byte[] hintBytes = hint.getBytes(ENC);
        bout.write(hintBytes.length);
        bout.write(hintBytes);

        byte[] randomBytes = new byte[16];
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(randomBytes);
        } catch (NoSuchAlgorithmException ex) {
            LogIt.log(this.getClass(), LogIt.ERROR, null, ex);
            System.exit(1);
        }

        // 3. Save key hash and randomBytes used to make hash
        byte[] keyHash = Util.sha1hash(Util.concat(m_docMeta.key, randomBytes));
        bout.write(keyHash.length); // Add format version 3
        bout.write(keyHash);
        bout.write(randomBytes);

        Cipher ecipher = getCipher(Cipher.ENCRYPT_MODE, m_docMeta.key, randomBytes);
        CipherOutputStream cout = new CipherOutputStream(bout, ecipher);
        GZIPOutputStream zout = new GZIPOutputStream(cout);
        DataOutputStream dout = new DataOutputStream(zout);

        // 4. Save encrypted meta data (modify history)
        m_docMeta.saveMetadata(dout);

        // 5. Save encrypted text
        byte[] ddata = m_text.getBytes(ENC);
        dout.writeInt(ddata.length);
        dout.write(ddata);

        // System.out.println("Written " + ddata.length + " bytes");
        // dout.writeUTF(text); // Java doesn't work with strings > 64 KiB :DD

        try {
            dout.close();
            // zout.close();    // Are these really needed ?
            // cout.close();
            // bout.close();
            // fout.close();
        } catch (IOException e) {
            LogIt.log(this.getClass(), LogIt.ERROR, "Save file", e);
        }
        return true;
    }

    /**
     * Opens the specified file to be the currently edited document.
     *
     * @param fOpen File to read.
     * @param pwd  set to null to get hint but not decrypt file.
     *
     * @return true if file decrypted
     *
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public boolean doOpen(File fOpen, String pwd) throws FileNotFoundException, IOException, DocException, DocPasswordException {

        FileInputStream fin = new FileInputStream(fOpen);
        BufferedInputStream bin = new BufferedInputStream(fin);

        byte[] sig = new byte[Doc.SIGNATURE.length];

        // 1. Read header - signature, format and version.
        bin.read(sig);
        m_verFormat = (byte) bin.read();
        m_verMinor = (byte) bin.read();

        DocException docEx = null;

        boolean equal = Arrays.equals(sig, Doc.SIGNATURE);
        if (!equal) {
            docEx = new DocException("File is not a valid EncrypNotes file: " + fOpen.getAbsolutePath());
        } else if (m_verFormat > Doc.VERSION_FORMAT) {
            docEx = new DocException("File is a EncrypNotes file but cannot be opened by this version of the program");
        } else if  (m_verFormat > Doc.VERSION_MINOR) {
            docEx = new DocException("File format version is newer than this app version supports");
        }

        if (docEx != null) {
            bin.close();
            throw docEx;
        }

        // 2. Read hint
        this.m_hint = "";
        if (m_verFormat >= Doc.VERSION_FORMAT_HAS_HINT) {
            int hintLen = (byte)bin.read();
            if (hintLen >= 0 && hintLen < HINT_MAX_LEN) {
                byte[] hintBytes = new byte[hintLen];
                bin.read(hintBytes);
                this.m_hint = new String(hintBytes, ENC);
            } else {
                this.m_hint = "???";
            }
        }

        // Return if no password and just wanted hint.
        if (pwd == null) {
            return false;
        }

        // 3. Read hash and randomBytes
        byte[] pwdhash = new byte[2];
        if (m_verMinor >= Doc.VERSION_FORMAT_HAS_HASH_LEN) {
            int hintLen = (byte)bin.read();
            if (hintLen != pwdhash.length)
                pwdhash = new byte[hintLen];
        }
        bin.read(pwdhash);
        byte[] randomBytes = new byte[16];
        bin.read(randomBytes);

        DocMetadata newdocm = new DocMetadata();
        newdocm.key = Util.sha1hash(pwd);

        byte[] keyHash = Util.sha1hash(Util.concat(newdocm.key, randomBytes));
        if (m_verMinor >= Doc.VERSION_FORMAT_HAS_HASH_LEN) {
            equal = Arrays.equals(keyHash, pwdhash);
        } else {
            equal = (keyHash[0] == pwdhash[0]) && (keyHash[1] == pwdhash[1]);
        }

        if (!equal) {
        	bin.close();
            throw new DocPasswordException("Invalid password!");
        }

        Cipher dcipher = getCipher(Cipher.DECRYPT_MODE, newdocm.key, randomBytes);
        CipherInputStream cin = new CipherInputStream(bin, dcipher);
        GZIPInputStream zin = new GZIPInputStream(cin);
        DataInputStream din = new DataInputStream(zin);

        // 4. Read encrypted Meta data.
        String newtext;
        newdocm.loadMetadata(din, m_verMinor);

        // 5. Read encrypted text.
        if (m_verMinor < VERSION_MINOR_HAS_ENC)
            newtext = din.readUTF();
        else {
            int len = din.readInt();
            byte[] ddata = new byte[len];
            int total_read = 0;
            while (total_read < len) {
                int nread = din.read(ddata, total_read, len - total_read);
                total_read += nread;
            }
            System.out.println("Read " + total_read + " bytes");
            newtext = new String(ddata, ENC);
        }

        din.close();
        // zin.close();     // Are these really needed ?
        // cin.close();
        // bin.close();
        // fin.close();

        newdocm.filename = fOpen.getAbsolutePath();
        newdocm.hint = m_hint;
        m_docMeta = newdocm;
        m_text = newtext;

        return true;
    }

    public String getText() {
        return m_text;
    }

    public DocMetadata getDocMetadata() {
        return m_docMeta;
    }

    /***
     * Create cipher to d/encrypt stream.
     *
     * @param cipherMode  Cipher.DECRYPT_MODE or Cipher.ENCRYPT_MODE
     * @param key
     * @param randomBytes
     * @return
     */
    private Cipher getCipher(int cipherMode, byte[] key, byte[] randomBytes)  {
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(randomBytes);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(CRYPTO_MODE);
        } catch (NoSuchAlgorithmException ex) {
            LogIt.log(this.getClass(), LogIt.ERROR, null, ex);
            System.exit(1);
        } catch (NoSuchPaddingException ex) {
            LogIt.log(this.getClass(), LogIt.ERROR, null, ex);
            System.exit(1);
        }

        try {
            cipher.init(cipherMode, new SecretKeySpec(key, 0, 16, CRYPTO_ALG), paramSpec);
        } catch (InvalidKeyException ex) {
            LogIt.log(this.getClass(), LogIt.ERROR, null, ex);
            System.exit(1);
        } catch (InvalidAlgorithmParameterException ex) {
            LogIt.log(this.getClass(), LogIt.ERROR, null, ex);
            System.exit(1);
        }

        return cipher;
    }
}
