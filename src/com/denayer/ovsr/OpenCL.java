package com.denayer.ovsr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class OpenCL extends Object {
	private Context mContext; //<-- declare a Context reference
	Bitmap bmpOrig, bmpOpenCL;
	float saturatie=0;
	public ImageButton outputButton;
	final int info[] = new int[3]; // Width, Height, Execution time (ms)
	static boolean sfoundLibrary = true;

 
	public OpenCL(Context context, ImageButton outImageButton) {
    	mContext = context; //<-- fill it with the Context you passed
    	outputButton = outImageButton;
    	try { 
    		System.load("/system/vendor/lib/libPVROCL.so");
    		Log.i("Debug", "libPVROCL Loaded"); 
    	}
    	catch (UnsatisfiedLinkError e) {
    		sfoundLibrary = false;
    	}
    	if(sfoundLibrary==false)
    	{
	    	try { 
	    		System.load("/system/vendor/lib/egl/libGLES_mali.so");
	    		Log.i("Debug", "libGLES_mali loaded");
	    	}
	    	catch (UnsatisfiedLinkError e) {
	    		sfoundLibrary = false;
	    	}
    	}
    	try {
    		System.loadLibrary("OVSR");  
    		Log.i("Debug","My Lib Loaded!");
    	}
    	catch (UnsatisfiedLinkError e) {
    		Log.e("Debug", "Error log", e);
    	} 	
    }
	public boolean getOpenCLSupport(){
		return sfoundLibrary;
	}
    public void setBitmap(Bitmap bmpOrigJava)
    {
    	bmpOrig = bmpOrigJava;
        info[0] = bmpOrig.getWidth();
        info[1] = bmpOrig.getHeight();
        bmpOpenCL = Bitmap.createBitmap(info[0], info[1], Bitmap.Config.ARGB_8888);
    }
    public Bitmap getBitmap()
    {
    	return bmpOpenCL;
    }
    private native void initOpenCL (String kernelName);
    private native void nativeBasicOpenCL (
            Bitmap inputBitmap,
            Bitmap outputBitmap
        );
    private native void nativeSaturatieOpenCL(
            Bitmap inputBitmap,
            Bitmap outputBitmap,
            float saturatie
        );
    private native void nativeImage2DOpenCL(
            Bitmap inputBitmap,
            Bitmap outputBitmap
        );
    private native void shutdownOpenCL ();
	
	public void OpenCLEdge ()
	{
		if(bmpOrig == null)
			return;
		Log.i("OpenCL","OpenCLEdge");
    	copyFile("edge.cl");
    	String kernelName="edge";
    	Log.i("DEBUG","BEFORE runOpencl");
    	initOpenCL(kernelName);
    	nativeBasicOpenCL(
                bmpOrig,
                bmpOpenCL
            );
    	shutdownOpenCL();
    	Log.i("DEBUG","AFTER runOpencl");
	}
	public void OpenCLInverse ()
	{
		if(bmpOrig == null)
			return;
		Log.i("OpenCL","OpenCLInverse");
    	copyFile("inverse.cl");
    	String kernelName="inverse";
    	Log.i("DEBUG","BEFORE runOpencl");
    	initOpenCL(kernelName);
    	nativeBasicOpenCL(
                bmpOrig,
                bmpOpenCL
            );
    	shutdownOpenCL();
    	Log.i("DEBUG","AFTER runOpencl");
	}
	public void OpenCLSharpen ()
	{
		if(bmpOrig == null)
			return;
		Log.i("OpenCL","OpenCLSharpen");
    	copyFile("sharpen.cl");
    	String kernelName="sharpen";
    	Log.i("DEBUG","BEFORE runOpencl sharpen");
    	initOpenCL(kernelName);
    	nativeBasicOpenCL(
 //   	nativeImage2DOpenCL( //TODO nativeImage2DOpenCL testen
                bmpOrig,
                bmpOpenCL
            );
    	shutdownOpenCL();
    	Log.i("DEBUG","AFTER runOpencl sharpen");
	}
	public void OpenCLMediaan ()
	{
		if(bmpOrig == null)
			return;
		Log.i("OpenCL","OpenCLMediaan");
    	copyFile("mediaan.cl");
    	String kernelName="mediaan";
    	Log.i("DEBUG","BEFORE runOpencl Mediaan");
    	initOpenCL(kernelName);
    	nativeBasicOpenCL(
                bmpOrig,
                bmpOpenCL
            );
    	shutdownOpenCL();
    	Log.i("DEBUG","AFTER runOpencl Mediaan");
	}
	public void OpenCLBlur ()
	{
		if(bmpOrig == null)
			return;
		Log.i("OpenCL","OpenCLBlur");
    	copyFile("blur.cl");
    	String kernelName="blur";
    	Log.i("DEBUG","BEFORE runOpencl blur");
    	initOpenCL(kernelName);
    	nativeBasicOpenCL(
                bmpOrig,
                bmpOpenCL
            );
    	shutdownOpenCL();
    	Log.i("DEBUG","AFTER runOpencl blur");
	}	
	public void OpenCLSaturatie ()
	{	
		if(bmpOrig == null)
			return;
		Log.i("DEBUG","OPENCLSATURATIE");

		final TextView progressView = new TextView(mContext);
		final Resources res = mContext.getResources();
		final SeekBar MySeekBar = new SeekBar(mContext);

		MySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ 
			   @Override 
			   public void onProgressChanged(SeekBar seekBar, int progress, 
			     boolean fromUser) { 
			    //  Auto-generated method stub 
				   progressView.setText(String.valueOf(progress)); 
			   } 
			   @Override 
			   public void onStartTrackingTouch(SeekBar seekBar) { 
			    //  Auto-generated method stub 
			   } 
			   @Override 
			   public void onStopTrackingTouch(SeekBar seekBar) { 
			    //  Auto-generated method stub 
			   } 
			       }); 
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);        
        builder.setMessage("saturation value")
               .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   saturatie = MySeekBar.getProgress();
                	   saturate();
                	   outputButton.setImageBitmap(bmpOpenCL);
                   }
               });
        progressView.setGravity(1 | 0x10);
        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
	     LinearLayout ll=new LinearLayout(mContext);
	        ll.setOrientation(LinearLayout.VERTICAL);
	        ll.addView(MySeekBar);
	        ll.addView(progressView);
	        dialog.setView(ll);
        dialog.show(); 
	}	
	private void saturate()
	{
		Log.i("OpenCL","OpenCLSaturatie");
    	copyFile("saturatie.cl");
    	String kernelName="saturatie";
    	Log.i("DEBUG","BEFORE runOpencl Saturatie");
    	initOpenCL(kernelName);
    	nativeSaturatieOpenCL(
                bmpOrig,
                bmpOpenCL,
                saturatie
            );
    	shutdownOpenCL();
    	Log.i("DEBUG","AFTER runOpencl Saturatie");		
	}
	private void copyFile(final String f) {
		InputStream in;
		try {
			in = mContext.getAssets().open(f);
			final File of = new File(mContext.getDir("execdir",Context.MODE_PRIVATE), f);

			final OutputStream out = new FileOutputStream(of);

			final byte b[] = new byte[65535];
			int sz = 0;
			while ((sz = in.read(b)) > 0) {
				out.write(b, 0, sz);
			}
			in.close();
			out.close();
		} catch (IOException e) {       
			e.printStackTrace();
		}
	}
	public void setTimeFromJNI(float time)
	{
		Log.i("setTimeFromJNI","Time set on " + String.valueOf(time));
		time = (float) (Math.round(time*1000000.0) / 1000000.0);		
		View rootView = ((Activity)mContext).getWindow().getDecorView().findViewById(android.R.id.content);
		TextView v = (TextView) rootView.findViewById(R.id.timeview);
		v.setText(String.valueOf(time) + " seconds");
		
	}
}
