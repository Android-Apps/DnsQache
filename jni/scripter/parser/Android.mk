LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

SAVE_LOCAL_PATH := $(LOCAL_PATH)

LOCAL_MODULE    := libparser
LOCAL_SRC_FILES := format.c parse.c token.c

include $(BUILD_STATIC_LIBRARY)
