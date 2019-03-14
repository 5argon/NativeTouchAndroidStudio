package com.Exceed7.NativeTouch;

import com.unity3d.player.*;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class NativeTouchListener {

    static View.OnTouchListener nativeTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {

            //Report the same timestamp for all touches, even if they move or not. We have to quickly do this the first thing on the listener.
            double listenerTimestamp = (double) AndroidTouchTime();

            startTouches();

            //Log.i("DISPATCH ", event.getX() + " " + event.getY() + " Android Touch Time : " + String.valueOf(listenerTimestamp) + " Time : " + String.valueOf(event.getEventTime()) + " Action : " + event.getActionMasked() + " Pointer count : " + event.getPointerCount());

            //One `MotionEvent` contains multiple touch data. We will peel them out and write to ring buffer, then told C# to check them.

            int pointerCount = event.getPointerCount();

            //One `MotionEvent` may includes other unrelated fingers, but only one "main event" exist in this. We will determine that main event.

            int act = event.getActionMasked();
            boolean downAction = false;
            boolean upAction = false;
            boolean moveAction = false;
            boolean cancelledAction = false;

            switch (act) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    downAction = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    upAction = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    moveAction = true;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    cancelledAction = true;
                    break;
            }

            //Determine the callback type from the "main" action. Use it with every touches in this `MotionEvent`.
            int callbackType = 4; // Default to cancelled.
            if (downAction) {
                callbackType = 0;
            } else if (upAction) {
                callbackType = 3;
            } else if (moveAction) {
                callbackType = 1;
            }

            int pointerIdOfAction = event.getPointerId(event.getActionIndex());
            for (int i = 0; i < pointerCount; i++) {
                int pointerId = event.getPointerId(i);
                boolean actionOccurOnThisPointer = pointerIdOfAction == pointerId;

                float x = event.getX(i);
                float y = event.getY(i);

                //We are not using the time that comes with the touch.
                //Because for touches that aren't moving, their time is not updated.
                //Instead, we bake the time we get manually at the very first line to all touches.
                //float time = event.getEventTime();

                int phase = 4;
                if (downAction && actionOccurOnThisPointer) {
                    phase = MotionEvent.ACTION_DOWN;
                } else if (upAction && actionOccurOnThisPointer) {
                    phase = 3; //MotionEvent.ACTION_UP; //UP is 1 in Android but in Unity it's 3
                } else if (cancelledAction) {
                    // Cancelled action will put all touches in the pack as Cancelled.
                    // Does not care about whether if the action is on this touch or not.
                    phase = 4;
                } else if (!actionOccurOnThisPointer || moveAction) {
                    //If action is not on this pointer we are not sure the others move or stay still. We always say move.
                    //If it is move action then also we don't know which one move or stay still. We always say move.
                    phase = 1; //MotionEvent.ACTION_MOVE; //MOVE is 2 in Android but in Unity it's 1
                }

                if (isMinimalMode) {
                    //Writes the touch to ring buffer, and advance the ring buffer pointer.
                    //Log.i("Writing Touch", x + " " + y + " Action? " + actionOccurOnThisPointer + " ("+ pointerIdOfAction +") ID " + pointerId );
                    writeTouchMinimal(callbackType, x, y, phase, listenerTimestamp, pointerId);
                } else {
                    //Get more..
                    float orientation = event.getOrientation(i);
                    float pressure = event.getPressure(i);
                    float size = event.getSize(i);
                    float touchMajor = event.getTouchMajor(i);
                    float touchMinor = event.getTouchMinor(i);

                    //Writes the touch to ring buffer, and advance the ring buffer pointer.
                    writeTouchFull(callbackType, x, y, phase, listenerTimestamp, pointerId,
                            orientation, pressure, size, touchMajor, touchMinor);
                }
            }

            //All ring buffer writes are now complete for this listener call.
            //We now do reverse p-invoke once to tell C# to check the buffer at specific spot.
            //Those spots are a parameter of that invoke.

            //If we are in no callback mode, we rely on C# to manually read out the ring buffer without telling it now.
            if(!isNoCallback) {
                if (isMinimalMode) {
                    commitTouchesMinimal();
                } else {
                    commitTouchesFull();
                }
            }

            //So that everything else like uGUI, EventTrigger, etc. in Unity still works, we are forwarding the touch to Unity also.
            //However ours will probably arrives first, be it the callback-based way or dequeue-based way.
            if (!isDisableUnityTouch) {
                UnityPlayer.currentActivity.onTouchEvent(event);
            }

            //Returning true for successful touch. (When will it could be false??)
            return true;
        }
    };

    private static boolean isMinimalMode;
    private static boolean isDisableUnityTouch;
    private static boolean isNoCallback;

    public static void StopNativeTouch() {
        //Log.i("Android Native Touch","Stopped");
        UnityPlayer.currentActivity.getCurrentFocus().setOnTouchListener(null);
    }

    public static void StartNativeTouch(boolean fullMode, boolean disableUnityTouch, boolean noCallback) {
        //Log.i("Android Native Touch","Registered");
        isMinimalMode = !fullMode;
        isDisableUnityTouch = disableUnityTouch;
        isNoCallback = noCallback;
        UnityPlayer.currentActivity.getCurrentFocus().setOnTouchListener(nativeTouchListener);
    }

    public static int RealScreenHeight() {
        return UnityPlayer.currentActivity.getCurrentFocus().getHeight();
    }

    public static int RealScreenWidth() {
        return UnityPlayer.currentActivity.getCurrentFocus().getWidth();
    }

    public static long AndroidTouchTime() {
        return SystemClock.uptimeMillis();
    }

    //This probably has no use, but may be handy for debugging here and also from Unity.
    //public static long ElapsedRealtimeNanos() {
        //return SystemClock.elapsedRealtimeNanos();
    //}

    public static native void startTouches();

    public static native void writeTouchMinimal(int callbackType, float x, float y, int phase, double timestamp, int pointerId);

    public static native void writeTouchFull(int callbackType, float x, float y, int phase, double timestamp, int pointerId,
                                             float orientation, float pressure, float size, float touchMajor, float touchMinor);

    public static native void commitTouchesMinimal();

    public static native void commitTouchesFull();

}