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
            //Log.i("DISPATCH ", event.getX() + " " + event.getY() + " Time : " + String.valueOf(event.getEventTime()) +
            //" Action : " + event.getActionMasked() + " Pointer count : " + event.getPointerCount());

            //Report the same timestamp for all touch even if they move or not.
            double timestamp = (double)AndroidTouchTime();

            //MotionEvent might contains multiple touch data. We will callback once for each.

                int pointerCount = event.getPointerCount();
                int act = event.getActionMasked();
                boolean downAction = act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN;
                boolean upAction = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP;
                boolean moveAction = act == MotionEvent.ACTION_MOVE;
                boolean cancelledAction = act == MotionEvent.ACTION_CANCEL;

                //Determine the callback type from the "main" action. Used with every touches in this MotionEvent.
                int callbackType = 4; //cancelled
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
                    int phase = 4;
                    if (downAction && actionOccurOnThisPointer) {
                        phase = MotionEvent.ACTION_DOWN;
                    } else if (upAction && actionOccurOnThisPointer) {
                        phase = 3; //MotionEvent.ACTION_UP; //UP is 1 in Android but in Unity it's 3
                    } else if (cancelledAction) {
                        // Cancelled action will put all touches in the pack as Cancelled.
                        // Does not care about whether if the action is on this touch or not.
                        phase = 4;
                    }else if (!actionOccurOnThisPointer || moveAction) {
                        //If action is not on this pointer we are not sure the others move or stay still. We always say move.
                        //If it is move action then also we don't know which one move or stay still. We always say move.
                        phase = 1; //MotionEvent.ACTION_MOVE; //MOVE is 2 in Android but in Unity it's 1
                    }

                    if (isMinimalMode) {
                        //If minimal mode we can start sending now!
                        //Log.i("EACH", x + " " + y + " Action? " + actionOccurOnThisPointer + " ("+ pointerIdOfAction +") ID " + pointerId );
                        sendTouchMinimal(callbackType, x, y, phase, timestamp, pointerId);
                    } else {
                        //Get more..
                        float orientation = event.getOrientation(i);
                        float pressure = event.getPressure(i);
                        float size = event.getSize(i);
                        float touchMajor = event.getTouchMajor(i);
                        float touchMinor = event.getTouchMinor(i);
                        sendTouchFull(callbackType, x, y, phase, timestamp, pointerId,
                                orientation, pressure, size, touchMajor, touchMinor);
                    }
                }

            if (!isDisableUnityTouch) {
                //Unity input still works
                UnityPlayer.currentActivity.onTouchEvent(event);
            }
            return true;
        }

    };

    private static boolean isMinimalMode;
    private static boolean isDisableUnityTouch;

    public static void StopNativeTouch()
    {
        //Log.i("Android Native Touch","Stopped");
        UnityPlayer.currentActivity.getCurrentFocus().setOnTouchListener(null);
    }

    public static void StartNativeTouch(boolean fullMode, boolean disableUnityTouch) {
        //Log.i("Android Native Touch","Registered");
        isMinimalMode = !fullMode;
        isDisableUnityTouch = disableUnityTouch;
        UnityPlayer.currentActivity.getCurrentFocus().setOnTouchListener(nativeTouchListener);
    }

    public static int RealScreenHeight()
    {
        return UnityPlayer.currentActivity.getCurrentFocus().getHeight();
    }

    public static int RealScreenWidth()
    {
        return UnityPlayer.currentActivity.getCurrentFocus().getWidth();
    }

    public static long AndroidTouchTime()
    {
        return SystemClock.uptimeMillis();
    }

    public static native void sendTouchMinimal(int callbackType, float x, float y, int phase, double timestamp, int pointerId);
    public static native void sendTouchFull(int callbackType, float x, float y,  int phase, double timestamp, int pointerId,
                                            float orientation, float pressure,float size, float touchMajor, float touchMinor);

}