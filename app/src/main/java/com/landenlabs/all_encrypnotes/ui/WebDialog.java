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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.MailTo;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.landenlabs.all_encrypnotes.R;

/**
 * Create a Web (html) viewer dialog.
 *
 * @author Dennis Lang
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 */
public class WebDialog {
    public static String HTML_CENTER_BOX = "<div style='min-height:128px;'><table height='100%%' width='100%%'><tr valign='middle'><td style='border: 2px solid; border-radius: 25px;'><center>%s</center></table></div>";
    
    public static AlertDialog show(final Context activity, String ... htmlStr) {

        String fullHtmlStr = htmlStr[0];
        if (htmlStr.length > 1) {
            String fmt = htmlStr[0];
            String arg = htmlStr[1];
            fullHtmlStr = String.format(fmt, arg);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("");
        
        WebView wv = new WebView(activity);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.setBackgroundResource(R.drawable.about_bg);
       

        // String htmlStr = String.format(LoadData("about.html"), getPackageInfo().versionName,
        //         Doc.CRYPTO_MODE);
        wv.loadData(fullHtmlStr, "text/html; charset=utf-8", "utf-8");
        wv.setMinimumHeight(128 * 2);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.startsWith("mailto:")){
                    MailTo mt = MailTo.parse(url);
                    Intent emailIntent = UiUtil.newEmailIntent(activity, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
                    activity.startActivity(emailIntent);
                    view.reload();
                    return true;
                } else{
                    // Open link in external browser.
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    activity.startActivity(webIntent);
                }
                return true;
            }
        });

        builder.setView(wv);
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }
}
