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

#include <unistd.h>
#include <jni.h>
#include <android/log.h>
#define  LOG_TAG    "your-log-tag"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
int ringBufferIndex = 0;
int ringBufferCurrentCount = 0;

NativeTouchCheckRingBufferDelegate fullCallbackCheckRingBuffer;
NativeTouchCheckRingBufferDelegate minimalCallbackCheckRingBuffer;

//Receive ring buffer space from C#, write on native side.
NativeTouchData* ntdRingBuffer;
NativeTouchDataFull* ntdFullRingBuffer;

int* finalCursor; //Set from C#, here we just focus on increasing it.
int* dekker; //int[3] at C#

void registerCallbacksCheckRingBuffer(
    NativeTouchCheckRingBufferDelegate fullDelegate,
    NativeTouchCheckRingBufferDelegate minimalDelegate,
    NativeTouchDataFull* fullRingBuffer,
    NativeTouchData* minimalRingBuffer,
    int* finalCursorHandle,
    int* dekkerHandle,
    int ringBufferSizeFromCSharp
)
{
    //You could increase the ring buffer size from a `const` in C#. It would be synchronized to here.
    ringBufferSize = ringBufferSizeFromCSharp;
    ringBufferIndex = 0;
    ringBufferCurrentCount = 0;

    fullCallbackCheckRingBuffer = fullDelegate;
    minimalCallbackCheckRingBuffer = minimalDelegate;

    ntdFullRingBuffer = fullRingBuffer;
    ntdRingBuffer = minimalRingBuffer;

    finalCursor = finalCursorHandle;
    dekker = dekkerHandle;
}

void enterCriticalSection()
{
    dekker[1] = 1; //Want to enter
    while(dekker[0] == 1) //While C# wants to enter
    {
        //LOGD("Waiting for C# to finish %d %d %d", dekker[0], dekker[1], dekker[2]);
        if(dekker[2] != 1) //If not native's turn
        {
            //LOGD("Incorrect turn %d %d %d", dekker[0], dekker[1], dekker[2]);
            dekker[1] = 0; //Don't want to enter for a while
            while(dekker[2] != 1)
            {
                //Busy wait
                //LOGD("Busy waiting %d %d %d", dekker[0], dekker[1], dekker[2]);
                usleep(1); //Without sleeping for some reason it could not check for the new value C# written for us...
            }
            //LOGD("Out of busy %d %d %d", dekker[0], dekker[1], dekker[2]);
            dekker[1] = 1; //Want to enter again, if C# don't want to enter I will go ahead.
        }
        else
        {
            usleep(1); //Without sleeping for some reason it could not check for the new value C# written for us...
        }
        //LOGD("Checking again %d %d %d", dekker[0], dekker[1], dekker[2]);
    }
    //LOGD("Entering critical section! %d %d %d", dekker[0], dekker[1], dekker[2]);
}

void exitCriticalSection()
{
    dekker[2] = 0; //Give turn to C#
    dekker[1] = 0; //Don't want to enter now that I am finished.
    //LOGD("ENDING critical section! %d %d %d", dekker[0], dekker[1], dekker[2]);
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_startTouches(JNIEnv *env, jclass clazz)
{
    ringBufferIndex = (ringBufferIndex + ringBufferCurrentCount) % ringBufferSize;
    ringBufferCurrentCount = 0;
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_writeTouchMinimal(JNIEnv *env, jclass clazz,
        int callbackType, float x, float y, int phase, double timestamp, int pointerId)
{
    enterCriticalSection();
    //Try to write C# memory in-place so we don't have to allocate the struct needlessly.
    //We have already paid JNI cost from Java to C with tons of args...
    NativeTouchData* ntd = &ntdRingBuffer[(ringBufferIndex + ringBufferCurrentCount) % ringBufferSize];

    ntd->callbackType = callbackType;
    ntd->x = x;
    ntd->y = y;
    ntd->previousX = -1; //no use on android
    ntd->previousY = -1; //no use on android
    ntd->phase = phase;
    ntd->timestamp = timestamp;
    ntd->pointerId = pointerId;
    ntd->nativelyGenerated = 1;

    ringBufferCurrentCount++;

    (*finalCursor)++;

    exitCriticalSection();
}


void Java_com_Exceed7_NativeTouch_NativeTouchListener_writeTouchFull(JNIEnv *env, jclass clazz,
                int callbackType, float x, float y,  int phase, double timestamp, int pointerId,
                float orientation, float pressure,float size, float touchMajor, float touchMinor)
{
    enterCriticalSection();
    //Try to write C# memory in-place so we don't have to allocate the struct needlessly.
    //We have already paid JNI cost from Java to C with tons of args...
    NativeTouchDataFull* ntd = &ntdFullRingBuffer[(ringBufferIndex + ringBufferCurrentCount) % ringBufferSize];

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

    ringBufferCurrentCount++;

    (*finalCursor)++;

    exitCriticalSection();
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_commitTouchesMinimal(JNIEnv *env, jclass clazz)
{
    minimalCallbackCheckRingBuffer(ringBufferIndex, ringBufferCurrentCount);
}

void Java_com_Exceed7_NativeTouch_NativeTouchListener_commitTouchesFull(JNIEnv *env, jclass clazz)
{
    fullCallbackCheckRingBuffer(ringBufferIndex, ringBufferCurrentCount);
}
