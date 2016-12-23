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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

/**
 * Created by Dennis Lang on 7/1/16.
 *
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 */
public class Email {

    public static void send(Activity activity, String emailTo, String emailSubject, String emailContent)
    {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ emailTo});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailContent);

        // use below 2 commented lines if need to use BCC an CC feature in email
        boolean bcc = false;
        if (bcc) {
            String bccTo = "foo@gmail.com";
            emailIntent.putExtra(Intent.EXTRA_CC, new String[]{bccTo});
            emailIntent.putExtra(Intent.EXTRA_BCC, new String[]{bccTo});
        }

        // use below 3 commented lines if need to send attachment
        boolean attachment = false;
        if (attachment) {
            emailIntent.setType("image/jpeg");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "My Picture");
            String sdPath = Environment.getExternalStorageDirectory().getPath();
            String imgPath = String.format("file://%s/image.png", sdPath);
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imgPath));
        }

        // need this to prompts email client only
        emailIntent.setType("message/rfc822");
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity.startActivity(Intent.createChooser(emailIntent, "Select an Email Client:"));
    }
}
