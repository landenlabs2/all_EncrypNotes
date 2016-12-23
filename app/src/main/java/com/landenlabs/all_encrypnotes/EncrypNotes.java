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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.landenlabs.all_encrypnotes.ui.DlgClickListener;
import com.landenlabs.all_encrypnotes.ui.Email;
import com.landenlabs.all_encrypnotes.ui.HomeWatcher;
import com.landenlabs.all_encrypnotes.ui.LogIt;
import com.landenlabs.all_encrypnotes.ui.RenameDialog;
import com.landenlabs.all_encrypnotes.ui.SliderDialog;
import com.landenlabs.all_encrypnotes.ui.UiUtil;
import com.landenlabs.all_encrypnotes.ui.WebDialog;
import com.landenlabs.all_encrypnotes.ui.YesNoDialog;
import com.landenlabs.all_encrypnotes.util.AppCrash;
import com.landenlabs.all_encrypnotes.util.GoogleAnalyticsHelper;

/**
 * Encrypted Notepad based off work from Ivan Voras
 * 
 * @author Dennis Lang <br>
 *         WebSite: {@link http://home.comcast.net/~lang.dennis/}
 *         <p>
 * @author Ivan Voras <br>
 *         WebSite: {@link http://sourceforge.net/projects/enotes/}
 *
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 * @version 1.3
 * @since 2014-Nov-25
 */
public class EncrypNotes extends Activity implements DlgClickListener, OnSeekBarChangeListener  {

    // -- Internal helper objects
    private final EncrypPrefs m_prefs = new EncrypPrefs(this);
    private final DocFileDlg m_docFileDialog = new DocFileDlg(this);
    private final UiSplashScreen m_splashScreen = new UiSplashScreen(this);
    private final HomeWatcher m_homeWatcher = new HomeWatcher(this);
    
    // -- UI view objects
    private float      m_mainTextSize;
    private View       m_titleBar;
    private ScrollView m_mainScroll;
    private EditText   m_mainText;
    private MenuItem   m_menuParanoid;
    private MenuItem   m_menuGlobalPwd;
    private MenuItem   m_menuInvertBg;

    public static final int HNDMSG_LOAD_DONE = 1;
    public static final int HNDMSG_SAVE_DONE = 2;
    private final Handler m_handler = new Handler() {

        public void handleMessage(Message msg) {
             
            switch (msg.what) {
            case HNDMSG_LOAD_DONE:
                updateTitle();
                break;
            case HNDMSG_SAVE_DONE:
                updateTitle();
                break;
            }
        }
    };

    // Send message when Document file load is done  (or fails, see arg1 for state).
    public class SendLoadDoneMsg implements SendMsg {
        public void send(int msgNum) {
            m_handler.sendMessage(m_handler.obtainMessage(HNDMSG_LOAD_DONE, msgNum, 0));
        }
    }
    // Send message when Document file save is done (or fails, see arg1 for state).
    public class SendSaveDoneMsg implements SendMsg {
        public void send(int msgNum) {
            m_handler.sendMessage(m_handler.obtainMessage(HNDMSG_SAVE_DONE, msgNum, 0));
        }
    }
    
    private SendLoadDoneMsg mSendLoadDoneMsg = new SendLoadDoneMsg();
    private SendSaveDoneMsg mSendSaveDoneMsg = new SendSaveDoneMsg();

    private GoogleAnalyticsHelper mAnalytics;

    // ========================================================================
    // Activity overrides

