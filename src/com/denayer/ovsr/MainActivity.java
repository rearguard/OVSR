package com.denayer.ovsr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.IplImage;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_highgui.*;

import com.lamerman.FileDialog;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

import java.security.*;



public class MainActivity extends Activity {
	public static String IP_ADDR="192.168.1.3";
	private Uri mImageCaptureUri;
	private ImageButton Input_button;
	private ImageButton Output_button;
	private Bitmap bitmap   = null;
	private Bitmap outBitmap   = null;
	private File file;
	private String Filter;
	private static final int PICK_FROM_CAMERA = 1;
	private static final int PICK_FROM_FILE = 2;
	private static final int REQUEST_LOAD = 3;
	private static final int REQUEST_SAVE = 4;
	private static final int REQUEST_SAVE_IMAGE = 5;
	private static final int SETTINGS = 6;
	private static final int PICK_VIDEO = 7;
	private static final int REQUEST_PATH = 8;
	private Button SubmitButton;
	private RadioButton RenderScriptButton;
	private RadioButton OpenCLButton;
	private EditText CodeField;
	public TextView TimeView;
	public TextView ConsoleView;
	public TextView NetworkView;
	public String fileName;
	public String CodeFieldCode;
	public String tabCodeString;
	public String tabConsoleString;
	public String tabLogString;
	OpenCL OpenCLObject;
	RsScript RenderScriptObject;
	LogFile LogFileObject;   
	private Button connectButton, disconnectButton;
	
	public String username = "";
	public String passwd = "";
	
	private TabHost myTabHost;

	public boolean isImage = false;
	public String videoPath;
	public String savePath;
	private Method m;
	
	private TcpClient mTcpClient;	
	MyFTPClient ftpclient = null;
	ProgressDialog dialog = null ;
	//item in de lijst toevoegen voor nieuwe filters toe te voegen.
	private String [] itemsFilterBox = new String [] {"Edge", "Inverse","Sharpen","Mediaan","Saturatie","Blur"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ActionBar actionBar = getActionBar();
		actionBar.setTitle("");
		
		ftpclient = new MyFTPClient();
		
		SharedPreferences settings = getSharedPreferences("Preferences", 0);
		SharedPreferences.Editor editor = settings.edit();
		if(!settings.getBoolean("AutoName", false))
		{
			editor.putBoolean("AutoName", false);
			editor.commit();
		}		

		Input_button = (ImageButton)findViewById(R.id.imageButton1);
		Output_button = (ImageButton)findViewById(R.id.imageButton2);
		SubmitButton=(Button) findViewById(R.id.submit_button);
		ConsoleView=(TextView)findViewById(R.id.ConsoleView);
		TimeView=(TextView)findViewById(R.id.timeview);
		NetworkView=(TextView)findViewById(R.id.networkview);
		CodeField=(EditText)findViewById(R.id.editText1);
		RenderScriptButton = (RadioButton) findViewById(R.id.radioButton1);
		OpenCLButton = (RadioButton) findViewById(R.id.radioButton2);
		connectButton = (Button) findViewById(R.id.connect_button);
		disconnectButton = (Button) findViewById(R.id.disconnect_button);

		myTabHost= (TabHost) findViewById(R.id.tabhost);
		myTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				if(tabId=="CodeTab")	
				{
					myTabHost.setCurrentTabByTag("Code");
				}
				else if(tabId=="ConsoleTab")
				{
					myTabHost.setCurrentTabByTag("Console");					
				}
				else if(tabId=="LogTab")
				{
					myTabHost.setCurrentTabByTag("Log");										
				}
			}
		});
		myTabHost.setup();
		TabSpec spec1 = myTabHost.newTabSpec("Code");
		spec1.setContent(R.id.CodeTab);
		spec1.setIndicator("Code");
		TabSpec spec2 = myTabHost.newTabSpec("Console");
		spec2.setContent(R.id.ConsoleTab);
		spec2.setIndicator("Console");        
		TabSpec spec3 = myTabHost.newTabSpec("Log");
		spec3.setContent(R.id.LogTab);
		spec3.setIndicator("Log");
		TabSpec spec4 = myTabHost.newTabSpec("Network");
		spec4.setContent(R.id.NetworkTab);
		spec4.setIndicator("Network"); 
		myTabHost.addTab(spec1);
		myTabHost.addTab(spec2);
		myTabHost.addTab(spec3);     
		myTabHost.addTab(spec4);


		OpenCLObject = new OpenCL(MainActivity.this,(ImageButton)findViewById(R.id.imageButton2));
		RenderScriptObject = new RsScript(this,(ImageButton)findViewById(R.id.imageButton2),TimeView);
		LogFileObject = new LogFile(this);   

