//
// Created by Garen Xie on 6/30/2020.
//


#include <jni.h>  /* /usr/lib/jvm/java-1.7.0-openjdk-amd64/include/ */
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>

#include <android/log.h>  /* liblog */
#include <string.h>
#include <unistd.h>

#define LOG_TAG "tamperBtnStatus"

#define DEBUG
#define ANDROID_PLATFORM

#ifdef DEBUG
#ifdef ANDROID_PLATFORM
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
#else
#define LOGD(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
	#define LOGI(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
	#define LOGW(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
	#define LOGE(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
#endif
#else
#define LOGD(...)
	#define LOGI(...)
	#define LOGW(...)
	#define LOGE(...)
#endif

#if 0
typedef struct {
    char *name;          /* Java里调用的函数名 */
    char *signature;    /* JNI字段描述符, 用来表示Java里调用的函数的参数和返回值类型 */
    void *fnPtr;          /* C语言实现的本地函数 */
} JNINativeMethod;
#endif

static int fd;
#define	tamper_Dev	"/sys/devices/soc.0/TamperSw.31/tamperStatus"
#define tamperPressed   '0'
#define tamperReleased   '1'

jint getTamperStatus(JNIEnv *env, jobject cls)
{
    char readBuf[100];
    int retCount;
    int ret = -1;

    memset(readBuf,0,sizeof(readBuf));
    fd = open(tamper_Dev, O_RDONLY);
    if(fd < 0){
        LOGE("Can't open file.");
        return -1;
    }

    retCount = read(fd, readBuf, sizeof(readBuf));
    if(retCount < 0){

        LOGE("read file failed.");
    }
    else{

        LOGI("read data:%s",readBuf);
        if(readBuf[0] == tamperPressed){
            LOGI("tamper pressed.");
            ret = 0;
        }
        else if(readBuf[0] == tamperReleased){
            LOGI("tamper released.");
            ret = 1;
        }
        else{
            LOGI("tamper Unknown status.");
            ret = -1;
        }
    }
    close(fd);
    return ret;
}




static const JNINativeMethod methods[] = {
        {"getTamperStatus", "()I", (void *)getTamperStatus},    //() --> 无参数, V --> 无 返回值 void, I --> 返回值 int
};




/* System.loadLibrary */
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    JNIEnv *env;
    jclass cls;

    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_4)) {
        return JNI_ERR; /* JNI version not supported */
    }
    cls = (*env)->FindClass(env, "com/garen/gettamperswitch/MainActivity");    //package com.garen.gettamperswitch;  MainActivity class
    if (cls == NULL) {
        return JNI_ERR;
    }

    /* 2. map java hello <-->c c_hello */
    if ((*env)->RegisterNatives(env, cls, methods, sizeof(methods)/sizeof(methods[0])) < 0)
        return JNI_ERR;

    return JNI_VERSION_1_4;
}
