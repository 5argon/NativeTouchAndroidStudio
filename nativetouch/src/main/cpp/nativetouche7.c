//
// Created by Sirawat Pitaksarit on 2018/09/01.
//

// Cannot do extern in Unity if this is .cpp file lol why

#include <stdlib.h>
#include <assert.h>
#include <jni.h>
#include <string.h>
#include <pthread.h>
#include <math.h>

// For logging
#include <android/log.h>

typedef struct
{
    int callbackType;
    float x;
    float y;
    float previousX; //no use on android
    float previousY; //no use on android
    int phase;
    double timestamp;
    int pointerId;
} NativeTouchData;

typedef struct
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
} NativeTouchDataFull;

typedef void (*NativeTouchFullDelegate)(NativeTouchDataFull datas);
typedef void (*NativeTouchMinimalDelegate)(NativeTouchData datas);

NativeTouchFullDelegate fullCallback;
NativeTouchMinimalDelegate minimalCallback;;

void registerCallbacks(NativeTouchFullDelegate fullDelegate, NativeTouchMinimalDelegate minimalDelegate)
{
    fullCallback = fullDelegate;
    minimalCallback = minimalDelegate;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_sendTouchMinimal(JNIEnv *env, jclass clazz,
        int callbackType, float x, float y, int phase, double timestamp, int pointerId)
{
    NativeTouchData ntd;

    ntd.callbackType = callbackType;
    ntd.x = x;
    ntd.y = y;
    ntd.previousX = -1; //no use on android
    ntd.previousY = -1; //no use on android
    ntd.phase = phase;
    ntd.timestamp = timestamp;
    ntd.pointerId = pointerId;

    minimalCallback(ntd);
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_sendTouchFull(JNIEnv *env, jclass clazz,
                int callbackType, float x, float y,  int phase, double timestamp, int pointerId,
                float orientation, float pressure,float size, float touchMajor, float touchMinor)
{
    NativeTouchDataFull ntd;

    ntd.callbackType = callbackType;
    ntd.x = x;
    ntd.y = y;
    ntd.previousX = -1; //no use on android
    ntd.previousY = -1; //no use on android
    ntd.phase = phase;
    ntd.timestamp = timestamp;
    ntd.pointerId = pointerId;

    ntd.tapCount = -1; //no use on android
    ntd.type = -1; //no use on android
    ntd.pressure = pressure;
    ntd.maximumPossibleForce = -1; //no use on android
    ntd.touchMajor = touchMajor;
    ntd.touchMinor = touchMinor;
    ntd.size = size;
    ntd.orientation = orientation;

    fullCallback(ntd);

}