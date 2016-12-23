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

package com.landenlabs.password;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.landenlabs.all_encrypnotes.R;

/**
 * Wrapper class on Button to make it easier to style password buttons
 * and detect password objects in PasswordGrid layout.
 *
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 * @author Dennis Lang v2 9/1/2014
 *         <p>
 * @author ahmed v1 7/2/2014 <br>
 *         Original version: {@link https ://github.com/asghonim/simple_android_lock_pattern}
 *
 */
public class PasswordButton extends Button {

    public PasswordButton(Context context) {
        super(context);
        init(context, null);
    }

    public PasswordButton(Context context, AttributeSet attrs) {
		super(context, attrs, R.attr.passwordButtonStyle);
		init(context, attrs);
    }

    public PasswordButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, ((defStyle == 0) ? R.attr.passwordButtonStyle : defStyle) );
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
    	if (!isInEditMode()) {
	        if (getTag() == null) {
	            setTag(getText());
	        }
    	}   
    }
}
