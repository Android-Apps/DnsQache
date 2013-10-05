LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

SAVE_LOCAL_PATH := $(LOCAL_PATH)

LOCAL_MODULE    := libdqnativetask
LOCAL_SRC_FILES := com_tdhite_dnsqache_system_NativeTask.c

LOCAL_CFLAGS := -DANDROID
include $(BUILD_SHARED_LIBRARY)

# build dnsmasq as a native executable
LOCAL_PATH := $(SAVE_LOCAL_PATH)
include $(SAVE_LOCAL_PATH)/polipo/Android.mk

# build dnsmasq as a native executable
LOCAL_PATH := $(SAVE_LOCAL_PATH)
include $(SAVE_LOCAL_PATH)/dnsmasq/Android.mk

# build dnsmasq as a native executable
LOCAL_PATH := $(SAVE_LOCAL_PATH)
include $(SAVE_LOCAL_PATH)/tinyproxy/Android.mk

# build the scripter native executable
LOCAL_PATH := $(SAVE_LOCAL_PATH)
include $(SAVE_LOCAL_PATH)/scripter/Android.mk
