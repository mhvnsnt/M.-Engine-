#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ai_LlamaEngine_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Llama.cpp NDK wrapper initialized. Ready for GGUF models on mobile GPU.";
    return env->NewStringUTF(hello.c_str());
}