    /**
     * Creation of activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Prevent screen capture, must call before super.onCreate()
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        boolean DEBUG = (getApplicationInfo().flags & 2) != 0;
        // new Util.UncaughtExceptionHandler();
        AppCrash.initalize(getApplication(), DEBUG);
        mAnalytics = new GoogleAnalyticsHelper(getApplication(), DEBUG);

        m_mainText = (EditText) this.findViewById(R.id.main_text);
        m_mainTextSize = m_mainText.getTextSize();
        m_mainScroll = (ScrollView) this.findViewById(R.id.main_scroll);

        LogIt.setDebugMode(getApplicationInfo());

        if (Util.fileExists(EncrypPrefs.PREFS_FILENAME))
            loadPrefs();

        updateTitle();
        setupUI();

        ensureDocDir();
        m_splashScreen.show(); 
    }

    /**
     * Create option menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);

        // Save off checkable menu items.
        m_menuParanoid  = menu.findItem(R.id.menu_paranoid);
        m_menuGlobalPwd = menu.findItem(R.id.menu_global_pwd);
        m_menuInvertBg  = menu.findItem(R.id.menu_invert);

        updateMenu();

        return super.onCreateOptionsMenu(menu);
    }
    
/* *****************
    // ** See associated code in DocFileDlg [context_menu]
    // Context menu on open file list does not work.

    / **
     * Create context menu - Load File list (delete, etc)
     * /
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle("Options");
        mode.getMenuInflater().inflate(R.menu.file_context_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view.getTag() != null
                && view.getClass().getClass() == ListView.class.getClass()
                && (Integer)view.getTag() == DocFileDlg.FILE_CONTEXT_MENU) {
            ListView listView = (ListView)view;

            // Brute force - getSelectedItem();
            Object obj = null;
            for (int idx = 0; idx != listView.getChildCount(); idx++) {
                if (listView.getChildAt(idx).isSelected()) {
                    obj = listView.getAdapter().getItem(idx);
                    break;
                }
            }

            if (obj != null) {
                String selItem = obj.toString();
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.file_context_menu, menu);
                menu.setHeaderTitle(selItem);
            }
        }
    }
    
   / **
     * Long click on file load list - context menu 
     * http://stackoverflow.com/questions/9211545/android-context-menu-listview-for-files
     * This does not fire - web says it may go to onMenuItemSelected
     * /
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.file_delete:
                // File file = new File(m_storageDir + m_contextListView.getAdapter().getItem(info.position).toString() + DOC_EXT);
                // boolean deleted = file.delete();
                return true;
        }

        return super.onContextItemSelected(item);
    }
 
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
        case R.id.file_delete:
            // File file = new File(m_storageDir + m_contextListView.getAdapter().getItem(info.position).toString() + DOC_EXT);
            // boolean deleted = file.delete();
            return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }

 ***************** */
    
