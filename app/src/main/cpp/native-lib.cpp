#include <jni.h>
#include <string>
#include <iostream>
#include <unistd.h>

#include "leelazero.h"


extern "C" {
JNIEXPORT jint JNICALL Java_com_example_user_blugoAI_Leela_StopEngine(
        JNIEnv *env,
        jobject /* this */) {
    return 1;
}

JNIEXPORT jint JNICALL Java_com_example_user_blugoAI_Leela_StartEngine(
        JNIEnv *env,
        jobject, /* this */
        jstring weightfilename) {
    std::cout << "Java_com_example_leelaapplication_MainActivity_StartEngine" << std::endl ;
        const char* str;
        str = env->GetStringUTFChars(weightfilename, NULL);
        std::string weightsfile = str;
        env->ReleaseStringUTFChars(weightfilename, str);
    return StartEngine(weightsfile);
}

JNIEXPORT jint JNICALL Java_com_example_user_blugoAI_Leela_SendGTP(
        JNIEnv *env,
        jobject /* this */,
        jstring weightfilename) {

    std::cout << "Java_com_example_leelaapplication_MainActivity_SendGTP"<< std::endl ;
        const char* str;
        str = env->GetStringUTFChars(weightfilename, NULL);
        std::string cmd = str;
        env->ReleaseStringUTFChars(weightfilename, str);
    return SendGTP(cmd);
}
JNIEXPORT jstring JNICALL Java_com_example_user_blugoAI_Leela_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "version";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jstring JNICALL Java_com_example_user_blugoAI_Leela_getStdoutFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(getStdout().c_str());
}

}
