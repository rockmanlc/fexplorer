//
// Created by superlee on 2020/11/12.
//
#include <jni.h>
#include <string>
#include <android/log.h>

JavaVM * gPJavaVm = NULL;

static const char *TAG = "rocklee";

void onChanged() {
    JNIEnv * pJniEnv = NULL;
    jobject joJavaObj = NULL;
    jclass clazz = NULL;
    jmethodID method = NULL;
    jint jStatus;
    if (gPJavaVm != NULL) {
        __android_log_write(ANDROID_LOG_DEBUG, TAG,"gPJavaVm != NULL");
        jStatus = gPJavaVm->GetEnv((void **)&pJniEnv, JNI_VERSION_1_6);
        if(JNI_EDETACHED == jStatus) {
            __android_log_write(ANDROID_LOG_DEBUG, TAG, "AttachCurrentThread");
            JavaVMAttachArgs sAttachArgs = {JNI_VERSION_1_6, NULL, NULL};
            jStatus = gPJavaVm->AttachCurrentThread(&pJniEnv, &sAttachArgs);
            if(JNI_OK != jStatus)
                return;
        }
        clazz = pJniEnv->FindClass("com/rocklee/fexplorer/test/Counter");
        if (clazz == NULL)
            return;
        method = pJniEnv->GetStaticMethodID(clazz, "changed", "()V");
        pJniEnv->CallStaticVoidMethod(clazz, method);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_rocklee_fexplorer_test_Counter_stringFromJNI(
        JNIEnv* env,
        jobject) {
    std::string hello = "Hello from C++";
    onChanged();
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    if(vm == NULL) {
        return JNI_ERR;
    }
    gPJavaVm = vm;
    return JNI_VERSION_1_6;
}