    /**
     * Handle option menu selections.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            
        case R.id.menu_new:
            newFile();
            return true;
            
        case R.id.menu_open:
            loadFile();
            return true;
            
        case R.id.menu_save:
            saveFileUI(DocFileDlg.SAVE);
            return true;
            
        case R.id.menu_save_as:
            saveFileUI(DocFileDlg.SAVE_AS);
            return true;

        case R.id.menu_email:
            if (m_mainText.getText().length() == 0) {
                WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Nothing to save</h2>");
                return false;
            }
            Email.send(this, "to@gmail.com", "EncrypNotes", m_mainText.getText().toString());
            return true;

        case R.id.menu_about:
            m_splashScreen.show();
            aboutBox();
            return true;

        case R.id.menu_paranoid:
            m_prefs.Paranoid = !m_prefs.Paranoid;
            item.setChecked(m_prefs.Paranoid);
            return true;

        case R.id.menu_global_pwd:
            // m_prefs.Global_pwd_state = !m_prefs.Global_pwd_state;
            // item.setChecked(m_prefs.Global_pwd_state);
            // if (item.isChecked()) {
                getGlobalPwdState(item);
            // }
            return true;

        case R.id.menu_file_browser:
            openFileBrowser();
            return true;
            
        case R.id.menu_invert:
            m_prefs.InvertBg = !m_prefs.InvertBg;
            item.setChecked(m_prefs.InvertBg);
            updateBg();
            return true;

        case R.id.menu_zoom:
            SliderDialog sliderDlg = SliderDialog.create(R.layout.font_zoom_dlg, PRGMSG_FONT_ZOOM);
            sliderDlg.show(getFragmentManager(), "font_zoom");
            return true;

        case R.id.menu_copy:
        case R.id.menu_paste:
        case R.id.menu_cut:
        case R.id.menu_clear:
            ClipboardManager cMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            switch (item.getItemId()) {
            case R.id.menu_copy:
                if (m_mainText.length() != 0) {
                    ClipData clip = ClipData.newPlainText("simple text", m_mainText.getText());
                    cMan.setPrimaryClip(clip);
                    YesNoDialog.showOk(this, "Copied " + m_mainText.length() + " characters");
                }
                return true;

            case R.id.menu_paste:
                if (cMan.getPrimaryClip() != null && cMan.getPrimaryClip().getItemCount() != 0) {
                    String pasteStr = cMan.getPrimaryClip().getItemAt(0).getText().toString();
                    int beg = m_mainText.getSelectionStart();
                    int end = m_mainText.getSelectionEnd();
                    if (beg != -1 && end != -1) {
                        m_mainText.setText(m_mainText.getText().replace(beg, end, pasteStr));
                    } else {
                        m_mainText.setText(m_mainText.getText() + pasteStr);
                    }
                }
                return true;

            case R.id.menu_cut:
                if (m_mainText.length() != 0) {
                    int beg = m_mainText.getSelectionStart();
                    int end = m_mainText.getSelectionEnd();
                    beg = (beg == -1) ? 0 : beg;
                    end = (end == -1) ? m_mainText.length() : end;
                    ClipData clip = ClipData.newPlainText("simple text", m_mainText.getText()
                            .subSequence(beg, end));
                    cMan.setPrimaryClip(clip);
                    m_mainText.setText(m_mainText.getText().replace(beg, end, ""));
                }
                return true;

            case R.id.menu_clear:
                if (m_mainText.length() != 0) {
                    ClipData clip = ClipData.newPlainText("simple text", m_mainText.getText());
                    cMan.setPrimaryClip(clip);
                    m_mainText.setText("");
                }

                return true;
            }

        case R.id.menu_info:
            m_docFileDialog.showInfo();
            break;
        }
        return false;
    }

    private static int m_startDepthCnt = 0;
    @Override
    protected void onStart() {
        super.onStart();
        new Util.UncaughtExceptionHandler();
        m_startDepthCnt++;

        m_homeWatcher.startWatch();
    }

    public void onStop() {
        m_startDepthCnt--;
        m_homeWatcher.stopWatch();
        super.onStop();
    }
    
    public void onPause() {
        saveIfNeeded(false);
        m_prefs.save();

        // Clear screen before saving - so thumbnail does not contain text.
        // This does not seem to work, so used FLAG_SECURE in onCreate()
        m_mainText.setVisibility(View.INVISIBLE);
        super.onPause();
    }
    
    public void onResume() {
        m_mainText.setVisibility(View.VISIBLE);
        super.onResume();
    }

    /*
     * The problem: already saved files can be saved since we know both the filename and the
     * password. But new documents which are not yet saved have no such data associated with them.
     * We shall thus save these documents in internal memory in plaintext and hope it is secure
     * enough. We must also kill this saved data when we finally save the document.
     */
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        m_prefs.save();
        if (!m_prefs.Paranoid) {
            m_docFileDialog.saveInstanceState(bundle, m_mainText);
            saveIfNeeded(false);
        }

        super.onSaveInstanceState(bundle);
    }

    /*
     * Only called if starting app after os killed app (ex: too many backgrounded apps).
     * If not paranoid, restore state.
     */
    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        m_prefs.load();
        if (m_prefs.Paranoid)
            return;
        m_docFileDialog.restoreInstanceState(bundle, m_mainText);
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        String exitMsg = m_docFileDialog.isModified() ? "Exit without saving ?" : "Exit ?";
        YesNoDialog.showDialog(this, "", exitMsg, CLKMSG_EXIT, YesNoDialog.BTN_YES_NO);
        // WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "Exit ?");
        // TODO - change to webdialog, make it nicer exit dialog.
        // WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Exit ?</h2>");
    }

