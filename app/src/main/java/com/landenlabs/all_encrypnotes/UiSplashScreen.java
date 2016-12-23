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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Show Large application image and fade out.
 *
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 */
public class UiSplashScreen {
    
    private final Activity m_context;
    private ImageView m_splashImg;
    private TextView m_splashTxt;
    private AnimatorSet m_splashAnimatSet;
    
    public UiSplashScreen(Activity context) {
        m_context = context;
    }
    
    /**
     * Show animated splash screen.
     * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
     */
    public void show() {
        final int fadeinMsec = 2000;
        final int fadeOutMsec = 1000;

        m_splashImg = (ImageView) m_context.findViewById(R.id.overlay_logo);
        m_splashImg.setVisibility(View.VISIBLE);
        m_splashTxt = (TextView) m_context.findViewById(R.id.overlay_text);
        m_splashTxt.setVisibility(View.VISIBLE);
        ObjectAnimator fadeImgIn = ObjectAnimator.ofFloat(m_splashImg, "alpha", 0f, 1f);
        fadeImgIn.setDuration(fadeinMsec);

        ObjectAnimator fadeTxtIn = ObjectAnimator.ofFloat(m_splashTxt, "alpha", 0f, 1f);
        fadeTxtIn.setDuration(fadeinMsec);
        
        ObjectAnimator fadeImgOut = ObjectAnimator.ofFloat(m_splashImg, "alpha", 1f, 0f);
        fadeImgOut.setDuration(fadeOutMsec);
        
        ObjectAnimator fadeTxtOut = ObjectAnimator.ofFloat(m_splashTxt, "alpha", 1f, 0f);
        fadeTxtOut.setDuration(fadeOutMsec);

        m_splashAnimatSet = new AnimatorSet();
        m_splashAnimatSet.playSequentially(fadeImgIn, fadeTxtIn, fadeImgOut, fadeTxtOut);
        m_splashAnimatSet.start();
    }

    /**
     * Hide splash screen.
     */
    public void hide() {
        if (m_splashAnimatSet != null)
            m_splashAnimatSet.end(); // possible threading issue.

        if (m_splashImg != null) {
            m_splashImg.setVisibility(View.INVISIBLE);
            m_splashImg = null;
            m_splashTxt.setVisibility(View.INVISIBLE);
            m_splashTxt = null;
        }
    }
}
