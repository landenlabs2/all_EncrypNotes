package com.landenlabs.all_encrypnotes;

public interface SendMsg {
    int MSG_FAIL = 0;
    int MSG_OKAY = 1;
    
    void send(int msgNum);
}