// ========================================================================
    // DlgClickListener  overrides

    @Override
    public void onClick(DialogFragment dialog, int whichMsg) {
        switch (whichMsg) {
        case CLKMSG_EXIT:
            this.finish();  // Fatal error - time to exit or backpress.
            break;
        case -CLKMSG_EXIT:
                break;
        case CLKMSG_NEW_FILE:
            newFile();      // Save any existing, setup new file.
            break;
        case CLKMSG_SAVE_THEN_NEW:
            if (!saveIfNeeded(true))
                return;
        case -CLKMSG_SAVE_THEN_NEW:
            m_docFileDialog.Clear();
            UiUtil.setText(m_mainText, "");
            updateTitle();
            break;
        case CLKMSG_SAVE_THEN_OPEN:
            if (!saveIfNeeded(true))
                return;
        case -CLKMSG_SAVE_THEN_OPEN:
            m_docFileDialog.showLoad(m_prefs, m_mainText, mSendLoadDoneMsg);
            break;
        case CLKMSG_FILENAME_CHANGED:
            updateTitle();
            break;
        case R.id.file_delete:
            {
            YesNoDialog yesNoDialog = (YesNoDialog) dialog;
            String filename = (String) yesNoDialog.getValue();
            FileListAdapter fileListAdapter = (FileListAdapter) yesNoDialog.getViewer();
            fileListAdapter.deleteFile(filename);
            }
            break;
        case R.id.file_rename:
            {
            RenameDialog renameDialog = (RenameDialog) dialog;
            String fromFile = renameDialog.getFrom();
            String toFile = renameDialog.getTo();
            FileListAdapter fileListAdapter = (FileListAdapter) renameDialog.getViewer();
            fileListAdapter.renameFile(fromFile, toFile);
            }
            break;
        default:  // CLKMSG_NONE
            break;
        }
    }

    // ========================================================================
    // OnSeekBarChangeListener overrides
    // Font scale

    // OnProgress messages
    static final int PRGMSG_FONT_ZOOM = 100;
    
    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        int value = arg0.getProgress(); // 0...100
        switch (arg1) {
        case PRGMSG_FONT_ZOOM:
            m_prefs.TextScale = (value - 50) / 50.0f; // -1.0f ... +1.0f;
            updateTextSize();
        }
    }

 
    // ========================================================================
    // Activity main logic
    
    /**
     * Setup User interface, add callbacks.
     *   + afterTextChanged (mainText)
     *   + onTouch   (scrollView)
     *   + onClick   (titleBar)
     */
    private void setupUI() {
        m_mainText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // See UiUtil.setText, trick set to disable to indicate no text watcher. 
                if (m_mainText.isEnabled()) {
                    if (!m_docFileDialog.isModified()) {
                        m_docFileDialog.setModified(true);
                        m_splashScreen.hide();
                    }
                    updateTitle();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // Hide soft keyboard if user is scrolling for awhile.
        m_mainScroll.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                long downDurationMillis = SystemClock.uptimeMillis() - event.getDownTime();
                if (event.getAction() == MotionEvent.ACTION_MOVE && downDurationMillis > 300) {
                    UiUtil.hideSoftKeyboard(getCurrentFocus());
                }
                return false;
            }
        });

        // Hide soft keyboard if user clicks on action/title bar.
        if (m_titleBar != null)
            m_titleBar.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    UiUtil.hideSoftKeyboard(getCurrentFocus());
                }
            });

    }

    /**
     * Load Preferences saved
     * 
     * @return {@code true} if successful
     */
    private boolean loadPrefs() {
        m_prefs.load();
        updateBg();
        updateTextSize();
        updateMenu();
        return true;
    }

    /**
     * Set dark or lite background.
     */
    @SuppressWarnings("deprecation")
    private void updateBg() {
        if (m_prefs.InvertBg) {
            m_mainText.setBackgroundDrawable(getResources().getDrawable(R.drawable.paper_dark));
            m_mainText.setTextColor(Color.WHITE);
        } else {
            m_mainText.setBackgroundDrawable(getResources().getDrawable(R.drawable.paper_lite));
            m_mainText.setTextColor(Color.BLACK);
        }
    }

    /**
     * Set text size using text scale.
     */
    private void updateTextSize() {
        float adjSize = m_mainTextSize / 2 * m_prefs.TextScale;
        m_mainText.setTextSize(m_mainTextSize + adjSize);
    }

    /**
     * Update menu to match preferences.
     */
    private void updateMenu() {
        m_menuParanoid.setChecked(m_prefs.Paranoid);
        m_menuGlobalPwd.setChecked(m_prefs.Global_pwd_state);
        m_menuInvertBg.setChecked(m_prefs.InvertBg);
    }

    /**
     * Ensure document directory exists.
     */
    private void ensureDocDir() {
        if (m_docFileDialog.ensureDocDir())
            return;
        YesNoDialog.showOk(this, "Cannot make directory " + m_docFileDialog.getDir().getName()
                + "\nEnable write permissions.", CLKMSG_EXIT);
    }

    /**
     * Save active file if needed (does not prompt).
     * @param  isVisible
     * @return  False is need save but unable.
     */
    private boolean saveIfNeeded(boolean isVisible) {
        boolean saved = true;
        if (m_docFileDialog.isModified()) {
            if (isVisible) {
                if (m_docFileDialog.canSave()) {
                    m_docFileDialog.saveFile(null, null, null, m_mainText, mSendSaveDoneMsg);
                } else {
                    WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "Need filename, Use <b>Save As</b> to save");
                    saved = false;
                }
            } else {
                LogIt.log(this.getClass().getSimpleName(), LogIt.WARN, "Going background - unsaved text");
                m_docFileDialog.saveOnBackground("quick_save", m_prefs.Global_pwd_value, m_prefs.Global_pwd_hint, m_mainText);
            }
        }

        return saved;
    }

    /**
     * Prompt to save any active file, then clear buffer for new file.
     */
    private void newFile() {
        if (m_docFileDialog.isModified() && m_mainText.length() != 0) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            YesNoDialog yesNoDialog = YesNoDialog.create("Save Changes", "Save changes ?", CLKMSG_SAVE_THEN_NEW, YesNoDialog.BTN_YES_NO);
            yesNoDialog.show(ft, "msgDialog");
        } else {
            m_docFileDialog.Clear();
            UiUtil.setText(m_mainText, "");
            updateTitle();
        }
    }

    private void loadFile() {
        if (m_docFileDialog.isModified() && m_mainText.length() != 0) {
            YesNoDialog.showDialog(this, "Save Changes", "Save changes ?", CLKMSG_SAVE_THEN_OPEN, YesNoDialog.BTN_YES_NO);
            // FragmentTransaction ft = getFragmentManager().beginTransaction();
            // YesNoDialog yesNoDialog = YesNoDialog.create("Save Changes", "Save changes ?", CLKMSG_SAVE_THEN_OPEN, YesNoDialog.BTN_YES_NO);
            // yesNoDialog.show(ft, "newSaveDialog");
        } else {
            m_docFileDialog.showLoad(m_prefs, m_mainText, mSendLoadDoneMsg);
        }
    }
    
    /**
     * Prompt for file to save text to.
     * 
     * @param saveMode
     *            SAVE or SAVE_AS
     */
    private void saveFileUI(int saveMode) {

        if (m_mainText.getText().length() == 0) {
            WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Nothing to save</h2>");
            return;
        }
        
        if (saveMode == DocFileDlg.SAVE) {
            if (!m_docFileDialog.isModified()) {
                WebDialog.show(this, WebDialog.HTML_CENTER_BOX, "<h2>Already saved, nothing has changed</h2>");
                return;
            }
                
            if (m_docFileDialog.canSave()) {
                // Save with current filename and pwd.
                m_docFileDialog.saveFile(null, null, null, m_mainText, mSendSaveDoneMsg);
                return;
            }
        }

        m_docFileDialog.showSaveAs(m_prefs, DocFileDlg.SAVE_AS, CLKMSG_FILENAME_CHANGED, m_mainText, mSendSaveDoneMsg);
    }

  
    /**
     * Show about information in dialog box.
     * Use html web viewer in AlertDialog.
     */
    private void aboutBox() {
        // wv.loadUrl("file:///android_asset/about.html");
        String htmlStr = String.format(UiUtil.LoadData(this, "about.html"), UiUtil.getPackageInfo(this).versionName,
                Doc.CRYPTO_MODE);
        WebDialog.show(this, WebDialog.HTML_CENTER_BOX, htmlStr);
    }
 
    /* Shows a simple about box dialog */
    @SuppressWarnings("unused")
    private void optionsBox() {
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.globalpwd_state_dlg);
        dlg.setTitle(R.string.dlg_prefs_title);
        dlg.setCancelable(true);

        Button btn_ok = (Button) dlg.findViewById(R.id.dp_btn_ok);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                dlg.dismiss();
            }
        });
        dlg.show();
    }

    /**
     * Manage global password.
     * TODO - complete code.
     */
    private void getGlobalPwdState(final MenuItem item) {
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.globalpwd_state_dlg);
        dlg.setTitle(R.string.dlg_prefs_title);
        dlg.setCancelable(true);

        Button btn_ok = (Button) dlg.findViewById(R.id.dp_btn_ok);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                m_prefs.Global_pwd_state = true;
                item.setChecked(m_prefs.Global_pwd_state);
                m_prefs.save();
                dlg.dismiss();
                getGlobalPwdValue();
            }
        });

        Button btn_cancel = (Button) dlg.findViewById(R.id.dp_btn_cancel);
        btn_cancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                m_prefs.Global_pwd_state = false;
                item.setChecked(m_prefs.Global_pwd_state);
                m_prefs.save();
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    /**
     * Manage global password.
     * TODO - complete code.
     */
    private void getGlobalPwdValue() {
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(R.layout.globalpwd_value_dlg);
        dlg.setTitle(R.string.set_global_password);
        dlg.setCancelable(true);

        final UiPasswordManager managePwd = new UiPasswordManager(m_prefs, dlg, true);
        managePwd.getPwdView().setHint(R.string.global_password);
        if (!TextUtils.isEmpty(m_prefs.Global_pwd_hint))
            managePwd.setHint(m_prefs.Global_pwd_hint);

        Button btn_ok = (Button) dlg.findViewById(R.id.dp_btn_ok);
        btn_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                // EditText hintText = UiUtil.viewById(dlg, R.id.pwd_hint_value);
                EditText pwdText = managePwd.getPwdView();

                m_prefs.Global_pwd_hint = managePwd.getHint();
                m_prefs.Global_pwd_value = pwdText.getText().toString();
                m_prefs.Global_pwd_state = true;
                m_prefs.save();
                dlg.dismiss();
            }
        });

        Button btn_cancel = (Button) dlg.findViewById(R.id.dp_btn_cancel);
        btn_cancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                dlg.dismiss();
            }
        });

        dlg.show();
    }


    // Does not fully work - allows file browser to open, but does not
    // default to our directory.
    private void openFileBrowser() {
        Uri selectedUri = Uri.parse(m_docFileDialog.getDir().getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(selectedUri, "*/*");
        startActivity(intent);
        Toast.makeText(this, selectedUri.getPath(), Toast.LENGTH_LONG).show();
        //  startActivity(Intent.createChooser(intent, "Open folder"));
    }
    
    /**
     * Set app title to open filename, red indicates text has been modified.
     */
    private void updateTitle() {
        final String fname =  TextUtils.isEmpty(m_docFileDialog.getName()) ?
                getResources().getString(R.string.new_document) :
                m_docFileDialog.getName();
                            
        final int lineCnt = m_mainText.getLineCount();
        final int charCnt = m_mainText.length();
        this.setTitle(getResources().getString(R.string.app_title, fname, charCnt));

        if (m_titleBar == null) {
            // Setting background color depends on whether action bar is enabled
            int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
            m_titleBar = findViewById(titleId);
            if (m_titleBar == null) {
                View title = getWindow().findViewById(android.R.id.title);
                m_titleBar = (View) title.getParent();
            }

            // Allow clicking on title bar to hide keyboard.
            if (m_titleBar != null) {
                m_titleBar.setFocusableInTouchMode(true);
            }
        }

        if (m_titleBar != null) {
            if (m_docFileDialog.isModified())
                m_titleBar.setBackgroundColor(Color.RED);
            else
                m_titleBar.setBackgroundColor(Color.BLACK);
        }
    }

}
