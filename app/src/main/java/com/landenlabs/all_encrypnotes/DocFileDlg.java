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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;

import com.landenlabs.all_encrypnotes.ui.RenameDialog;
import com.landenlabs.all_encrypnotes.ui.UiUtil;
import com.landenlabs.all_encrypnotes.ui.WebDialog;
import com.landenlabs.all_encrypnotes.ui.YesNoDialog;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;

/**
 * Abstract base class for Load and Save Document file UI.
 * 
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 *
 */
public class DocFileDlg {
    
    // --Static constants
    public  static final int FILE_CONTEXT_MENU = 1;
    
    private static final String DOC_DIR = "documents/encrypnotes";
    private static final String DOC_EXT = ".etxt";
    private static final DateFormat m_dateFormat = DateFormat.getDateInstance();
    private static final File STORAGE_DIR = new File(Environment.getExternalStorageDirectory(), DOC_DIR);

    private Doc.DocMetadata m_docMetadata = new Doc.DocMetadata();
    
    // Passed parameters
    private final Activity m_context;
    
    // Common internal objects
    private UiPasswordManager m_managePwd;
    private SimpleFileDialog m_fileOpenDialog;
    
    /**
     * Base class constructor
     * 
     * @param context
     *       UI context object (main activity)
     */
    public DocFileDlg(Activity context) {
        m_context = context;
    }
    
    /**
     * Ensure storage directory exists, make if needed.
     * @return
     */
    public boolean ensureDocDir() {
        // PermissionChecker.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        int permissionCheck = ContextCompat.checkSelfPermission(m_context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // OK permission granted, let's do stuff
            return (STORAGE_DIR.isDirectory() || STORAGE_DIR.mkdirs());
        }

