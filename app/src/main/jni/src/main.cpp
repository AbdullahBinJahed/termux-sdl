/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 
#include <jni.h>
#include <stdlib.h>
#include <unistd.h>
#include <android/log.h>

#include <SDL.h>
#include <SDL_image.h>
#include <SDL_mixer.h>
#include <SDL_net.h>
#include <SDL_ttf.h>

#define LOG_TAG "System.out"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__) 
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__) 
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__) 
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__) 
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__) 

#define printf(fmt, ...)  \
__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "\e[35m[%s:%s:\e[32mline:%d]\e[0m\t" fmt, __FILE__, __FUNCTION__, __LINE__, ##__VA_ARGS__);


#ifdef __cplusplus
extern "C" {
#endif

enum SDL_Libraries {
    SDL_IMAGE,
    SDL_MIXER,
    SDL_NET,
    SDL_TTF
};

 // fun setEnv()
JNIEXPORT jint JNICALL Java_com_termux_sdl_JNI_setEnv(JNIEnv *env, jobject thiz, 
                                jstring name, jstring value, jboolean overwrite){
    const char *_name  = env->GetStringUTFChars(name, NULL);
    const char *_value = env->GetStringUTFChars(value, NULL);

    return setenv(_name, _value, overwrite);
}

// fun unSetEnv()
JNIEXPORT jint JNICALL Java_com_termux_sdl_JNI_unSetEnv(JNIEnv *env, jobject thiz, 
                                                                    jstring name) {
    const char *_name = env->GetStringUTFChars(name, NULL);

    return unsetenv(_name);
}

// fun getEnv()
JNIEXPORT jstring JNICALL Java_com_termux_sdl_JNI_getEnv(JNIEnv *env, jobject thiz, 
                                                                    jstring name) {
    const char *_name = env->GetStringUTFChars(name, NULL);

    char *value = getenv(_name);
    if (!value) {
		return NULL;
    } else {
		return env->NewStringUTF(value);
    }
}

// fun chDir()
JNIEXPORT jint JNICALL Java_com_termux_sdl_JNI_chDir(JNIEnv *env, jobject thiz, 
                                                                  jstring path){
    const char *_path = env->GetStringUTFChars(path, NULL);
    return chdir(_path);
}


// fun getSDLVersion()
JNIEXPORT jstring JNICALL Java_com_termux_sdl_JNI_getSDLVersion(JNIEnv *env, 
                                                    jclass thiz, jint lib) {
    char buf[256];
    SDL_version version;
    
    switch(lib) {
    case SDL_IMAGE:
	    SDL_IMAGE_VERSION(&version);
	    break;
    case SDL_MIXER:
	    SDL_MIXER_VERSION(&version);
	    break;
    case SDL_NET:
	    SDL_NET_VERSION(&version);
	    break;
    case SDL_TTF:
	    SDL_TTF_VERSION(&version);
	    break;
    default:
	    SDL_GetVersion(&version);
	    break;
    }

    snprintf(buf, sizeof(buf), "%d.%d.%d", version.major, version.minor, version.patch);

    return env->NewStringUTF(buf);
}


#ifdef __cplusplus
};
#endif

