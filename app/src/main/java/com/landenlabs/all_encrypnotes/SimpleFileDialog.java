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
*
* http://www.scorchworks.com/Blog/simple-file-dialog-for-android-applications/
*
* This file is licensed under The Code Project Open License (CPOL) 1.02
* http://www.codeproject.com/info/cpol10.aspx
* http://www.codeproject.com/info/CPOL.zip
*
* License Preamble:
* This License governs Your use of the Work. This License is intended to allow developers to use the Source
* Code and Executable Files provided as part of the Work in any application in any form.
*
* The main points subject to the terms of the License are:
*    Source Code and Executable Files can be used in commercial applications;
*    Source Code and Executable Files can be redistributed; and
*    Source Code can be modified to create derivative works.
*    No claim of suitability, guarantee, or any warranty whatsoever is provided. The software is provided "as-is".
*    The Article(s) accompanying the Work may not be distributed or republished without the Author's consent
*
* This License is entered between You, the individual or other entity reading or otherwise making use of
* the Work licensed pursuant to this License and the individual or other entity which offers the Work
* under the terms of this License ("Author").
*  (See Links above for full license text)
*
*
*   @author Dennis Lang (modified original work)
*   @see <a href="http://landenlabs.com">http://landenlabs.com</a>
*/

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.landenlabs.all_encrypnotes.ui.UiUtil;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleFileDialog {

    // Default file or directory.
    public String DefaultFileName = "default.txt";

    public boolean ShowDirs = false;
    public String DirPattern = ".+";

    public boolean ShowFiles = true;
    public String FilePattern = ".+";   // .+\..+
    
    public boolean ShowExt = false;
    public DateFormat DateFmt = DateFormat.getDateInstance();
    
    private int FileOpen = 0;
    private int FileSave = 1;
    private int FolderChoose = 2;

    private int m_selectType = FileSave;
    private String m_sdcardDirectory = "";
    private Context m_context;
    private TextView m_titleView1;
    private TextView m_titleView;

    private String m_selectedFileName = DefaultFileName;
    private EditText m_inputText;

    private String m_dir = "";
    // private String m_ext = "";
    private List<String> m_subdirs = null;
    private SimpleFileDialogListener m_simpleFileDialogListener = null;
    private ArrayAdapter<String> m_listAdapter = null;
    private int m_dialogHeight;
    
    // Callback interface for selected directory
    public interface SimpleFileDialogListener {
        void onChosenDir(String chosenDir);
    }

    // Constructor for File/Dir selection dialog.
    public SimpleFileDialog(Context context, String file_select_type, int dialogHeight,
    		SimpleFileDialogListener SimpleFileDialogListener) {
    	
        m_dialogHeight = dialogHeight;
        if (file_select_type.equals("FolderChoose")) {
            m_selectType = FolderChoose;
            ShowFiles = false;
            ShowDirs = true;
        } else if (file_select_type.equals("FileSave")) {
            m_selectType = FileSave;
            ShowFiles = true;
            ShowDirs = false;
        }  else {     //  "FileOpen"
            m_selectType = FileOpen;
            ShowFiles = true;
            ShowDirs = false;
        }

        m_context = context;
        m_sdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        m_simpleFileDialogListener = SimpleFileDialogListener;
    }

    /**
     * Open (show) file or directory selection dialog.
     *
     * @param dir
     *          Starting directory
     *          If null or empty start on SD card directory.
     */
    public void choose(String dir) {
        if (dir == null || dir.isEmpty())
            dir = m_sdcardDirectory;

        File dirFile = new File(dir);
        if (!dirFile.exists() || !dirFile.isDirectory())
            dir = m_sdcardDirectory;

        try {
            dir = new File(dir).getCanonicalPath();
        } catch (IOException ioe) {
            return;
        }

        m_dir = dir;
        m_subdirs = getDirList(dir);

        class SimpleFileDialogOnClickListener implements OnClickListener {
            public void onClick(DialogInterface dialog, int itemIdx) {
                String orgDir = m_dir;
                String sel = "" + ((AlertDialog) dialog).getListView().getAdapter().getItem(itemIdx);
                if (sel.charAt(0) == '/')
                    sel = sel.substring(1, sel.length());

                // Navigate into the sub-directory
                if (sel.equals("..")) {
                    m_dir = m_dir.substring(0, m_dir.lastIndexOf("/"));
                } else {
                    m_dir += "/" + sel;
                }
                m_selectedFileName = DefaultFileName;

                if ((new File(m_dir).isFile())) // If the selection is a regular file
                {
                    m_dir = orgDir;
                    m_selectedFileName = sel;
                }

                updateDirectory();
            }
        }

        AlertDialog.Builder dialogBuilder = createDirectoryChooserDialog(dir, m_subdirs,
                new SimpleFileDialogOnClickListener());

        dialogBuilder.setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Current directory chosen
                // Call registered listener supplied with the chosen directory
                if (m_simpleFileDialogListener != null) {
                    {
                        if (m_selectType == FileOpen || m_selectType == FileSave) {
                            m_selectedFileName = m_inputText.getText() + "";
                            m_simpleFileDialogListener.onChosenDir(m_dir + "/" + m_selectedFileName);
                        } else {
                            m_simpleFileDialogListener.onChosenDir(m_dir);
                        }
                    }
                }
            }
        }).setNegativeButton("Cancel", null);

        final AlertDialog dirsDialog = dialogBuilder.create();

        // Show directory chooser dialog
        dirsDialog.show();
    }

    private boolean createSubDir(String newDir) {
        File newDirFile = new File(newDir);
        if (!newDirFile.exists()) return newDirFile.mkdir();
        else return false;
    }

    /**
     * Get Directory List (optionally subDir and/or files)
     * See ShowFiles, FilePattern, ShowDir, DirPattern
     *
     * @param dir
     *          Start directory scan at 'dir'
     * @return
     *          List of directory entries (files and/or directories)
     */
    private List<String> getDirList(String dir) {
        List<String> dirs = new ArrayList<String>();
        try {
            File dirFile = new File(dir);

            // If directory is not the base sd card directory add ".." for going up one directory
            if (!m_dir.equals(m_sdcardDirectory) && ShowDirs)
                dirs.add("..");

            for (File file : dirFile.listFiles()) {
                if (file.isDirectory()) {
                    if (ShowDirs && file.getName().matches(DirPattern)) {
                        // Add "/" to directory names to identify them in the list
                        dirs.add("/" + file.getName());
                    }
                } else if (m_selectType == FileSave || m_selectType == FileOpen) {
                    // Add file names to the list if we are doing a file save or file open operation
                    if (ShowFiles && file.getName().matches(FilePattern)) {
                        dirs.add(file.getName());
                    }
                }
            }
        } catch (Exception e) {
        }

        Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);
        return dirs;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////                                   START DIALOG DEFINITION                                    //////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    static final int DARK_GRAY = 0xff444444;
    
    private AlertDialog.Builder createDirectoryChooserDialog(
    		String title, List<String> listItems, OnClickListener onClickListener) {
    	
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(m_context);
        
        // Create title text showing file select type //
        m_titleView1 = new TextView(m_context);
        m_titleView1.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        //m_titleView1.setTextAppearance(m_context, android.R.style.TextAppearance_Large);
        //m_titleView1.setTextColor( m_context.getResources().getColor(android.R.color.black) );

        if (m_selectType == FileOpen)
            m_titleView1.setText("Open:");
        if (m_selectType == FileSave)
            m_titleView1.setText("Save As:");
        if (m_selectType == FolderChoose)
            m_titleView1.setText("Folder Select:");

        // Need to make this a variable Save as, Open, Select Directory
        m_titleView1.setGravity(Gravity.CENTER_VERTICAL);
        m_titleView1.setBackgroundColor(DARK_GRAY);
        //noinspection deprecation
        m_titleView1.setTextColor(m_context.getResources().getColor(android.R.color.white));

        // Create custom view for AlertDialog title
        LinearLayout titleLayout1 = new LinearLayout(m_context);
        titleLayout1.setOrientation(LinearLayout.VERTICAL);
        titleLayout1.addView(m_titleView1);

        if (ShowDirs && (m_selectType == FolderChoose || m_selectType == FileSave)) {
        	
            // ----Create New Folder Button   
            Button newDirButton = new Button(m_context);
            newDirButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            newDirButton.setText("New Folder");
            newDirButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final EditText input = new EditText(m_context);

                        // Show new folder name input dialog
                        new AlertDialog.Builder(m_context).
                                setTitle("New Folder Name").
                                setView(input).setPositiveButton("OK", new OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Editable newDir = input.getText();
                                String newDirName = newDir.toString();
                                // Create new directory
                                if (createSubDir(m_dir + "/" + newDirName)) {
                                    // Navigate into the new directory
                                    m_dir += "/" + newDirName;
                                    updateDirectory();
                                } else {
                                    Toast.makeText(m_context, "Failed to create '"
                                            + newDirName + "' folder", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).setNegativeButton("Cancel", null).show();
                    }
                }
            );
            titleLayout1.addView(newDirButton);
        }

        // ---- Create View with folder path and entry text box  
        LinearLayout titleLayout = new LinearLayout(m_context);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        m_titleView = new TextView(m_context);
        m_titleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        m_titleView.setBackgroundColor(DARK_GRAY);
        //noinspection deprecation
        m_titleView.setTextColor(m_context.getResources().getColor(android.R.color.white));
        m_titleView.setGravity(Gravity.CENTER_VERTICAL);
        m_titleView.setText(title);

        titleLayout.addView(m_titleView);

        if (m_selectType == FileOpen || m_selectType == FileSave) {
            m_inputText = new EditText(m_context);
            m_inputText.setText(DefaultFileName.replaceAll(".*/", ""));
            titleLayout.addView(m_inputText);
        }
        
        // ---- Set Views and Finish Dialog builder   
        dialogBuilder.setView(titleLayout);
        dialogBuilder.setCustomTitle(titleLayout1);
        m_listAdapter = createListAdapter(listItems);
        dialogBuilder.setSingleChoiceItems(m_listAdapter, -1, onClickListener);
        dialogBuilder.setCancelable(false);
        return dialogBuilder;
    }

    private void updateDirectory() {
        m_subdirs.clear();
        m_subdirs.addAll(getDirList(m_dir));
        m_titleView.setText(m_dir);
        m_listAdapter.notifyDataSetChanged();

        if (m_selectType == FileSave || m_selectType == FileOpen) {
            m_inputText.setText(m_selectedFileName);
        }
    }

    private ArrayAdapter<String> createListAdapter(List<String> items) {
        return new ArrayAdapter<String>(m_context, R.layout.file_list_row, R.id.fl_name, items) {
            @SuppressLint("DefaultLocale")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemView = super.getView(position, convertView, parent);
                String item = getItem(position);
                
                int rowHeight = m_dialogHeight / (this.getCount()+2);
                if (itemView.getHeight() < rowHeight) {
                    AbsListView.LayoutParams params = (AbsListView.LayoutParams) itemView.getLayoutParams();
                    params.height = Math.min(params.height*2, rowHeight);
                    itemView.setLayoutParams(params);
                }
                
                File file = new File(m_dir, item);

                TextView nameTv = UiUtil.viewById(itemView, R.id.fl_name);
                TextView dateTv = UiUtil.viewById(itemView, R.id.fl_date);
                TextView sizeTv = UiUtil.viewById(itemView, R.id.fl_size);
                TextView hintTv = UiUtil.viewById(itemView, R.id.fl_hint);
                TextView vernTv = UiUtil.viewById(itemView, R.id.fl_version);
                
                if (!ShowExt)
                    nameTv.setText(item.replaceAll("\\..*", ""));
                
                if (file.exists()) {
                    // Get Hint and version.
                    String hint = "";
                    String version = "";
                    Doc doc = new Doc();
                    try {
                        doc.doOpen(file, null);
                        hint = doc.getHint();
                        version = doc.getVersion();
                    } catch (Exception ex) {
                    }

                    dateTv.setText(DateFmt.format(file.lastModified()));
                    sizeTv.setText(String.format("%,d", file.length()));
                    vernTv.setText(version);
                    hintTv.setText(hint);
                } else {
                    dateTv.setText("");
                    sizeTv.setText("");
                    vernTv.setText("");
                    hintTv.setText("");
                }
                
                return itemView;
            }
        };
    }
}
