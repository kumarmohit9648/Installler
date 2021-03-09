package com.mohit.installer;

public interface OnAttachmentDownloadListener {
    void onAttachmentDownloadedSuccess();
    void onAttachmentDownloadedError();
    void onAttachmentDownloadedFinished();
    void onAttachmentDownloadUpdate(float percent);
}
