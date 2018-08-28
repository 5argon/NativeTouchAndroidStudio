package com.exceed7.nativetouch;

import com.unity3d.player.*;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class NativeTouchActivity extends UnityPlayerActivity {

    // If you have an other custom activity and want to merge with Native Touch's function, you can copy all these to that.

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //Log.i("DISPATCH ", event.getX() + " " + event.getY() + " " + String.valueOf(event.getEventTime()));

        //Report the same timestamp for all touch even if they move or not.
        long timestamp = AndroidTouchTime();

        //MotionEvent might contains multiple touch data. We will callback once for each.
        if(storedDelegate != null) {

            int pointerCount = event.getPointerCount();
            int act = event.getActionMasked();
            boolean downAction = act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN;
            boolean upAction = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP;
            boolean moveAction = act == MotionEvent.ACTION_MOVE;

            for (int i = 0; i < pointerCount; i++) {

                int pointerId = event.getPointerId(i);
                int pointerIdOfAction;
                if(pointerCount > 1)
                {
                    pointerIdOfAction = event.getActionIndex();
                }
                else
                {
                    pointerIdOfAction = pointerId;
                }

                boolean actionOccurOnThisPointer;
                if(pointerCount > 1)
                {
                    actionOccurOnThisPointer = pointerIdOfAction == pointerId;
                }
                else
                {
                    actionOccurOnThisPointer = true;
                }

                float x = event.getX(i);
                float y = event.getY(i);
                int phase = 4; //If ACTION_CANCELLED it will not match on any ifs below, we return CANCELLED phase which is 3 in Android but 4 in Unity.
                if(downAction && actionOccurOnThisPointer)
                {
                    phase = MotionEvent.ACTION_DOWN;
                }
                else if(upAction && actionOccurOnThisPointer)
                {
                    phase = 3; //MotionEvent.ACTION_UP; //UP is 1 in Android but in Unity it's 3
                }
                else if(!actionOccurOnThisPointer || moveAction)
                {
                    //If action is not on this pointer we are not sure the others move or stay still. We always say move.
                    //If it is move action then also we don't know which one move or stay still. We always say move.

                    phase = 1; //MotionEvent.ACTION_MOVE; //MOVE is 2 in Android but in Unity it's 1
                }

                if(isMinimalMode) {
                    //If minimal mode we can start sending now!
                    storedDelegate.NativeTouchMinimalDelegate(x, y, phase, timestamp, pointerId);
                }
                else
                {
                    //Get more..
                    float orientation = event.getOrientation(i);
                    float pressure = event.getPressure(i);
                    float size = event.getSize(i);
                    float touchMajor = event.getTouchMajor(i);
                    float touchMinor = event.getTouchMinor(i);
                    storedDelegate.NativeTouchRawDelegate(x,y,phase, timestamp, pointerId,
                            orientation, pressure, size, touchMajor, touchMinor);
                }
            }
        }

        if(!isDisableUnityTouch) {
            //Unity input still works
            return super.dispatchTouchEvent(event);
        }
        else
        {
            return true;
        }
    }

    private static TouchDelegate storedDelegate;
    private static boolean isMinimalMode;
    private static boolean isDisableUnityTouch;

    // There should be only one static delegate waiting at Unity side. From there we can call multiple static callbacks.
    public static void RegisterTouchDelegate(TouchDelegate td, boolean minimal, boolean disableUnityTouch) {
        storedDelegate = td;
        isMinimalMode = minimal;
        isDisableUnityTouch = disableUnityTouch;
    }

    public interface TouchDelegate {
        void NativeTouchMinimalDelegate(float x, float y, int phase, long timestamp, int pointerId);
        void NativeTouchRawDelegate(float x, float y,  int phase, long timestamp, int pointerId,
                                    float orientation, float pressure,float size, float touchMajor, float touchMinor);
    }

    public static long AndroidTouchTime()
    {
        return SystemClock.uptimeMillis();
    }
}