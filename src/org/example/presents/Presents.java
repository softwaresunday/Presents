package org.example.presents;

import android.app.Activity;
import android.media.MediaPlayer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.os.Debug;
import java.lang.Math;

public class Presents extends Activity
	implements OnTouchListener, AccelerometerListener {
   private static final String TAG = "Presents";
   // These matrices will be used to move and zoom image
   Matrix matrix = new Matrix();
   Matrix savedMatrix = new Matrix();
   static float xLoc = (float) 1100.0;
   static float yLoc = (float) 650.0;
   static float newXLoc;
   static float newYLoc;
   static float mx,my,mz;
   static final float xMin = (float)0.0;
   static final float xMax = (float)1900.0;
   static final float yMin = (float)0.0;
   static final float yMax = (float)1550.0;
   static float tX = (float)0.0;
   static float tY = (float)0.0;
   private MediaPlayer affirm;
   private MediaPlayer deny;
   private static Context CONTEXT;

   // We can be in one of these 3 states
   static final int NONE = 0;
   static final int DRAG = 1;
   static final int ZOOM = 2;
   int mode = NONE;

   // Remember some things for zooming
   PointF start = new PointF();
   PointF mid = new PointF();
   float oldDist = 1f;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // start tracing to "/sdcard/tuch.trace"
//      android.os.Debug.startMethodTracing("tuch");
      Context mc = getApplicationContext();
      affirm = MediaPlayer.create(mc,R.raw.gotit);
      deny = MediaPlayer.create(mc,R.raw.nope);  		 

      setContentView(R.layout.main);
      CONTEXT = this;
      ImageView view = (ImageView) findViewById(R.id.imageView);
      view.setOnTouchListener(this);
      matrix.postTranslate(-xLoc,-yLoc);
      view.setImageMatrix(matrix);
   }
   protected void onResume() {
	   super.onResume();
	   if (AccelerometerManager.isSupported()){
		   AccelerometerManager.startListening(this);
	   }
   }
   protected void onDestroy() {
	   super.onDestroy();
	   if (AccelerometerManager.isListening()){
		   AccelerometerManager.stopListening();
	   }
   }
   public static Context getContext() {
	   return CONTEXT;
   }

   public boolean onTouch(View v, MotionEvent event) {
	  
      ImageView view = (ImageView) v;


      // Handle touch events here...
      switch (event.getAction() & 0xFF) {
      case MotionEvent.ACTION_DOWN:
         savedMatrix.set(matrix);
         start.set(event.getX(), event.getY());
         Log.d(TAG, "mode=DRAG");
         mode = DRAG;
         break;
      case MotionEvent.ACTION_UP:
         mode = NONE;
         Log.d(TAG, "mode=NONE");
         xLoc -= tX;
         yLoc -= tY;
         if (Math.abs(start.x-event.getX())<10 && Math.abs(start.y-event.getY())<10)
         {
        	 onScan(v);
         }
         break;
      case MotionEvent.ACTION_MOVE:
         if (mode == DRAG) {
            // ...
            matrix.set(savedMatrix);
            tX = event.getX()-start.x;
            tY = event.getY()-start.y;
            newXLoc = xLoc - tX;
            newYLoc = yLoc - tY;
            if (newXLoc<xMin || newXLoc>xMax) {tX = (float)0.0;}
            if (newYLoc<yMin || newYLoc>yMax) {tY = (float)0.0;}
            matrix.postTranslate(tX,tY);
         }
         else if (mode == ZOOM) {
            float newDist = spacing(event);
            Log.d(TAG, "newDist=" + newDist);
            if (newDist > 10f) {
               matrix.set(savedMatrix);
               float scale = newDist / oldDist;
               matrix.postScale(scale, scale, mid.x, mid.y);
            }
         }
         break;
      }

      view.setImageMatrix(matrix);
      return true; // indicate event was handled
   }


   /** Determine the space between the first two fingers */
   private float spacing(MotionEvent event) {
      float x = event.getX();
      float y = event.getY();
      return FloatMath.sqrt(x * x + y * y);
   }
   
   public void onScan(View v) {
	        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
	    }

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == 0) {
	        if (resultCode == RESULT_OK) {
	            String contents = intent.getStringExtra("SCAN_RESULT");
	            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
	           if (contents.length() > 30)
	           {
	        	   if (affirm != null) deny.start();
	           }
	           else {
	        	   if (deny != null) affirm.start();
	           }
	            
	            // Handle successful scan
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    }
	}
	
	public void onAccelerationChanged(float x, float y, float z) {
		if (Math.abs(x-mx) > 1.5 || Math.abs(y-my) > 1.5 || Math.abs(z-mz) > 1.5)
		{
			affirm.start();
		}
		mx =x; my = y; mz =z;	
	}

	public void onShake(float force) {
		 if (force > 0.2) affirm.start();
	}

}