        // Need to ask for permission
        return false;
    }

    /**
     * @return storage directory.
     */
    public static File getDir() {
        return STORAGE_DIR;
    }
    
    /**
     * @return current document filename
     */
    public final String getFilename() {
        return m_docMetadata.filename;
    }
    
    /**
     * @return current document name (less extension)
     */
    public final String getName() {
        return TextUtils.isEmpty(m_docMetadata.filename) ? "" : m_docMetadata.filename.replace(DOC_EXT, "");
    }
    
    /**
     * @return true if document is currently modified.
     */
    public boolean isModified() {
        return m_docMetadata.modified;
    }
    
    /**
     * Set new modified state, marking if document needs to be saved.
     * @param newModified
     */
    public void setModified(boolean newModified) {
        m_docMetadata.modified = newModified;
    }
    
    /**
     * @return true if filename exists and encryption key specified. 
     */
    public boolean canSave() {
        return (!TextUtils.isEmpty(m_docMetadata.filename)
                && m_docMetadata.key != null && m_docMetadata.key.length != 0);
    }
    
    /**
     * Clear metaData (create new instance).
     */
    public void Clear() {
        m_docMetadata = new Doc.DocMetadata();
    }
    
    /**
     * Save docMetaData plus document text (via passed in EditText object).
     * @param b
     * @param docText
     */
    public void saveInstanceState(Bundle b, EditText docText) {
        m_docMetadata.caretPosition = docText.getSelectionStart();
        b.putString("text", docText.getText().toString());  // TODO - encrypt
        b.putSerializable("metadata", m_docMetadata);
    }
    
    /**
     * Restore saved docMetaData and text object. 
     * 
     * @param b
     * @param docText
     */
    public void restoreInstanceState(Bundle b, EditText docText) {
        m_docMetadata = (Doc.DocMetadata) b.getSerializable("metadata");
        UiUtil.setText(docText, b.getString("text"));       // TODO - decrypt
    }

    public void showInfo() {
        String infosStr = Doc.getInfoStr(m_docMetadata, m_dateFormat).replace("\n", "<p>");
        // YesNoDialog.showDialog(m_context, "Info", infosStr,
        //         R.id.file_info, YesNoDialog.BTN_OK);
        String htmlStr = infosStr.replaceAll("\n([^:]*:)(.*)", "<tr><td><span style='color:blue;'>$1</span><td>$2");
        WebDialog.show(m_context, /* WebDialog.HTML_CENTER_BOX, */
                String.format("<center><h2>Info</h2></center><p><table>%s</table>", htmlStr));
    }

    // -------------------------------------------------------------------------------------------
    //                                     L O A D     S e c t i o n
    // -------------------------------------------------------------------------------------------
    
    /**
     * Show list of available encrpyted files. 
     * Load and decrypt selected file into text object.
     * 
     * On completion send message Okay or Fail.
     */
    public void showLoad(final EncrypPrefs prefs, final EditText docText, final SendMsg sendMsg) {
        File[] file_list = STORAGE_DIR.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fname) {
                return fname.endsWith(DOC_EXT);
            }
        });

        if (file_list == null || file_list.length == 0) {
            YesNoDialog.showOk(m_context, m_context.getResources().getString(R.string.no_etxt_files, DOC_EXT, DOC_DIR));
            return;
        }

        final String[] file_names = new String[file_list.length];
        for (int i = 0; i < file_list.length; i++)
            file_names[i] = file_list[i].getName().replaceAll(DOC_EXT, "");

        AlertDialog.Builder builder = new AlertDialog.Builder( 
                new ContextThemeWrapper(m_context, R.style.FileListDialogStyle));
        builder.setTitle(m_context.getResources().getString(R.string.list_etxt_files, DOC_EXT, DOC_DIR));
        final FileListAdapter fileListAdapter = new FileListAdapter(m_context, STORAGE_DIR, DOC_EXT,
                m_dateFormat, m_context.getWindow().getDecorView().getHeight());

        fileListAdapter.addAll(file_names);
        fileListAdapter.sort();

        builder.setAdapter(fileListAdapter,
                new DialogInterface.OnClickListener() {
                    // *** THIS DOES NOT FIRE ON SELECTION ***
                    // See custom OnItemClick methods below.
                    // Details:  After adding custom logic to handle long click on list items
                    // it breaks this default click action and forces all click actions to
                    // be handled via custom logic.
                    public void onClick(DialogInterface dialog, int pos) {
                        dialog.dismiss();
                        loadSelectedFile(prefs, fileListAdapter.getItem(pos), docText, sendMsg);
                    }
                });

        final AlertDialog alert = builder.create();

        fileListAdapter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int pos, long id) {
                final String filename = fileListAdapter.getItem(pos);
                final View adapterView = arg0;

                PopupMenu popup = new PopupMenu(m_context, view);
                popup.getMenuInflater().inflate(R.menu.file_context_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @SuppressLint("DefaultLocale")
                    public boolean onMenuItemClick(MenuItem item) {
                        File file = fileListAdapter.getFile(filename);

                        switch (item.getItemId()) {
                            case R.id.file_delete:
                                YesNoDialog.showDialog(m_context, "Delete ?",
                                        String.format("Name:     %s\nModified: %s\nLength:    %,d",
                                                filename, fileListAdapter.getDateStr(file), file.length()),
                                        R.id.file_delete, YesNoDialog.BTN_YES_NO).setValue(filename).setViewer(fileListAdapter);
                                break;
                            case R.id.file_info:
                                YesNoDialog.showDialog(m_context, "Info",
                                        String.format("Name:     %s\nModified: %s\nLength:    %,d\n",
                                                filename, fileListAdapter.getDateStr(file), file.length()) +
                                                fileListAdapter.getInfoStr(file),
                                        R.id.file_info, YesNoDialog.BTN_OK).setValue(filename).setViewer(fileListAdapter);
                                break;
                            case R.id.file_rename:
                                RenameDialog renameDlg = RenameDialog.create(R.layout.rename_dlg, R.id.file_rename);
                                renameDlg.showIt(m_context.getFragmentManager(), "renameDlg").setFrom(filename).setViewer(fileListAdapter);
                                break;
                        }
                        return true;
                    }
                });

                popup.show();//showing popup menu

                return true;
            }
        });

        fileListAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                loadSelectedFile(prefs, fileListAdapter.getItem(pos), docText, sendMsg);
                alert.dismiss();
            }
        });


        alert.show();
        // alert.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    private void setHint(String filename) {
        String hint = getHint(filename);
        this.m_managePwd.setHint(hint);
    }

    /**
     * Ask for password to load and decrypt file.
     * 
     * Process is asynchronous, see SendMsg for indication of success/failure.
     * 
     * @param prefs
     * @param fname
     * @param docText
     * @param sendMsg
     */
    private void loadSelectedFile(final EncrypPrefs prefs, final String fname, final EditText docText, final SendMsg sendMsg) {
        final Dialog dlg = new Dialog(new ContextThemeWrapper(m_context, R.style.OpenDialogStyle));
        dlg.setContentView(R.layout.open_dlg);
        dlg.setTitle(R.string.dlg_open_title);
        dlg.setCancelable(true);

        // File file = new File(m_fileDir, fname);
        // TODO - set dialog title to show date of last save, file size, etc.

        final EditText filenameETxt = (EditText) dlg.findViewById(R.id.open_filename);
        filenameETxt.setText(fname);
        filenameETxt.setOnFocusChangeListener(new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus)
                    UiUtil.hideSoftKeyboard(view);
            }
        });

        m_managePwd = new UiPasswordManager(prefs, dlg, true);
        setHint(fname);

        Button btn_browse = (Button) dlg.findViewById(R.id.file_browser);
        btn_browse.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                m_fileOpenDialog = new SimpleFileDialog(m_context, "FileOpen",
                        m_context.getWindow().getDecorView().getHeight(),
                        new SimpleFileDialog.SimpleFileDialogListener() {
                            @Override
                            public void onChosenDir(String chosenDir) {
                                filenameETxt.setText(chosenDir.replaceAll(".*/", "").replace(DOC_EXT, ""));
                            }
                        });

                m_fileOpenDialog.DefaultFileName = new File(STORAGE_DIR, fname).getPath();
                m_fileOpenDialog.FilePattern = ".+\\" + DOC_EXT;
                m_fileOpenDialog.choose(STORAGE_DIR.getPath());
                setHint(fname);
            }
        });

        Button btn_ok = (Button) dlg.findViewById(R.id.okBtn);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            @SuppressLint("DefaultLocale")
            public void onClick(View v) {
                EditText pwdText = (EditText) dlg.findViewById(R.id.pwd);
                String pwd = pwdText.getText().toString();
                String filename = filenameETxt.getText().toString().trim();
                File file = new File(STORAGE_DIR, filename + DOC_EXT);
                Date lastModified = Util.getCurrentDate();
                if (file.exists())
                    lastModified = new Date(file.lastModified());

                try {
                    loadFile(filename, pwd, docText, sendMsg);
                } catch (Doc.DocPasswordException e) {
                    WebDialog.show(m_context, WebDialog.HTML_CENTER_BOX, 
                            String.format("<center><h2>%s</h2><table frame='box'><tr><td>File:<td>%s<tr><td>Modified:<td>%s<tr><td>Size:<td>%s</table>",  
                            e.getMessage(), filename, m_dateFormat.format(lastModified), String.format("%,d", file.length())));
                    sendMsg.send(SendMsg.MSG_FAIL);
                    return;
                } catch (Exception e) {
                    String exName = e.getClass().getName().replaceAll(".*\\.", "");
                    String exMsg = e.getMessage();
                    if (!TextUtils.isEmpty(exMsg))
                        exMsg = exMsg.replaceAll(".*\\(", "(");
                    YesNoDialog.showOk(m_context, exName + "\n File: " + filename + "\n\n" + exMsg);
                    WebDialog.show(m_context, WebDialog.HTML_CENTER_BOX, 
                            String.format("<center><h2>%s</h2><table frame='box'><tr><td>File:<td>%s</table><p>%s",  
                            e.getMessage(), filename, exMsg));
                    sendMsg.send(SendMsg.MSG_FAIL);
                    return;
                }
                dlg.dismiss();
            }
        });

        dlg.findViewById(R.id.cancelBtn).setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    /**
     * Load decrypted text from permanent file storage into text object.
     * Send message on completion.
     * 
     * @param fname
     *            file to load from storage (no path and no extension)
     * @param pwd
     *            password to decrypt file
     * @throws java.io.FileNotFoundException
     * @throws Doc.DocPasswordException
     * @throws java.io.IOException
     * @throws Doc.DocException
     */
    private void loadFile(final String fname, String pwd, final EditText docText, final SendMsg sendMsg) 
            throws IOException, Doc.DocException {
        Doc doc = new Doc();
        doc.doOpen(new File(STORAGE_DIR, fname + DOC_EXT), pwd);

        Doc.DocMetadata docMetadata = doc.getDocMetadata();
        docMetadata.copyTo(m_docMetadata);
        docMetadata = null;
        
        // Set state before docText, because docText has 'textChanged' listener
        // to update the Title bar.
        m_docMetadata.modified = false;  
        m_docMetadata.filename = new File(m_docMetadata.filename).getName();
        UiUtil.setText(docText, doc.getText());
        docText.setSelection(m_docMetadata.caretPosition, m_docMetadata.caretPosition);

        if (sendMsg != null)
            sendMsg.send(SendMsg.MSG_OKAY);
    }

    /**
     * Get Hint string from document.
     * @param filename
     * @return Hint string or ""
     */
    String getHint(final String filename) {
        Doc doc = new Doc();
        try {
            doc.doOpen(new File(STORAGE_DIR, filename + DOC_EXT), null);
        } catch (Exception ex) {
            return "";
        }
        return doc.getHint();
    }

    // -------------------------------------------------------------------------------------------
    //                                     S A V E    S e c t i o n
    // -------------------------------------------------------------------------------------------
    public static final int SAVE = 0;
    public static final int SAVE_AS = 1;
    
    /**
     *  Show "save as" dialog"
     *   o Browser existing files or user entered filename.
     *   o Collect password (grid pattern or text field)
     *   o Save encrypted file to storage.
     *   
     *  Process is asynchronous, see SendMsg for indication of success/failure.
     */
    public void showSaveAs(
            final EncrypPrefs prefs, int saveMode, final int saveMsgNum, final EditText docText, final SendMsg sendMsg) {
        final Dialog dlg = new Dialog(new ContextThemeWrapper(m_context, R.style.SaveDialogStyle));
        dlg.setContentView(R.layout.save_dlg);
        dlg.setTitle(R.string.dlg_save_title);
        dlg.setCancelable(true);

        m_managePwd = new UiPasswordManager(prefs, dlg, false);
        this.m_managePwd.showHint();


        final EditText filenameETxt = UiUtil.viewById(dlg, R.id.save_filename);

        filenameETxt.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                setFilenameColor(filenameETxt);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        if (!TextUtils.isEmpty(m_docMetadata.filename)) 
            setFilename(filenameETxt, getName());
        
        Button btn_browse = UiUtil.viewById(dlg, R.id.file_browser);
        btn_browse.setVisibility(saveMode == SAVE ? View.GONE : View.VISIBLE);
        btn_browse.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                m_fileOpenDialog = new SimpleFileDialog(m_context, "FileSave",
                        m_context.getWindow().getDecorView().getHeight(), 
                        new SimpleFileDialog.SimpleFileDialogListener() {
                            @Override
                            public void onChosenDir(String chosenDir) {
                                setFilename(filenameETxt, chosenDir);
                            }
                        });

                String filename = filenameETxt.getText().toString().trim();
                if (TextUtils.isEmpty(filename))
                    filename = m_docMetadata.filename;
                if (TextUtils.isEmpty(filename))
                    filename = "new" + DOC_EXT;

                setFilename(filenameETxt, filename);
                m_fileOpenDialog.DefaultFileName = new File(STORAGE_DIR, filename).getPath(); // "*"
                                                                                               // +
                                                                                               // DOC_EXT;
                m_fileOpenDialog.FilePattern = ".+\\" + DOC_EXT;
                m_fileOpenDialog.choose(STORAGE_DIR.getPath());
            }
        });

        Button btn_ok = UiUtil.viewById(dlg, R.id.okBtn);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String filename, pwd;

                filename = filenameETxt.getText().toString().trim();

                if (filename.length() == 0 || filename.startsWith(".")
                        || filename.indexOf('/') != -1) {
                    YesNoDialog.showOk(m_context, m_context.getResources().getString(R.string.invalid_filename));
                    sendMsg.send(SendMsg.MSG_FAIL);
                    return;
                }
                if (!filename.endsWith(DOC_EXT))
                    filename = filename + DOC_EXT;

                if (m_managePwd.isValid()) {
                    pwd = m_managePwd.getPwdView().getText().toString();
                    if (pwd.length() == 0) {
                        YesNoDialog.showOk(m_context, m_context.getResources().getString(R.string.pwd_empty));
                        sendMsg.send(SendMsg.MSG_FAIL);
                        return;
                    }

                    String hint = m_managePwd.getHint();

                    dlg.dismiss();
                    if (saveFile(filename, pwd, hint, docText, sendMsg)) {
                        YesNoDialog.showOk(m_context, m_context.getResources().getString(R.string.file_saved, filename), saveMsgNum);
                    }
                } else {
                    YesNoDialog.showOk(m_context, m_context.getResources().getString(R.string.pwd_nomatch));
                    sendMsg.send(SendMsg.MSG_FAIL);
                }

            }
        });

        dlg.findViewById(R.id.cancelBtn).setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                dlg.dismiss();
            }
        });
        dlg.show();
    }
    
    public void setFilename(final EditText editText, final String filename) {
        editText.setText(filename.replaceAll(".*/", "").replace(DOC_EXT, ""));
        setFilenameColor(editText);
    }

    public  void setFilenameColor(final EditText editText) {
        File file = new File(STORAGE_DIR, editText.getText().toString() + DOC_EXT);
        editText.setTextColor(file.exists() ? Color.RED : Color.BLACK);
    }


    /**
     * Save current text encrypted into permanent file storage.
     *
     * @param filename
     *            save to filename else use <b>doc meta</b> filename.
     * @param pwd
     *            encrypt doc with password
     * @param hint
     *            password hint
     * @param docText
     *
     * Successful saving will be processed synchronously, failure will complete async.
     * @return {@code true} if file saved and send message.
     */
    public boolean saveFile(String filename, String pwd, String hint, EditText docText, final SendMsg sendMsg) {

        if (!STORAGE_DIR.canWrite()) {
            YesNoDialog.showOk(m_context, "Apparently I cannot write to " + STORAGE_DIR.getAbsolutePath());
            sendMsg.send(SendMsg.MSG_FAIL);
            return false;
        }

        m_docMetadata.caretPosition = docText.getSelectionStart();

    //    if (hint != null)
    //        m_docMetadata.hint = hint;

        // Use new filename if provided
        if (filename == null)
            filename = m_docMetadata.filename;
        else
            m_docMetadata.filename = filename;

        // Use new password if provided
        if (!TextUtils.isEmpty(pwd))
            m_docMetadata.setKey(pwd);

        if (hint == null)
            hint = m_docMetadata.hint;
        else
            m_docMetadata.hint = hint;

        Doc doc = new Doc(docText.getText().toString(), m_docMetadata);

        File saveToFile = new File(STORAGE_DIR, filename);

        if (saveToFile.exists())
            saveToFile.delete();

        try {
            doc.doSave(saveToFile, hint);
        } catch (IOException e) {
            e.printStackTrace();
            YesNoDialog.showOk(m_context, e.toString() + " saving \"" + saveToFile.getAbsolutePath() + "\": "
                    + e.getMessage());
            sendMsg.send(SendMsg.MSG_FAIL);
            return false;
        } catch (Doc.DocException e) {
            e.printStackTrace();
            YesNoDialog.showOk(m_context, e.toString() + " saving \"" + saveToFile.getAbsolutePath() + "\": "
                    + e.getMessage());
            sendMsg.send(SendMsg.MSG_FAIL);
            return false;
        }

        m_docMetadata.modified = false;
        sendMsg.send(SendMsg.MSG_OKAY);
        return true;
    }

    public boolean saveOnBackground(String filename, String pwd, String hint, EditText docText) {
        boolean saved = false;

        if (STORAGE_DIR.canWrite()) {
            m_docMetadata.caretPosition = docText.getSelectionStart();

            m_docMetadata.filename = filename;
            m_docMetadata.setKey(pwd);
            m_docMetadata.hint = hint;

            Doc doc = new Doc(docText.getText().toString(), m_docMetadata);

            File saveToFile = new File(STORAGE_DIR, filename);

            if (saveToFile.exists())
                saveToFile.delete();

            try {
                doc.doSave(saveToFile, hint);
                m_docMetadata.modified = false;
                saved = true;
            } catch (Exception ex) {
            }
        }

        return saved;
    }
}
