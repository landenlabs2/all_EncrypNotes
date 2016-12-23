# EncrypNotes
Android - Encrypted Notepad

![welcome](http://landenlabs.com/android/encrypnotes/welcome.png)


Encryp Notes is an encrypted notepad based off of code provided by [Ivan Voras (enotes)](http://sourceforge.net/projects/enotes/).

Screen sample of EncrypNotes

![Screen](http://landenlabs.com/android/encrypnotes/main-menu.png)


Encryp Notes takes Ivan's app further by adding a password grid pattern UI as an alternate way to provide passwords.

![Lock Pattern](http://landenlabs.com/android/encrypnotes/grid-pattern.png)


The grid password UI is based off of code provided by [ahmed](https://github.com/asghonim/simple_android_lock_pattern).

The grid pattern code was heavily redesigned to allow both drag drawing and tap picking. 
The pattern also supports overlapping paths. When a node is used multiple times, its icon rotates. 
When a path overlaps it changes color and reduces its width, revealing the previous path underneath.

**Basic features:**

* General Notepad functionality provided by Android's EditText widget (spell checker, cut/paste)
* Load and Save encryupted files with normal text password or Grid pattern (numeric sequence).



**Main Menu**

When you start the app, you can start typing to create a document or load a previously saved document. Use the option menu to pick an action.

New document| Prompt to save existing and clear screen
------------| ----------------------------------------
Open|	Present file browser followed by password controls.
Save|	Save current file with existing password, else does 'save as'
Save As|	Present file browser followed by password controls.
About|	Show app version and author info
File browser|	[experimental] open file browser to allow deletion of encrypted files
Paranoid|	Prevent saving of active document when no password is provided and app is closed or place in background. By default app will save current work when backgrounded and restore when app resumes. With paranoid enabled you will lose your work if you background the app and don't manually save your work.
Invert colors|	[experimental] Invert main screen background and font colors.


**Load Document**

To open and load an existing document, press the menu button and select **Open...** You will be presented with a file selection list of existing encrypted files. The file selection list shows the file name, its modification date and file size (# characters).

After selecting a file you need to provide a valid password to decrypt the document. You can enter a password in the lower box using the keyboard or use the numeric grid pattern. To use the grid patten either drag your finger across the numeric buttons or tap on the buttons. Your numeric sequence will appear in the password text box at the bottom. By default the grid pattern is displayed and your password is displayed. If you prefer to hide your password you can hit the check box labeled show.

![open1](http://landenlabs.com/android/encrypnotes/open-pwd1.png)

![open2](http://landenlabs.com/android/encrypnotes/open-pwd2.png)


**Save Document**

Similar to loading a document, you save by accessing the menu and selecting Save or Save As. You can either enter a file name or use the file browser to find an existing file to replaced. Use the grid pattern or text box to enter a password. If you hide your password and grid pattern you will be forced to enter the password twice.

![saveas1](http://landenlabs.com/android/encrypnotes/save-as1.png)

![saveas2](http://landenlabs.com/android/encrypnotes/save-as2.png)


