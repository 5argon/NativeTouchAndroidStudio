//
// Created by Sirawat Pitaksarit on 2018/09/01.
//

// Cannot do extern in Unity if this is .cpp file lol why

// Note : Using AndroidJavaProxy to talk back to Unity is not thread safe and will randomly cause SIGSEGV 11.
// when triggered from other thread like Android's touch.
// Using native delegate like you see here is safe.

// This file is compiled to dynamically linked library named "libnativetouche7" according to the CMakeList.txt file.
// It could be used from Unity with [DllImport("nativetouche7")]
// Note that Android could not do static linking like iOS. (If that is possible then we could  [DllImport("__Internal")]

#include <jni.h>

//CAREFULLY design the struct shape to be the same as in C#.
//We are writing to C# memory here! And that includes,
//we have to make space for even fields that we aren't using on Android. (But used in iOS)
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
    int nativelyGenerated;
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
    int nativelyGenerated;

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

//This type of delegate tells C# to check the ring buffer starting from `start` index and goes by `count`.
//That's all the new touches.
typedef void (*NativeTouchCheckRingBufferDelegate)(int start, int count);

int ringBufferSize = -1;
int startingMinimalRbIndex = 0;
int minimalRbCurrentCount = 0;
int startingFullRbIndex = 0;
int fullRbCurrentCount = 0;

NativeTouchCheckRingBufferDelegate fullCallbackCheckRingBuffer;
NativeTouchCheckRingBufferDelegate minimalCallbackCheckRingBuffer;
//Receive ring buffer space from C#, write on native side.
NativeTouchData* ntdRingBuffer;
NativeTouchDataFull* ntdFullRingBuffer;

void registerCallbacksCheckRingBuffer(NativeTouchCheckRingBufferDelegate fullDelegate, NativeTouchCheckRingBufferDelegate minimalDelegate,
NativeTouchDataFull* fullRingBuffer, NativeTouchData* minimalRingBuffer, int ringBufferSizeFromCSharp )
{
    //You could increase the ring buffer size from a `const` in C#. It would be synchronized to here.
    ringBufferSize = ringBufferSizeFromCSharp;

    fullCallbackCheckRingBuffer = fullDelegate;
    minimalCallbackCheckRingBuffer = minimalDelegate;

    ntdFullRingBuffer = fullRingBuffer;
    ntdRingBuffer = minimalRingBuffer;
}
void Java_com_Exceed7_NativeTouch_NativeTouchListener_startTouches(JNIEnv *env, jclass clazz)
{
    minimalRbCurrentCount = 0;
    fullRbCurrentCount = 0;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_writeTouchMinimal(JNIEnv *env, jclass clazz,
        int callbackType, float x, float y, int phase, double timestamp, int pointerId)
{
    //Try to write C# memory in-place so we don't have to allocate the struct needlessly.
    //We have already paid JNI cost from Java to C with tons of args...
    NativeTouchData* ntd = &ntdRingBuffer[(startingMinimalRbIndex + minimalRbCurrentCount) % ringBufferSize];

    ntd->callbackType = callbackType;
    ntd->x = x;
    ntd->y = y;
    ntd->previousX = -1; //no use on android
    ntd->previousY = -1; //no use on android
    ntd->phase = phase;
    ntd->timestamp = timestamp;
    ntd->pointerId = pointerId;
    ntd->nativelyGenerated = 1;

    minimalRbCurrentCount++;
}


void Java_com_Exceed7_NativeTouch_NativeTouchListener_writeTouchFull(JNIEnv *env, jclass clazz,
                int callbackType, float x, float y,  int phase, double timestamp, int pointerId,
                float orientation, float pressure,float size, float touchMajor, float touchMinor)
{
    //Try to write C# memory in-place so we don't have to allocate the struct needlessly.
    //We have already paid JNI cost from Java to C with tons of args...
    NativeTouchDataFull* ntd = &ntdFullRingBuffer[(startingFullRbIndex + fullRbCurrentCount) % ringBufferSize];

    ntd->callbackType = callbackType;
    ntd->x = x;
    ntd->y = y;
    ntd->previousX = -1; //no use on android
    ntd->previousY = -1; //no use on android
    ntd->phase = phase;
    ntd->timestamp = timestamp;
    ntd->pointerId = pointerId;
    ntd->nativelyGenerated = 1;

    ntd->tapCount = -1; //no use on android
    ntd->type = -1; //no use on android
    ntd->pressure = pressure;
    ntd->maximumPossibleForce = -1; //no use on android
    ntd->touchMajor = touchMajor;
    ntd->touchMinor = touchMinor;
    ntd->size = size;
    ntd->orientation = orientation;

    fullRbCurrentCount++;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_commitTouchesMinimal(JNIEnv *env, jclass clazz)
{
    minimalCallbackCheckRingBuffer(startingMinimalRbIndex, minimalRbCurrentCount);
    startingMinimalRbIndex = (startingMinimalRbIndex + minimalRbCurrentCount) % ringBufferSize;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_commitTouchesFull(JNIEnv *env, jclass clazz)
{
    fullCallbackCheckRingBuffer(startingFullRbIndex, fullRbCurrentCount);
    startingFullRbIndex = (startingFullRbIndex + fullRbCurrentCount) % ringBufferSize;
}
