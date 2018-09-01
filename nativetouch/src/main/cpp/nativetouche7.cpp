//
// Created by Sirawat Pitaksarit on 2018/09/01.
//

#include <stdlib.h>
#include <assert.h>
#include <jni.h>
#include <string.h>
#include <pthread.h>
#include <math.h>

// For logging
#include <android/log.h>

struct NativeTouchData
{
    int callbackType;
    float x;
    float y;
    float previousX; //no use on android
    float previousY; //no use on android
    int phase;
    double timestamp;
    int pointerId;
};

struct NativeTouchDataFull
{
    int callbackType;
    float x;
    float y;
    float previousX; //no use on android
    float previousY; //no use on android
    int phase;
    double timestamp;
    int pointerId;

    //-- Full mode only structs --

    int tapCount; //no use on android
    int type; //no use on android
    float pressure;
    float maximumPossibleForce; //no use on android
    float touchMajor;
    float touchMinor;
    float size;
    float orientation;
};

typedef void (*NativeTouchFullDelegate)(struct NativeTouchDataFull datas);
typedef void (*NativeTouchMinimalDelegate)(struct NativeTouchData datas);

NativeTouchFullDelegate fullCallback;
NativeTouchMinimalDelegate minimalCallback;;
bool isFullMode;
bool isDisableUnityTouch;

void startNativeTouch(NativeTouchFullDelegate fullDelegate, NativeTouchMinimalDelegate minimalDelegate, int fullMode, int disableUnityTouch)
{
    fullCallback = fullDelegate;
    minimalCallback = minimalDelegate;
    isDisableUnityTouch = disableUnityTouch == 1 ? true : false;
    isFullMode = fullMode == 1 ? true : false;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_sendTouchMinimal(JNIEnv *env, jclass clazz,
        int callbackType, float x, float y, int phase, long timestamp, int pointerId)
{
    NativeTouchData ntd;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_sendTouchFull(JNIEnv *env, jclass clazz,
                int callbackType, float x, float y,  int phase, long timestamp, int pointerId,
                float orientation, float pressure,float size, float touchMajor, float touchMinor)
{

}