//		final String [] items           = new String [] {"From Camera", "From SD Card", "Select Video"};
//		ArrayAdapter<String> adapter  = new ArrayAdapter<String> (this, android.R.layout.select_dialog_item,items);
//		AlertDialog.Builder builder     = new AlertDialog.Builder(this);

//		builder.setTitle("Select Image");
//		builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
//			@SuppressLint("SimpleDateFormat")
//			public void onClick( DialogInterface dialog, int item ) {
//				if (item == 0) {
//					Intent intent    = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//					//save file for camera
//					String SavePath = Environment.getExternalStorageDirectory().toString();
//					SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
//					Date now = new Date();
//					String fileCameraName = formatter.format(now) + ".jpg";
//					file = new File(SavePath, "OVSR"+fileCameraName);
//
//					mImageCaptureUri = Uri.fromFile(file);
//
//					try {
//						intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
//						intent.putExtra("return-data", true);
//
//						startActivityForResult(intent, PICK_FROM_CAMERA);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					dialog.cancel();
//				} else if(item==1) {
//					Intent intentLoad = new Intent(getBaseContext(), FileDialog.class);
//					intentLoad.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
//					intentLoad.putExtra(FileDialog.FORMAT_FILTER, new String[] { "png" , "jpeg" , "jpg" , "bmp"});
//					startActivityForResult(intentLoad, PICK_FROM_FILE);						
//				} else {
//					Intent intentLoad = new Intent(getBaseContext(), FileDialog.class);
//					intentLoad.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
//					intentLoad.putExtra(FileDialog.FORMAT_FILTER, new String[] {"png" , "jpeg" , "jpg" , "bmp", "mp4"});
//					startActivityForResult(intentLoad, PICK_VIDEO);
//				}
//			}
//		} );
//		final AlertDialog dialog = builder.create();
//		
		Input_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//dialog.show();
				createToast("Click on the images!", false);
			}
		});
		Output_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {  
				SharedPreferences settings = getSharedPreferences("Preferences", 0);
				if(settings.getBoolean("AutoName", false))
				{
					String filePath = null;
					//save the output bitmap to a file
					File picDir = new File(Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM + "/OpenCL/");
					picDir.mkdirs(); //creates directory when needed
					picDir = new File(Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM + "/RenderScript/");
					picDir.mkdirs(); //creates directory when needed
					//fileName created after filter
					filePath = Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM + File.separator + fileName + ".jpg";            	
					LogFileObject.writeToFile(" File saved to: " + filePath,"LogFile.txt",false);
					FileOutputStream out = null;
					try {
						out = new FileOutputStream(filePath);
						outBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
						createToast("Image saved!",false);	
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try{
							out.close();
						} catch(Throwable ignore) {}
					} 
				}
				else
				{
					//chose file
					Intent intentLoad = new Intent(getBaseContext(), FileDialog.class);
					intentLoad.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
					intentLoad.putExtra(FileDialog.FORMAT_FILTER, new String[] { "png" , "jpeg" , "jpg" , "bmp"});
					startActivityForResult(intentLoad, REQUEST_SAVE_IMAGE);
				}

			}
		}); 
		createBoxes();
		CodeField.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {
				CodeFieldCode = CodeField.getText().toString();			
			}
			@Override
			public void afterTextChanged(Editable s) {		
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {			
			}
		});

		final Handler handlerUi = new Handler();	

		SubmitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(CodeField.getText().toString()!=""){
					if(!RenderScriptButton.isChecked() ) 
					{
						if(TcpClient.isConnected)
						{
										
							//if user is not logged in
							if(username == "" && passwd == "")
							{
								final Dialog dialog = new Dialog(MainActivity.this);
								dialog.setContentView(R.layout.login);
							    dialog.setTitle("Login");
	
							    // get the Refferences of views
							    final  EditText editTextUserName=(EditText)dialog.findViewById(R.id.editTextUserNameToLogin);
							    final  EditText editTextPassword=(EditText)dialog.findViewById(R.id.editTextPasswordToLogin);
							    
							    editTextUserName.setText(username);
							    editTextPassword.setText(passwd);
							    
								Button btnSignIn=(Button)dialog.findViewById(R.id.buttonSignIn);
									
								// Set On ClickListener
								btnSignIn.setOnClickListener(new View.OnClickListener() {
									
									public void onClick(View v) {
										
										// get The User name and Password
										username=editTextUserName.getText().toString();
										passwd=editTextPassword.getText().toString();
										
										sendRenderscriptMessage(username, passwd);													
										
										dialog.dismiss();								
										
									}
								});
								
								dialog.show();
							}
							else
							{
								sendRenderscriptMessage(username, passwd);
							}				
						}
						else
							createToast("Not connected", false);
					}
					else 
					{
						if(OpenCLObject.getOpenCLSupport())
						{		
							OpenCLObject.codeFromFile(CodeField.getText().toString());	
							if(OpenCLObject.getBitmap()!=null)
							{
								outBitmap = OpenCLObject.getBitmap();
								Output_button.setImageBitmap(outBitmap);
								Log.i("Main","setImageBitmap done");
							}
							else createToast("Select image!",false);					
						}
						else createToast("No OpenCL support!",false);
					}
				}

			}
		});

		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				
				new ConnectTask().execute("");
				createToast("connecting to " + TcpClient.SERVER_IP, false);

			}
		});

		disconnectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(TcpClient.isConnected)
				{
					mTcpClient.stopClient();
				}
				createToast("disconnected", false);
				

			}
		});		

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x/2 - 15;
		int height = size.y/2 - 15;
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
		bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
		Input_button.setImageBitmap(bitmap);
		OpenCLObject.setBitmap(bitmap);
		RenderScriptObject.setInputBitmap(bitmap);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) return;
		String path     = "";
		if(requestCode == REQUEST_LOAD)
		{
			String PathLoadFile = data.getStringExtra(FileDialog.RESULT_PATH);
			String FileContent = null;
			Log.i("Debug",PathLoadFile);
			FileContent = LogFileObject.readFromFile(PathLoadFile,"");
			CodeField.setText(FileContent);
		}
		else if(requestCode== REQUEST_PATH)
		{
			savePath = data.getStringExtra(FileDialog.RESULT_PATH);
			editVideo(m,data.getBooleanExtra("isRs", true));
		}
		else if (requestCode == REQUEST_SAVE) {
			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);            	
			LogFileObject.writeToFile("		Code file saved to: " + filePath, "LogFile.txt",false);
			try{   
				if(!CodeFieldCode.equals("")){
					File CodeFile =new File(filePath);
					//if file doesnt exists, then create it
					if(!CodeFile.exists()){
						CodeFile.createNewFile();
					}
					FileWriter fileWritter = new FileWriter(CodeFile,true);
					BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
					bufferWritter.write(CodeFieldCode);
					bufferWritter.close();
				} 
			}catch (IOException e) {
				e.printStackTrace(); 
			}
		}
		else if (requestCode == PICK_FROM_FILE) {
			path = data.getStringExtra(FileDialog.RESULT_PATH);
			File f = new File(path);
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x/2 - 15;
			int height = size.y/2 - 15;
			bitmap = decodeAndResizeFile(f,height,width);
			setBitmaps();
		} else if (requestCode == PICK_FROM_CAMERA) {
			bitmap.recycle();
			bitmap  = BitmapFactory.decodeFile(mImageCaptureUri.getPath());
			try {                
				FileOutputStream out = new FileOutputStream(file);
				int BHeight = bitmap.getHeight()/2;
				int BWidth = bitmap.getWidth()/2;                	
				bitmap = Bitmap.createScaledBitmap(bitmap, BWidth, BHeight, false);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
				out.flush();
				out.close();
				MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
				file.delete(); //remove the temp file
			} catch (Exception e) {
				e.printStackTrace();
			}
			setBitmaps();
		} else if (requestCode == PICK_VIDEO){
			videoPath = data.getStringExtra(FileDialog.RESULT_PATH);
		} else if(requestCode == REQUEST_SAVE_IMAGE) {
			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			LogFileObject.writeToFile(" File saved to: " + filePath,"LogFile.txt",false);
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(filePath);
				outBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				createToast("Image saved!",false);	
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try{
					out.close();
				} catch(Throwable ignore) {}
			} 			
		}
		else if(requestCode == SETTINGS)
		{
			if(mTcpClient.isConnected)
			{
				//data from settingsactivity
				String str = data.getStringExtra("login");
				String[] split= str.split("\\s+");
				
				//login request
				if(split.length == 2)
				{
					username = split[0];
					passwd = split[1];
					String hash = createHash(passwd);						
					
					Log.i("tcp send","LOGIN " + username + " " + hash + " ENDLOGIN");
					mTcpClient.sendMessage("LOGIN " + username + " " + hash + " ENDLOGIN");
					
				}
				//create account request
				else if(split.length == 3)
				{	
					String newUser = split[0];
					String newPas = split[1];
					
					Log.i("tcp account", "ACCOUNT " + newUser + " " + newPas + " ENDACCOUNT");
					mTcpClient.sendMessage("ACCOUNT " + newUser + " " + newPas + " ENDACCOUNT");
					
					
				}
				else
					Log.i("main","error splitting string");
			}
			else
				createToast("Not connected", false);
						
						
		}
		System.gc();
	}
	public void setBitmaps()
	{
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x/2 - 15;
		int height = size.y/2 - 15;
		Log.i("Debug","Width: " + width + " " + "Height: " + height);

		bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
		Input_button.setImageBitmap(bitmap);
		Output_button.setImageBitmap(Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888));

		OpenCLObject.setBitmap(bitmap);
		RenderScriptObject.setInputBitmap(bitmap);
	}
	public String getRealPathFromURI(Uri contentUri) {
		String [] proj      = {MediaStore.Images.Media.DATA};
		Cursor cursor       = getContentResolver().query( contentUri, proj, null, null,null);

		if (cursor == null) return null;

		int column_index    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		cursor.moveToFirst();

		return cursor.getString(column_index);
	}
	public void createBoxes()
	{
		//choose box voor opencl of renderscript te selecteren
		ArrayAdapter<String> adapterFilterBox  = new ArrayAdapter<String> (this, android.R.layout.select_dialog_item,itemsFilterBox);        
		AlertDialog.Builder builderFilterBox     = new AlertDialog.Builder(this);
		builderFilterBox.setTitle("Select Filter");
		builderFilterBox.setAdapter( adapterFilterBox, new DialogInterface.OnClickListener() {
			public void onClick( DialogInterface dialogEdgeBox, int item ) {
				TimeView.setText("0");
				SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
				Date now = new Date();
				if(!RenderScriptButton.isChecked())
				{		
					fileName = "RenderScript/" + itemsFilterBox[item] + formatter.format(now);
					String FunctionName = "RenderScript" + itemsFilterBox[item];
					Filter = "RenderScript/" + itemsFilterBox[item];
					try {
						m = RsScript.class.getMethod(FunctionName);
						try {
							if(isImage){
								m.invoke(RenderScriptObject, null);
							}
							else
							{
								Intent intentLoad = new Intent(getBaseContext(), FileDialog.class);
								intentLoad.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
								intentLoad.putExtra(FileDialog.FORMAT_FILTER, new String[] {"mp4", "avi","3gp"});
								intentLoad.putExtra("isRs", true);
								startActivityForResult(intentLoad, REQUEST_PATH);
							}
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					if(RenderScriptObject.getOutputBitmap()!=null && isImage)
					{
						outBitmap = RenderScriptObject.getOutputBitmap();
						Output_button.setImageBitmap(RenderScriptObject.getOutputBitmap());
					}
					else
					{
						createToast("Select image!",false);	
					}
				}
				else
				{
					if(OpenCLObject.getOpenCLSupport())
					{		
						fileName = "OpenCL/" + itemsFilterBox[item] + formatter.format(now);
						String FunctionName = "OpenCL" + itemsFilterBox[item];
						Filter = "OpenCL/" + itemsFilterBox[item];
						try {
							//MainActivity obj = new MainActivity();
							m = OpenCL.class.getMethod(FunctionName);
							try {
								if(isImage){
									m.invoke(OpenCLObject, null);
								}
								else
								{
									Intent intentLoad = new Intent(getBaseContext(), FileDialog.class);
									intentLoad.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
									intentLoad.putExtra(FileDialog.FORMAT_FILTER, new String[] {"mp4", "avi","3gp"});
									intentLoad.putExtra("isRs", false);
									startActivityForResult(intentLoad, REQUEST_PATH);
								}
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}	
						if(OpenCLObject.getBitmap()!=null && isImage)
						{
							outBitmap = OpenCLObject.getBitmap();
							Output_button.setImageBitmap(outBitmap);
						}
						else
						{
							createToast("Select image!",false);					
						}
					}
					else
					{
						createToast("No OpenCL support!",false);					
					}
				}
				if(TimeView.getText()!="0")
				{
					String Method="OpenCL";
					if(!RenderScriptButton.isChecked()) Method="RenderScript";
					LogFileObject.writeToFile("\n" + Method + " : " + fileName + " : " + TimeView.getText(), "LogFile.txt",false);
				}
			}
		} );
		final AlertDialog dialogFilterBox = builderFilterBox.create();

		(findViewById(R.id.FilterButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialogFilterBox.show();
			}
		});
		//einde choose box

		//radio buttons

		RenderScriptButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				RenderScriptButton.setChecked(true);
				OpenCLButton.setChecked(false);
			}

		});
		OpenCLButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OpenCLButton.setChecked(true);
				RenderScriptButton.setChecked(false);
			}
		});
		ConsoleView.addTextChangedListener(new  TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				if(arg0.toString().contains("error"))
				{
					myTabHost.setCurrentTabByTag("Console");
				}
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {			
			}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {			
			}
		});
		//einde radio buttons
	}
	public void createToast(String Message,boolean isLong)
	{
		Context context = getApplicationContext();
		CharSequence text = Message;
		int duration = Toast.LENGTH_SHORT;
		if(isLong) duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.History:
			startHistoryActivity();
			return true;
		case R.id.Template:
			if(!RenderScriptButton.isChecked()) CodeField.setText(RenderScriptObject.getTemplate());
			else CodeField.setText(OpenCLObject.getTemplate());
			return true;
		case R.id.SaveF:
			Intent intentSave = new Intent(getBaseContext(), FileDialog.class);
			intentSave.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
			intentSave.putExtra(FileDialog.FORMAT_FILTER, new String[] { "txt" });
			startActivityForResult(intentSave, REQUEST_SAVE);
			return true;
		case R.id.LoadF:
			Intent intentLoad = new Intent(getBaseContext(), FileDialog.class);
			intentLoad.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
			intentLoad.putExtra(FileDialog.FORMAT_FILTER, new String[] { "txt" });
			startActivityForResult(intentLoad, REQUEST_LOAD);
			return true;   
		case R.id.Settings:
			Intent intent = new Intent(this,SettingsActivity.class);
			startActivityForResult(intent, SETTINGS);
			return true;
		case R.id.Camera:
			Intent intentCamera    = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			//save file for camera
			String SavePath = Environment.getExternalStorageDirectory().toString();
			SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
			Date now = new Date();
			String fileCameraName = formatter.format(now) + ".jpg";
			file = new File(SavePath, "OVSR"+fileCameraName);

			mImageCaptureUri = Uri.fromFile(file);

			try {
				intentCamera.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
				intentCamera.putExtra("return-data", true);
				startActivityForResult(intentCamera, PICK_FROM_CAMERA);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		case R.id.Video:
			Intent intentVideo = new Intent(getBaseContext(), FileDialog.class);
			intentVideo.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
			intentVideo.putExtra(FileDialog.FORMAT_FILTER, new String[] { "mp4" });
			startActivityForResult(intentVideo, PICK_VIDEO);
			return true;
		case R.id.Picture:
			Intent intentPicture = new Intent(getBaseContext(), FileDialog.class);
			intentPicture.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory() + File.separator + android.os.Environment.DIRECTORY_DCIM);
			intentPicture.putExtra(FileDialog.FORMAT_FILTER, new String[] {"png" , "jpeg" , "jpg" , "bmp"});
			startActivityForResult(intentPicture, PICK_FROM_FILE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	public void startHistoryActivity()
	{
		Intent intent = new Intent(this,DisplayMessageActivty.class);
		startActivity(intent);
	}
	public static Bitmap decodeAndResizeFile(File f,int Req_Height, int Req_Width) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);
			// Find the correct scale value. It should be the power of 2.
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < Req_Width || height_tmp / 2 < Req_Height)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}


	public class ConnectTask extends AsyncTask<String, String, TcpClient> {

		@Override
		protected TcpClient doInBackground(String... message) {
			//we create a TCPClient object
			mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
				@Override
				public void messageReceived(String message) {				
					Log.i("message","messageReceived: " + message);

					if(message.contains("Succesful"))
					{
						//TODO update consoleview
						ConsoleView.setText("Build Succesful");
						mTcpClient.sendMessage("give bc");
						Log.i("message","give bc");

					}    
					else if(message.contains("UPLOADED"))
					{
						//LogFileObject.writeToFile(byteCode, "template.bc", true); 
						//Connect to ftp server and fetch te file.
						new Thread(new Runnable() {
							public void run(){
								boolean status = false;
								Log.i("MainAct","FtpThread");
								// Replace your UID & PW here
								Log.i("ftp","ftp connect with " + username + " " + passwd);
								publishProgress("start");
								status = ftpclient.ftpConnect(IP_ADDR, username, passwd, 21);
								if (status == true) {
									Log.d("FTP", "Connection Success");
									status = ftpclient.ftpDownload("/template.bc", getFilesDir().getPath() + "/template.bc");
									publishProgress("stop");
									if(status){
									publishProgress("updateBitmap");
									}
									else
									{
										//Cannot create toasts in ftpthread
										//http://stackoverflow.com/questions/3875184/cant-create-handler-inside-thread-that-has-not-called-looper-prepare
											
										//createToast("Downloading failed", true);
									}
								} else {
									publishProgress("stop");
									Log.d("FTP", "Connection failed");
									//createToast("Connection with TCP server failed!", false);
									
									
									
									
								}
							}
						}).start();
					}
					else if(message.contains("login ok"))
					{
						publishProgress("login_ok");						
					}
					else if(message.contains("login error"))
					{
						publishProgress("login_nok");						
					}
					else if(message.contains("acount error"))
					{
						publishProgress("account_error");
					}
					else if(message.contains("acount created"))
					{
						publishProgress("acount_created");

					}
					else
					{
						publishProgress(message);
						Log.i("Error","Error message: " + message);
					}
				}
			});
			mTcpClient.run();

			return null;
		}
		//wanneer publishProgress opgeroepen wordt, word deze functie gecalled, 
		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			Log.i("onProgressUpdate",values[0]);
			if(values[0] == "updateBitmap"){
				RenderScriptObject.RenderScriptTemplate();
				Output_button.setImageBitmap(RenderScriptObject.getOutputBitmap()); 
			}
			else if(values[0]=="start")
			{
				dialog = ProgressDialog.show(MainActivity.this, "", "Processing. Please wait...", true); 
			}
			else if(values[0]=="stop")
			{
				dialog.dismiss();
			}
			else if(values[0] == "login_ok")
			{
				createToast("Login succesful", false);
				
			}
			else if(values[0] == "login_nok")
			{
				createToast("Wrong username or password", false);
				username = "";
				passwd = "";

			}
			else if(values[0] == "account_error")				
			{
				createToast("Accountname already in use", false);
			}
			else if(values[0] == "acount_created")
			{
				createToast("Acount created", false);
			}
			else
			{
				ConsoleView.append(values[0]);
				myTabHost.setCurrentTabByTag("Console");
			}
		}
	}
	
	String byteArrayToHex(byte[] a) {
		   StringBuilder sb = new StringBuilder();
		   for(byte b: a)
		      sb.append(String.format("%02x", b&0xff));
		   return sb.toString();
		}
	
	final protected static char[] hexArray = "0123456789abcdef".toCharArray();
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public String createHash(String passwd)
	{
		//create hash
		byte[] bytesOfMessage = null;
		try {
			bytesOfMessage = passwd.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] hash= md.digest(bytesOfMessage);
		
		Log.i("send",byteArrayToHex(hash));
		
		String strHash = bytesToHex(hash);
		
		return strHash;
		
	}
	
	public void sendRenderscriptMessage(String username, String passwd)
	{
		final Handler handlerUi = new Handler();
		
		ConsoleView.setText("");
		
		String message = CodeField.getText().toString();

		String lines[] = message.split("\\r?\\n");
		
		String strHash = createHash(passwd);
		
		Log.i("send after conversion",strHash);

		mTcpClient.sendMessage("STARTPACKAGE " + username + " " + strHash + " " + String.valueOf(android.os.Build.VERSION.SDK_INT)+ "\n");


		for(int i=0;i<lines.length;i++)
		{
			mTcpClient.sendMessage(lines[i]);
			Log.i("koen", lines[i]);							

		}
		//separator zodat de code en het ENDPACKAGE bericht niet aan elkaar kunnen hangen
		mTcpClient.sendMessage("\n");
		//wait some time
		handlerUi.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mTcpClient.sendMessage("ENDPACKAGE");
				Log.i("ENDPACKAGE","ENDPACKAGE");
			}

		},1000);
	}
	public void editVideo(Method m, boolean isRs)
	{
		try {
			FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath); 
			grabber.start();
			Log.i("Height",String.valueOf(grabber.getImageHeight()));
			Log.i("Width",String.valueOf(grabber.getImageWidth()));
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(savePath, grabber.getImageWidth(), grabber.getImageHeight());

			recorder.setFormat("mp4");
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
			recorder.setVideoBitrate(33000);
			recorder.setFrameRate(24);				

			IplImage image = IplImage.create(grabber.getImageWidth(), grabber.getImageHeight(), IPL_DEPTH_8U, 4);
			IplImage frame2 = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, 4);
			Bitmap MyBitmap = Bitmap.createBitmap(frame2.width(), frame2.height(), Bitmap.Config.ARGB_8888);   
			recorder.start();
			while(true)
			{					
				image = grabber.grab();
				if(image==null)
				{
					break;
				}
				opencv_imgproc.cvCvtColor(image, frame2, opencv_imgproc.CV_BGR2RGBA);
				MyBitmap.copyPixelsFromBuffer(frame2.getByteBuffer());

				if(isRs)
				{
					RenderScriptObject.setInputBitmap(MyBitmap);
					m.invoke(RenderScriptObject, null);
					MyBitmap = RenderScriptObject.getOutputBitmap();		            	
				}
				else
				{
					OpenCLObject.setBitmap(MyBitmap);
					m.invoke(OpenCLObject, null);
					MyBitmap = OpenCLObject.getBitmap();		            	
				}
				MyBitmap.copyPixelsToBuffer(frame2.getByteBuffer());
				opencv_imgproc.cvCvtColor(frame2, image, opencv_imgproc.CV_RGBA2BGR);		            
				recorder.record(image);
			}
			recorder.stop();
			grabber.stop();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	



}