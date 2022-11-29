LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libacStatus
LOCAL_SRC_FILES := acStatus.c
#LOCAL_SHARED_LIBRARIES := liblog
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)

# just for debug Android.mk
#$(warning "the value of LOCAL_PATH is$(LOCAL_PATH)")

# for compile tamperBtnStatus.c
include $(LOCAL_PATH)/../tamperBtnStatus/Android.mk
