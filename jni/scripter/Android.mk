LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := scripter
LOCAL_SRC_FILES := main.c

LOCAL_CFLAGS := -DANDROID
LOCAL_STATIC_LIBRARIES := libparser

include $(BUILD_EXECUTABLE)

include $(LOCAL_PATH)/parser/Android.mk
