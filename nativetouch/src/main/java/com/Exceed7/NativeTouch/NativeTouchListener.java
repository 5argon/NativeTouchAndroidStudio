package com.Exceed7.NativeTouch;

import com.unity3d.player.*;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class NativeTouchListener {

    static View.OnTouchListener nativeTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            //public boolean dispatchTouchEvent(MotionEvent event) {
            //Log.i("DISPATCH ", event.getX() + " " + event.getY() + " " + String.valueOf(event.getEventTime()));

            //Report the same timestamp for all touch even if they move or not.
            long timestamp = AndroidTouchTime();

            //MotionEvent might contains multiple touch data. We will callback once for each.
            if (storedDelegate != null) {

                int pointerCount = event.getPointerCount();
                int act = event.getActionMasked();
                boolean downAction = act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN;
                boolean upAction = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP;
                boolean moveAction = act == MotionEvent.ACTION_MOVE;

                //Determine the callback type from the "main" action. Used with every touches in this MotionEvent.
                int callbackType = 4; //cancelled
                if (downAction) {
                    callbackType = 0;
                } else if (upAction) {
                    callbackType = 3;
                } else if (moveAction) {
                    callbackType = 1;
                }

                for (int i = 0; i < pointerCount; i++) {

                    int pointerId = event.getPointerId(i);
                    int pointerIdOfAction;
                    if (pointerCount > 1) {
                        pointerIdOfAction = event.getActionIndex();
                    } else {
                        pointerIdOfAction = pointerId;
                    }

                    boolean actionOccurOnThisPointer;
                    if (pointerCount > 1) {
                        actionOccurOnThisPointer = pointerIdOfAction == pointerId;
                    } else {
                        actionOccurOnThisPointer = true;
                    }

                    float x = event.getX(i);
                    float y = event.getY(i);
                    int phase = 4; //If ACTION_CANCELLED it will not match on any ifs below, we return CANCELLED phase which is 3 in Android but 4 in Unity.
                    if (downAction && actionOccurOnThisPointer) {
                        phase = MotionEvent.ACTION_DOWN;
                    } else if (upAction && actionOccurOnThisPointer) {
                        phase = 3; //MotionEvent.ACTION_UP; //UP is 1 in Android but in Unity it's 3
                    } else if (!actionOccurOnThisPointer || moveAction) {
                        //If action is not on this pointer we are not sure the others move or stay still. We always say move.
                        //If it is move action then also we don't know which one move or stay still. We always say move.

                        phase = 1; //MotionEvent.ACTION_MOVE; //MOVE is 2 in Android but in Unity it's 1
                    }

                    if (isMinimalMode) {
                        //If minimal mode we can start sending now!
                        storedDelegate.NativeTouchMinimalDelegate(callbackType, x, y, phase, timestamp, pointerId);
                    } else {
                        //Get more..
                        float orientation = event.getOrientation(i);
                        float pressure = event.getPressure(i);
                        float size = event.getSize(i);
                        float touchMajor = event.getTouchMajor(i);
                        float touchMinor = event.getTouchMinor(i);
                        storedDelegate.NativeTouchRawDelegate(callbackType, x, y, phase, timestamp, pointerId,
                                orientation, pressure, size, touchMajor, touchMinor);
                    }
                }
            }

            if (!isDisableUnityTouch) {
                //Unity input still works
                UnityPlayer.currentActivity.onTouchEvent(event);
            }
            return true;
        }

    };

    private static TouchDelegate storedDelegate;
    private static boolean isMinimalMode;
    private static boolean isDisableUnityTouch;

    public static void StopNativeTouch()
    {
        //Log.i("Android Native Touch","Stopped");
        UnityPlayer.currentActivity.getCurrentFocus().setOnTouchListener(null);
    }

    // There should be only one static delegate waiting at Unity side. From there we can call multiple static callbacks.
    public static void RegisterTouchDelegate(TouchDelegate td, boolean minimal, boolean disableUnityTouch) {
        //Log.i("Android Native Touch","Registered");
        storedDelegate = td;
        isMinimalMode = minimal;
        isDisableUnityTouch = disableUnityTouch;
        UnityPlayer.currentActivity.getCurrentFocus().setOnTouchListener(nativeTouchListener);

        //Warm up
        storedDelegate.NativeTouchMinimalDelegate(-1,-1,-1,-1,-1,-1);
    }

    public interface TouchDelegate {
        void NativeTouchMinimalDelegate(int callbackType, float x, float y, int phase, long timestamp, int pointerId);
        void NativeTouchRawDelegate(int callbackType, float x, float y,  int phase, long timestamp, int pointerId,
                                    float orientation, float pressure,float size, float touchMajor, float touchMinor);
    }

    public static long AndroidTouchTime()
    {
        return SystemClock.uptimeMillis();
    }

    public static native void sendTouchMinimal(int callbackType, float x, float y, int phase, long timestamp, int pointerId);
    public static native void sendTouchFull(int callbackType, float x, float y,  int phase, long timestamp, int pointerId,
                                            float orientation, float pressure,float size, float touchMajor, float touchMinor);

}