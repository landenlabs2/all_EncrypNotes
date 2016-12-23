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

import android.app.Dialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.landenlabs.all_encrypnotes.ui.UiUtil;
import com.landenlabs.password.PasswordGrid;
import com.landenlabs.password.PasswordGrid.OnPasswordListener;


/**
 * Class to manage password UI options.
 * 
 * @author dlang_local
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 * 
 */
class UiPasswordManager
        implements OnFocusChangeListener
        , TextView.OnEditorActionListener
        , TextWatcher
        , CompoundButton.OnCheckedChangeListener
        , View.OnClickListener {

    private final EncrypPrefs m_prefs;
    private final boolean m_openMode;
    private final ViewGroup m_pwdHolder;
    private final EditText m_pwdText;
    private final ViewGroup m_pwdVerifyHolder;
    private final EditText m_pwdVerify;
    private final PasswordGrid m_pwdGrid;
    private final CheckBox m_patternCb;
    private final CheckBox m_showCb;
    private final TextView m_pwdHintLB;
    private final EditText m_pwdHintValue;

    public UiPasswordManager(EncrypPrefs prefs, Dialog dlg, boolean m_openMode) {

        m_prefs = prefs;
        
        this.m_openMode = m_openMode;

        m_pwdHolder = UiUtil.viewById(dlg, R.id.pwd_holder);
        m_pwdText = UiUtil.viewById(dlg, R.id.pwd);
        UiUtil.viewById(dlg, R.id.pwd_clear).setOnClickListener(this);
        UiUtil.viewById(dlg, R.id.pwd_del).setOnClickListener(this);

        m_pwdVerifyHolder = UiUtil.viewById(dlg, R.id.pwd_verify_holder);
        m_pwdVerify = UiUtil.viewById(dlg, R.id.pwd_verify);
        // pwdVerifyLB = UiUtil.viewById(dlg, R.id.pwd_verifyLB);
        UiUtil.viewById(dlg, R.id.pwd_verify_clear).setOnClickListener(this);
        UiUtil.viewById(dlg, R.id.pwd_verify_del).setOnClickListener(this);

        m_pwdGrid = UiUtil.viewById(dlg, R.id.pwd_grid);
        m_patternCb = UiUtil.viewById(dlg, R.id.pwd_patternCB);
        m_showCb = UiUtil.viewById(dlg, R.id.pwd_showCB);
        m_pwdHintLB = UiUtil.viewById(dlg, R.id.pwd_hint_label);
        m_pwdHintValue = UiUtil.viewById(dlg, R.id.pwd_hint_value);

        m_pwdGrid.clear();
        update();

        m_patternCb.setChecked(m_prefs.ShowPat);
        m_showCb.setChecked(m_prefs.ShowPwd);

        m_patternCb.setOnCheckedChangeListener(this);
        m_showCb.setOnCheckedChangeListener(this);

        m_pwdGrid.setListener(new OnPasswordListener() {

            @Override
            public void onPasswordComplete(String s) {
            }

            @Override
            public void onPasswordChanged(String s) {
                if (!s.equals(m_pwdText.getText().toString())) {
                    m_pwdText.setText(s);
                }
            }
        });
        
        
        m_pwdText.setOnFocusChangeListener(this);
        m_pwdText.setOnEditorActionListener(this);
        m_pwdText.addTextChangedListener(this);

        // m_pwdVerify.setOnFocusChangeListener(this);
        // m_pwdVerify.setOnEditorActionListener(this);
        // m_pwdGrid.setOnFocusChangeListener(this);
    }
    
    public EditText getPwdView() {
        return m_pwdText;
    }

    /***
     * Return true if valid password entered.
     *    Return true if Show Pattern or Password enabled
     *    else return true if both passwords entered match.
     *
     * @return true if valid.
     */
    public boolean isValid() {
        return (m_prefs.ShowPwd || m_prefs.ShowPat) ? true : m_pwdVerify.getText().toString()
                .equals(m_pwdText.getText().toString());
    }

    public void update() {
        int vis = m_prefs.ShowPat ? View.VISIBLE : View.GONE;
        m_pwdGrid.setVisibility(vis);

        boolean hide = m_prefs.ShowPat || m_prefs.ShowPwd || m_openMode;

        if (hide)
            m_pwdText.setNextFocusDownId(R.id.okBtn);
        else
            m_pwdText.setNextFocusDownId(R.id.pwd_verify);

        int otherVis =  hide ? View.GONE : View.VISIBLE;
        m_pwdVerifyHolder.setVisibility(otherVis);
        // m_pwdVerify.setVisibility(otherVis);
        // pwdVerifyLB.setVisibility(otherVis);
        // pwdVerifyClear.setVisibility(otherVis);

        m_pwdText.setInputType(InputType.TYPE_CLASS_TEXT
                | (m_prefs.ShowPwd ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_TEXT_VARIATION_PASSWORD));

        // m_pwdText.setFocusable(!m_showPat);
    }

    public void setHint(String hint) {
        m_pwdHintLB.setVisibility(TextUtils.isEmpty(hint) ? View.INVISIBLE : View.VISIBLE);
        m_pwdHintValue.setText(hint);
    }

    public String getHint() {
        return m_pwdHintValue.getText().toString();
    }

    public void showHint() {
        m_pwdHintLB.setVisibility(View.VISIBLE);
    }

    // =====
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus || !(view instanceof EditText)) {
            // if (view == null)
            //    view = getCurrentFocus();
            if (view != null)  
                UiUtil.hideSoftKeyboard(view);
        }
    }

    // =====
    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_DONE) {
            UiUtil.hideSoftKeyboard(view);
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        switch (id) {
            case R.id.pwd_patternCB:
                m_prefs.ShowPat = isChecked;
                update();
                break;
            case R.id.pwd_showCB:
                m_prefs.ShowPwd = isChecked;
                update();
                break;
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (!s.equals(m_pwdGrid.getPassword())) {
            m_pwdGrid.setPassword(s.toString(), false);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        String str;
        switch (id) {
            case R.id.pwd_clear:
                m_pwdText.setText("");
                m_pwdGrid.clear();
                break;
            case R.id.pwd_del:
                m_pwdGrid.deletePoint(-1);
                /*
                str = m_pwdText.getText().toString();
                if (!TextUtils.isEmpty(str)) {
                    m_pwdText.setText(str.substring(0, str.length() - 1));
                }
                */
                break;

            case R.id.pwd_verify_clear:
                m_pwdVerify.setText("");
                break;

            case R.id.pwd_verify_del:
                str = m_pwdVerify.getText().toString();
                if (!TextUtils.isEmpty(str)) {
                    m_pwdVerify.setText(str.substring(0, str.length() - 1));
                }
                break;
        }
    }
}