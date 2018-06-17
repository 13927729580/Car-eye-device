/*  car eye ��������ƽ̨ 
 * car-eye����ƽ̨   www.car-eye.cn
 * car-eye��Դ��ַ:  https://github.com/Car-eye-team
 * Copyright
 */

package com.sh.camera.service;


import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import org.push.push.Pusher;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore.Video;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.dss.car.launcher.provider.biz.ProviderBiz;
import com.sh.camera.FileActivity;
import com.sh.camera.R;
import com.sh.camera.SessionLinearLayout;
import com.sh.camera.SetActivity;
import com.sh.camera.DiskManager.DiskManager;
import com.sh.camera.ServerManager.ServerManager;
import com.sh.camera.codec.MediaCodecManager;
import com.sh.camera.util.AppLog;
import com.sh.camera.util.CameraUtil;
import com.sh.camera.util.Constants;
import com.sh.camera.util.ExceptionUtil;
import com.sh.camera.version.VersionBiz;

@SuppressLint("NewApi")
@SuppressWarnings("unused")
public class MainService extends Service {

	private static final String TAG = "CMD";
	public static Context c;
	private static MainService instance;
	public static Context application;	
	LayoutInflater inflater;
	public static boolean isrun = false;
	/**�������Ƿ�����ǰ����ʾ״̬*/
	public static boolean isWindowViewShow = true;
	public static String ACTION = "com.dss.car.dvr";
	//����������ȫ��
	public static String FULLSCREEN = "fullscreen";
	//����������ȫ��������һ�δ��ڻ�ָ��
	public static String PASSWINFULL = "passwinfullscreen";
	//�������������ڻ�
	public static String WINDOW = "window";
	//������������С��
	public static String MINIMIZE = "minimize";
	//����Ԥ����������
	public static String RESTART = "restart";
	//֪ͨ��ʼ¼��
	public static String STARTRECORDER = "startrecorder";
	//֪ͨ��ʼ�ϴ�
	public static String STARTPUSH = "startpush";
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~��ʼ����Ҫ���ܿؼ�~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//��Ҫ��ʾ�ؼ�
	public TextureView[] ttvs;
	private SurfaceTexture[] stHolder;
	//��ť����
	private LinearLayout ly_bts;
	//����ͷ����
	public static Camera[] camera;
	private static boolean avaliable[]= {false, false, false, false};
	static PreviewCallback[] preview;
	private MediaRecorder[] mrs;
	private String[] MrTempName;
	private ContentValues[] mCurrentVideoValues;
	public static SurfaceTextureListener[] stListener;
	//����ͷid
	public static int[] cid = null;
	//�ܿ�����ͷ
	public static int[] rules;
	//��¼��ǰ¼����������㣬δ¼��ʱ-1��
	long recoTime = -1;
	//�ؼ�id����
	private int[] ttvids = {R.id.textureview1, R.id.textureview2, R.id.textureview3, R.id.textureview4};
	private ImageView btiv1,btiv2;
	private LinearLayout[] lys;
	private int[] lyids = {R.id.ly_1_0, R.id.ly_1_1, R.id.ly_1_2, R.id.ly_2_0, R.id.ly_2_1, R.id.ly_2_2};
	private boolean isTwoCamera = true;
	public static int[] StreamIndex;
	public static boolean clickLock = false;
	public static boolean[] sc_controls = {false, false, false, false};
	int framerate = Constants.FRAMERATE;
	int bitrate;
	public static DiskManager disk;	
	public static Pusher mPusher;	
	//�ж����˳����Ǵ���������
	boolean isClose = true;	
	//֪ͨ����¼��
	public static String STOPRECORDER = "stoprecorder";
	//֪ͨ�����ϴ�
	public static String STOPPUSH = "stoppush";
	BroadcastReceiver 	SYSBr;	
	boolean usbcameraConnect = true;
	boolean sd_inject = false;	
	private String longitude = ""; // ����
	private String latitude = ""; // ά��
	private LocationManager lm;
	// ��ȡ����application�Ķ���
	public static MainService getInstance() {
		if (instance == null) {
			instance = new MainService();
		}
		return instance;
	}	
	public static DiskManager getDiskManager()
	{
		return disk;
	}	
	private boolean isTabletDevice = true;

	public void onCreate() {
		super.onCreate();
		isTabletDevice = isTabletDevice(this);

		instance = this;
		c = MainService.this;
		application = getApplicationContext();
		disk = new DiskManager(this);			
		mPusher = new Pusher();
		StreamIndex = new int[Constants.MAX_NUM_OF_CAMERAS];
		camera = new Camera[Constants.MAX_NUM_OF_CAMERAS];
		mrs = new MediaRecorder[Constants.MAX_NUM_OF_CAMERAS];
		MrTempName = new String[Constants.MAX_NUM_OF_CAMERAS];
		mCurrentVideoValues = new ContentValues[Constants.MAX_NUM_OF_CAMERAS];
		framerate = ServerManager.getInstance().getFramerate();
		CreateView();		
		//һ��ʼ�ͳ�ʼ����������̫ռ����Դ		
		isrun = true;			
		Constants.setParam(c);
		cid = Constants.CAMERA_ID;
		inflater = LayoutInflater.from(c);
		registerReceiver(br, filter);
			
		disk.CreateDirctionaryOnDisk(Constants.CAMERA_FILE_DIR);
		new Thread(new Runnable() {
			@Override
			public void run() {
				int count = 0;
				while (isrun) {
					try {
						if(recoTime>0&&new Date().getTime()-recoTime>1000*60*Constants.VIDEO_TIME){
							handler.sendMessage(handler.obtainMessage(1002));
							Thread.sleep(2000);
						}else if(recoTime>0&&disk.GetDiskFreeTotal()<=Constants.SD_FREEJX){
							//SdCardBiz.getInstance().getDetectionServiceSdCar(Constants.isCleaning,instance);
							disk.getDetectionServiceSdCar(instance);							
						}
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					count++;
					if(count == 8)
					{
						if(Constants.StartFlag == true)
						{
							Constants.StartFlag = false;
							Intent intent = new Intent(MainService.ACTION);
							intent.putExtra("type", MainService.MINIMIZE);
							sendBroadcast(intent);
						}
					}						
				}
			}
		}).start();
		
	}	
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		isrun = true;
		final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
		SYSBr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) { 

				String action = intent.getAction();
				UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);			   		
				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action) && device.getDeviceProtocol() ==1) {          
					Toast.makeText(context, "������usb����ͷ�䶯1"+device.getDeviceProtocol(), Toast.LENGTH_LONG).show();
					usbcameraConnect = false;    
					closeCamera(0);   		 
				} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) && device.getDeviceProtocol()==1) {
					Toast.makeText(context, "������usb����ͷ�䶯0"+device.getDeviceProtocol(), Toast.LENGTH_LONG).show();
					try {
						Thread.sleep(500);
					} catch (Exception e) {
					}
					usbcameraConnect = true;
					openCamera(0, 2);   
				}				
				else if(action.equals(Constants.ACTION_VIDEO_PLAYBACK))
				{
					int id = intent.getIntExtra("EXTRA_ID", 1);  //ͨ��ID
					int type = intent.getIntExtra("EXTRA_TYPE", 0);  //����  0 ͼƬ 1 ¼��
					String stime = intent.getStringExtra("EXTRA_STIME");  //�طſ�ʼʱ��
					String etime = intent.getStringExtra("EXTRA_ETIME");  //�طŽ���ʱ��
							
				}else if(action.equals(Constants.ACTION_VIDEO_FILE_PLAYBACK))
				{		 			
					int cameraid = intent.getIntExtra("Channel", 1);  //ͨ��ID
					String filename = intent.getStringExtra("Name");
					int splaysec = intent.getIntExtra("Start", 0); 
					int eplaysec = intent.getIntExtra("End", 0);
					CameraUtil.startVideoFileStream(cameraid, splaysec, eplaysec, filename,null);					
				}
				else if(action.equals(Constants.ACTION_VIDEO_FILE_PLAYBACK))
				{
					CameraUtil.stopVideoFileStream();
				}
				if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
				{
					//Toast.makeText(context, "������home key", Toast.LENGTH_LONG).show();
					MainService.getInstance().setWindowMin();
				}
			}
		};	      
		IntentFilter localIntentFilter = new IntentFilter();  
		localIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);	
		localIntentFilter.addAction(Constants.ACTION_VIDEO_PLAYBACK);        
		localIntentFilter.addAction(Constants.ACTION_VIDEO_FILE_PLAYBACK);   
		localIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		localIntentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS); 		
		registerReceiver(SYSBr, localIntentFilter);	        		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		isrun = false;
		unregisterReceiver(br);
		
		unregisterReceiver(SYSBr);
		//ȡ������
		Log.d("main service", "onDestroy");
	};

	
	//passһ��window
	boolean passwin = false;
	IntentFilter filter = new IntentFilter(ACTION);
	BroadcastReceiver br = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent intent) {
			String type = intent.getStringExtra("type");
			
			if(type.equals(WINDOW)){
				//				if(passwin){
				//					passwin = false;
				//				}else{
				//					setWindowWin();
				//				}
				ProviderBiz providerBiz = ProviderBiz.getInstance(c);
				int mainStatus = providerBiz.getDeviceInfo().getMainStatus();
				if(mainStatus == 1){
					setWindowWin();
				}
			}
			if(type.equals(MINIMIZE)){
				setWindowMin();
			}
			if(type.equals(FULLSCREEN)){
				setWindowFull();
			}
			if(type.equals(PASSWINFULL)){
				passwin = true;
				setWindowFull();
			}
			if(type.equals(RESTART)){
				passwin = true;
				restart();
			}
			if(type.equals(STARTRECORDER)){				
				int index = intent.getIntExtra("index", 0);
				/*if(!isRecording){
					click(R.id.bt_ly_2);
				}*/
				prepareRecorder(index, 1);				
			}
			if(type.equals(STOPRECORDER)){
				if(isRecording){
					click(R.id.bt_ly_2);
				}
			}
			if(type.equals(STARTPUSH)){
				if(!isSC){
					click(R.id.bt_ly_3);
				}
			}
			if(type.equals(STOPPUSH)){
				if(isSC){
					click(R.id.bt_ly_3);
				}
			}
		}

	};

	private void restart() {
		isrun = true;
		Constants.setParam(MainService.getInstance());
		StopCameraprocess();
		removeView();
		addView();
	}
	//��С��
	void setWindowMin(){
		ismatch = true;
		ly_bts.setVisibility(view.VISIBLE);
		wmParams.x = 1;
		wmParams.y = 1;
		
		wmParams.width = 1;
		wmParams.height = 1;
		//��С������̨����Ҫ����LayoutParams.FLAG_NOT_FOCUSABLE������ȡ���Է��ؼ������أ������Ƴ�layout
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | 
				LayoutParams.FLAG_NOT_FOCUSABLE | 
				WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
		if(layoutPoint != null && layoutPoint.isShown()){
			mWindowManager.removeView(layoutPoint);
		}

		mWindowManager.updateViewLayout(view, wmParams);
		for (int i = 0; i < lys.length; i++) {
			if(i!=0&&i!=3){
				lys[i].setOnClickListener(click2start);
			}
		}
		isWindowViewShow = false;
	}	
	
	//���
	void setWindowFull(){
		ismatch = true;
		ly_bts.setVisibility(view.VISIBLE);
		wmParams.x = 0;
		wmParams.y = 0;
		wmParams.width =  WindowManager.LayoutParams.MATCH_PARENT;
		wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;		
		//��󻯣���Ҫ����LayoutParams.FLAG_NOT_FOCUSABLE���������ط��ؼ�	
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | 
					WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
		mWindowManager.updateViewLayout(view, wmParams);
		
		//��󻯣����layout���������ط��ؼ�������Ϊ1���Ų��ᵲס����
		wmParams.width = 1;
		wmParams.height = 1;

		if(layoutPoint != null && layoutPoint.isShown()){
			mWindowManager.updateViewLayout(layoutPoint, wmParams);
		}else{
			mWindowManager.addView(layoutPoint, wmParams);
		}

		for (int i = 0; i < lys.length; i++) {
			if(i!=0&&i!=3){
				lys[i].setOnClickListener(click_ly);
			}
		}		
		isWindowViewShow = true;
		if(Constants.checkVersion){
			Constants.checkVersion = false;
			VersionBiz.doCheckVersionFirst(c, handler);
		}
	}

	//���ڻ�
	void setWindowWin(){
		ismatch = false;
		ly_bts.setVisibility(view.GONE);
		wmParams.x = 1;
		wmParams.y = 1;
		wmParams.width = 1;
		wmParams.height = 1;
		
		mWindowManager.updateViewLayout(view, wmParams);
		for (int i = 0; i < lys.length; i++) {
			if(i!=0&&i!=3){
				lys[i].setOnClickListener(click2start);
			}
		}

	}

	boolean ismatch = false;
	OnClickListener click2start = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(ismatch){
				setWindowWin();
			}else{
				///setWindowFull();
			}
		}
	};


	LayoutParams wmParams;
	WindowManager mWindowManager;
	View view;
	// һ���㣬������Window�У������������ؼ�����С�����Ƴ������ʱ���ӵ�window�С�
	SessionLinearLayout layoutPoint;
	// ��������  
	float lastX, lastY;  
	int oldOffsetX, oldOffsetY;  
	private void CreateView() {
		mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
		wmParams = new WindowManager.LayoutParams();
		wmParams.type = LayoutParams.TYPE_TOAST;
		wmParams.format = PixelFormat.RGBA_8888;
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		wmParams.x = 0;
		wmParams.y = 0;
		wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		addView();
						
	}
	private void addView() {
		if(inflater==null){
			inflater = LayoutInflater.from(c);
		}
		view = inflater.inflate(R.layout.activity_main, null);
		layoutPoint = (SessionLinearLayout) inflater.inflate(R.layout.layout_point, null);
		layoutPoint.setDispatchKeyEventListener(mDispatchKeyEventListener);
		initView();
		mWindowManager.addView(view, wmParams);
		view.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		//		view.setOnClickListener(click2start);
		setWindowFull();
	}

	public void StopCameraprocess()
	{
		if(isRecording){
			btiv1.setImageResource(R.drawable.a02);
			for (int i = 0; i < rules.length; i++) {
				stoprecorder(rules[i],i);
			}
			isRecording = false;
		}

		if(isSC){
			stopSC();
		}
		for (int i = 0; i < rules.length; i++) {
			sc_controls[rules[i]] = false;
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				stopMrs(rules[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				closeCamera(rules[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		}
		
	}
	
	public void removeView() {
		try {
			mWindowManager.removeView(view);
			view = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * �����I����
	 */
	private SessionLinearLayout.DispatchKeyEventListener mDispatchKeyEventListener = new SessionLinearLayout.DispatchKeyEventListener() {

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && isWindowViewShow) {
				setWindowMin();
				return true;
			}
			return false;
		}
	};

	public Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what==1001){
				boolean lock = false;
				Toast.makeText(c, "ִ�����ճɹ�", 1000).show();
				/*for (int i = 0; i < rules.length; i++) {
					if(rules[i]==picid&&rules.length>i+1){
						//boolean re = CameraUtil.cameraTakePicture(i+1, 1);
						boolean re = MediaCodecManager.TakePicture(i+1, 1);
						if(re){
							lock = true;
							break;
						}
					}
				}*/
				if(!lock){
					clickLock = false;
				}
			}
			if(msg.what==1003){
				boolean lock = false;
				Toast.makeText(c, "ִ������ʧ��", 1000).show();				
				if(!lock){
					clickLock = false;
				}
			}
			//¼�ƴﵽ�涨ʱ������¼
			if(msg.what==1002){
				clickLock = true;
				try {
					for (int i = 0; i < rules.length; i++) {
						stoprecorder(rules[i],i);
					}
					//����SD���ռ䴦���߼�
					//				SdCardBiz.getInstance().getDetection(Constants.isCleaning);
					//SdCardBiz.getInstance().getDetectionServiceSdCar(Constants.isCleaning,instance);
					for (int i = 0; i < rules.length; i++) {
						if(camera[rules[i]]!=null) startRecorder(rules[i]);
					}
					disk.getDetectionServiceSdCar(instance);

				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				recoTime = new Date().getTime();
				clickLock = false;
			}
			else if(msg.what==1022){
				postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						isClose = false;
						setWindowMin();
						Intent intent_set = new Intent(c, SetActivity.class);
						intent_set.putExtra("fromUpdateVersion", true);
						intent_set.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent_set);
					}
				}, 8000);
			}
		};
	};

	private void initView() {
		lys = new LinearLayout[6];
		for (int i = 0; i < lys.length; i++) {
			lys[i] = (LinearLayout) view.findViewById(lyids[i]);
			if(i!=0&&i!=3){
				//				lys[i].setOnClickListener(click_ly);
				lys[i].setOnClickListener(click2start);
			}
		}		
		
		//ȷ����·����·
		if(ServerManager.getInstance().getMode() == SetActivity.rgids[0]){
			lys[3].setVisibility(View.GONE);
			isTwoCamera = true;
		}else{
			lys[3].setVisibility(View.VISIBLE);
			isTwoCamera = false;
		}
		String rulestr = ServerManager.getInstance().getRule();
		rules = new int[rulestr.length()];
		for (int i = 0; i < rulestr.length(); i++) {
			rules[i] = Integer.parseInt(rulestr.substring(i, i+1));
		}
		for(int i =0; i<Constants.MAX_NUM_OF_CAMERAS; i++){	
			if(isTwoCamera&&i>1) break;
		}		
		ttvs = new TextureView[Constants.MAX_NUM_OF_CAMERAS];
		stHolder = new SurfaceTexture[Constants.MAX_NUM_OF_CAMERAS];		
		preview = new PreviewCallback[Constants.MAX_NUM_OF_CAMERAS];	
		stListener = new SurfaceTextureListener[Constants.MAX_NUM_OF_CAMERAS];
		if(isTabletDevice){
			ly_bts = (LinearLayout) view.findViewById(R.id.main_right_btly);
		}else{
			ly_bts = (LinearLayout) view.findViewById(R.id.main_bottom_btly);
		}
		ly_bts.setVisibility(View.VISIBLE);
		if(isTabletDevice){
			btiv1 = (ImageView) view.findViewById(R.id.imageView1);
			btiv2 = (ImageView) view.findViewById(R.id.imageView2);
		}else{
			btiv1 = (ImageView) view.findViewById(R.id.imageView1_bottom);
			btiv2 = (ImageView) view.findViewById(R.id.imageView2_bottom);
		}

		//Ԥ���ص�
		preview[0] = new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera1) {
				// TODO Auto-generated method stub
				MediaCodecManager.getInstance().onPreviewFrameUpload(data,0,camera[0]);
			}
		};
		preview[1] = new PreviewCallback() {

			@Override
			public void onPreviewFrame(byte[] data, Camera camera1) {
				// TODO Auto-generated method stub
				MediaCodecManager.getInstance().onPreviewFrameUpload(data,1,camera[1]);
			}
		};
		preview[2] = new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera1) {
				// TODO Auto-generated method stub
				MediaCodecManager.getInstance().onPreviewFrameUpload(data,2,camera[2]);
			}
		};
		preview[3] = new PreviewCallback() {

			@Override
			public void onPreviewFrame(byte[] data, Camera camera1) {
				// TODO Auto-generated method stub
				MediaCodecManager.getInstance().onPreviewFrameUpload(data,3,camera[3]);
			}
		};
		//��ʼ������ͷ����ʼԤ��
		for (int i = 0; i < Constants.MAX_NUM_OF_CAMERAS; i++) {
			if(isTwoCamera&&i>1) break;
			initPreview(i);
		}
	}
	
	public void SetPreviewValid(int index)
	{
		avaliable[index]  = true;		
	}
	/**
	 * ��ʼ��Ԥ��
	 * @param i
	 */	

	public void initPreview(int i){

		final int index = i;
		ttvs[i] = (TextureView) view.findViewById(ttvids[i]);
		stListener[i] = new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
			}
			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1, int arg2) {
			}
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
				colseCamera(index);
				return true;
			}
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,int arg2) {
				stHolder[index] = arg0;	
				openCamera(index, 1);
									
			}
		};
		ttvs[i].setSurfaceTextureListener(stListener[i]);	
	}
	/**
	 * �ر��ͷ�����ͷ
	 * @param i
	 */
	public void colseCamera(int index){
		try {		
		
			if(camera[index]!=null){
				camera[index].stopPreview();
				camera[index].release();
				camera[index] = null;
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void stopRecoders_SD_ERR()
	{		
		if(isRecording){
			btiv1.setImageResource(R.drawable.a02);
			for (int i = 0; i < rules.length; i++) {
				stoprecorder(rules[i],i);
			}
			isRecording = false;
			sd_inject = true;
		}	
	}
	

	public void startRecoders_SD_ERR()
	{
		if(!isRecording && sd_inject){
			btiv1.setImageResource(R.drawable.a02);
			for (int i = 0; i < rules.length; i++) {
				prepareRecorder(i,1);
			}
			isRecording = true;
			sd_inject = false;
		}	
	}	

	/**
	 * ������ͷ��Ԥ��
	 * @param i
	 * @param type 1 ��������  2 ����
	 */
	//int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
		
	public void openCamera(int index,int type){
		try {
			boolean falg = true;			
			if(falg){
				try {
					AppLog.w(TAG, "����ͷ����:"+Camera.getNumberOfCameras());
					camera[index] = Camera.open(cid[index]);
				} catch (Exception e) {
					e.printStackTrace();
					camera[index] = null;
				}	
				avaliable[index] = false;
				if (camera[index] != null) {
					try {
						camera[index].setPreviewTexture(stHolder[index]);
					} catch (Exception e) {
						e.printStackTrace();
					}
					Camera.Parameters parameters = camera[index].getParameters();
					parameters.setPreviewSize(Constants.RECORD_VIDEO_WIDTH, Constants.RECORD_VIDEO_HEIGHT);							
					camera[index].setErrorCallback(new CameraErrorCallback(index));					
					camera[index].setParameters(parameters);					
					camera[index].startPreview();													
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			AppLog.d(TAG, ExceptionUtil.getInfo(e));
		}
	}
	//��ֹ����һ������ͷ���������Ӱ��������ͷ������������
	//2017-06-29
	public boolean  checkCameraValid(final int index)
	{
		if(index > Constants.MAX_NUM_OF_CAMERAS)
			return false;		
		if(camera[index]== null)
		{
			return false;
		}
		camera[index].setPreviewCallback(preview[index]);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d("CMD", " checkCameraValid "+avaliable[index]);	
				if(avaliable[index] == false)				
				{
					//closeCamera(index);	
					
				}else
				{
					camera[index].setPreviewCallback(null);
					Intent intent = new Intent(MainService.ACTION);
					intent.putExtra("type", MainService.STARTRECORDER);
					intent.putExtra("index", index);
					sendBroadcast(intent);
					
				}				
			}
		}).start();
		return true;		
	}
	
	
	 public static void TakePictureAll(int type)
	 {
		 	
		 final int pictype = type;
		 Log.d("CMD", String.format(" TakePictureAll:%d", type));		 
		 try {		
				if(type == 1){
					//if(!SdCardUtil.checkSdCardUtil()){
					if(MainService.getDiskManager().getDiskCnt()<=0){
						AppLog.d("CMD", "SD��������");
						return ;
					}
				}else
				{
					File f = new File(Constants.SNAP_FILE_PATH);
					if(!f.exists()){
						f.mkdirs();
					}
				}
			
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				return ;
			}
		 	MediaCodecManager.CAMERA_OPER_MODE = type;
		 
		 new Thread(new Runnable() {
				@Override
				public void run() {
					boolean flag1 = false;
					for (int i = 0; i < rules.length; i++) {					
						if((camera[i]!= null) ){							
							picid = i;
							MediaCodecManager.Startpick(pictype);	
							camera[i].setPreviewCallback(preview[i]);							
							flag1 = true;							
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} //�ȴ��������//						
						}
			
					}					
					if( pictype == 1 )
				 	{
						if(flag1 ==  true )
						{
							Handler handler = MainService.getInstance().handler; 
							if(handler != null){
								handler.sendMessage(handler.obtainMessage(1001));
							}	
						}else
						{
							Handler handler = MainService.getInstance().handler; 
							if(handler != null){
								handler.sendMessage(handler.obtainMessage(1003));
							}	
						}						
				 	}						
				}
			}).start(); 		 	
		 
	 }
	
	//����ĳ·����
	boolean isgone = false;
	OnClickListener click_ly = new OnClickListener() {
		@Override
		public void onClick(View v) {
			boolean flag = false;
			if(isgone){
				isgone = false;
				setAllView();
				if(v.getId() == R.id.ly_1_1){
					flag = true;
				}
			}else{
				isgone = true;
				switch (v.getId()) {
				case R.id.ly_1_1:
					lys[2].setVisibility(View.GONE);
					if(!isTwoCamera){
						lys[3].setVisibility(View.GONE);
					}
					break;
				case R.id.ly_1_2:
					if(!isTwoCamera){
						lys[3].setVisibility(View.GONE);
					}
					lys[1].setVisibility(View.GONE);
					break;
				case R.id.ly_2_1:
					lys[0].setVisibility(View.GONE);
					lys[5].setVisibility(View.GONE);
					break;
				case R.id.ly_2_2:
					lys[0].setVisibility(View.GONE);
					lys[4].setVisibility(View.GONE);
					break;
				}
			}
			
		}
	};
	//����ĳ·����
	void setAllView(){
		if(isTwoCamera){
			lys[1].setVisibility(View.VISIBLE);
			lys[2].setVisibility(View.VISIBLE);
		}else{
			for (int i = 0; i < lys.length; i++) {
				lys[i].setVisibility(View.VISIBLE);
			}
		}
	}
	//�ͷ�����ͷ��Դ
	public void closeCamera(int index){
		if(camera[index]!=null){
			camera[index].setPreviewCallback(null);
			camera[index].stopPreview();
			camera[index].release();
			camera[index] = null;
		}
	}
	//�ͷ�¼����Դ
	public void stopMrs(int index){
		if (mrs[index]!=null) { 
			mrs[index].stop(); 
			mrs[index].release(); 
			mrs[index] = null; 
		}
	}

	//�ұ����������ĵ���¼�
	public static int picid = -1;
	boolean isRecording = false;
	boolean isSC = false;
	public void click(View v){
		click(v.getId());
	}
	
	
	public void click(int id){
		if(clickLock) return;
		switch (id) {
		case R.id.bt_ly_1://����		
		case R.id.bt_ly_1_bottom://拍照			
	
			//���SD���Ƿ����
			//if(!SdCardUtil.checkSdCardUtil()){			
			if(disk.getDiskCnt()<=0){
				Toast.makeText(c, "δ��⵽SD��,���޷�ִ�в���", 1000).show();
			}else{
				clickLock = true;
				//CameraUtil.cameraTakePicture(0, 1);
				TakePictureAll(1);				
			}			
			break;
		case R.id.bt_ly_2://¼��
		case R.id.bt_ly_2_bottom://录像

			//���SD���Ƿ����
			//if(!SdCardUtil.checkSdCardUtil()){
			
			if(disk.getDiskCnt()<=0){
				Toast.makeText(c, "δ��⵽SD��,���޷�ִ�в���", 1000).show();
			}else{
				clickLock = true;
				//���ж��Ƿ�¼����
				if(isRecording){
					btiv1.setImageResource(R.drawable.a02);
					//�����ܿ�����,ֹͣ¼��
					for (int i = 0; i < rules.length; i++) {
						stoprecorder(rules[i],i);
					}
					isRecording = false;
				}else{
					//�ж��Ƿ������ϴ�
					/*if(isSC){
						//ֹͣ�ϴ�
						stopSC();
					}*/
					btiv1.setImageResource(R.drawable.b02);
					disk.getDetectionServiceSdCar(instance);	
					//�����ܿ�����,��ʼ¼��
					for (int i = 0; i < rules.length; i++) {
						if(camera[rules[i]]!=null  ) startRecorder(rules[i]);
					}
					recoTime = new Date().getTime();
					isRecording = true;
				}
				clickLock = false;
			}
			break;
		case R.id.bt_ly_3://�ϴ�
		case R.id.bt_ly_3_bottom://上传

			clickLock = true;
			if(isSC){
				stopSC();
			}else{				
				//�����ϴ�
				btiv2.setImageResource(R.drawable.b03);
				for (int i = 0; i < rules.length; i++) {
					startVideoUpload2(ServerManager.getInstance().getIp(),ServerManager.getInstance().getPort(),ServerManager.getInstance().getStreamname(),i);
				}
				isSC = true;
			}
			clickLock = false;
			break;
		case R.id.bt_ly_4://�ط�
		case R.id.bt_ly_4_bottom://回放

			isClose = false;
			setWindowMin();
			Intent intent_file = new Intent(c, FileActivity.class);
			intent_file.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent_file);
			break;
		case R.id.bt_ly_5://����
		case R.id.bt_ly_5_bottom://设置

			isClose = false;
			setWindowMin();
			Intent intent_set = new Intent(c, SetActivity.class);
			intent_set.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent_set);
			break;
		case R.id.bt_ly_6://�˳�
		case R.id.bt_ly_6_bottom://退�?	
			setWindowMin();
			break;
		}
	}

	//�����ϴ�
	private void stopSC() {
		btiv2.setImageResource(R.drawable.a03);
		for (int i = 0; i < rules.length; i++) {
			stopVideoUpload(i);
			try {
				Thread.sleep(500);
			} catch (Exception e) {
			}
		}
		isSC = false;

	}
	

	public   void setCallback(int index, Camera camera)
	{
		camera.setPreviewCallback(preview[index]);	
	}	

	public void startVideoUpload2(String ipstr, String portstr, String serialno,  int index){

		int CameraId;
		CameraId = index+1;		
		if(camera[rules[index]]!=null && sc_controls[rules[index]]!=false){
			return;			
		}		
		try {
			CameraUtil.VIDEO_UPLOAD[index] = true;
			if(camera[rules[index]]!=null){
				//��ʼ����������
				StreamIndex[rules[index]]= mPusher.CarEyeInitNetWork( getApplicationContext(),ipstr, portstr, String.format("%s?channel=%d.sdp", serialno,CameraId), Constants.CAREYE_VCODE_H264,20,Constants.CAREYE_ACODE_AAC,1,8000);
				//����Ԥ���ص�
				sc_controls[rules[index]] = true;
				camera[rules[index]].setPreviewCallback(preview[rules[index]]);	
				MediaCodecManager.getInstance().StartUpload(rules[index],camera[rules[index]]);									
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/**
	 * ������Ƶ�ϴ�
	 * @param i
	 */
	public void stopVideoUpload(int i){
		try {
			Log.d("SERVICE", " stop upload"+i);
			CameraUtil.VIDEO_UPLOAD[i] = false;
			if(camera[rules[i]]!=null){				
				sc_controls[rules[i]] = false;				
				MediaCodecManager.getInstance().StopUpload(rules[i]);
				camera[rules[i]].setPreviewCallback(null);
				mPusher.stopPush(StreamIndex[rules[i]]);	

			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/**
	 * ׼��¼��
	 * @param index
	 */
	public void prepareRecorder(int index,int type){
		try {
			//if(!SdCardUtil.checkSdCardUtil()){
			if(disk.getDiskCnt()<=0){
				Log.d("CMD", " sd card not mount"+index);	
			}else{
				btiv1.setImageResource(R.drawable.b02);
				if(type == 1){
					recoTime = new Date().getTime();
				}
				isRecording = true;				
				startRecorder(rules[index]);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	/**
	 * ��ʼ¼��
	 * @param index
	 */
	private  String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }
	
	public static String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }
	
	public static void addVideo(final String path,final ContentValues values)
	{
		
		 AsyncTask.execute(new Runnable() {
             @Override
             public void run() {
            	 try
     			{	
     							
     				String finalName  = values.getAsString(Video.Media.DATA);
     				new File(path).renameTo(new File(finalName));
     				
     			}
     			catch(Exception e)
     			{
     				
     			}
             }
         });
		
		
	}
	
	private void generateVideoFilename(int index,  int outputFileFormat) {  	 
        
        String title = String.format("%d-%d", index+1, new Date().getTime()) ;
        String filename = title + convertOutputFormatToFileExt(outputFileFormat);
        String path = disk.getDiskDirectory(disk.SelectDisk())+Constants.CAMERA_FILE_DIR + filename;        
        String tmpPath = path + ".tmp";        
        String mime = convertOutputFormatToMimeType(outputFileFormat);  
        mCurrentVideoValues[index] = new ContentValues(4);
        mCurrentVideoValues[index].put(Video.Media.TITLE, title);
        mCurrentVideoValues[index].put(Video.Media.DISPLAY_NAME, filename);
        mCurrentVideoValues[index].put(Video.Media.MIME_TYPE, mime);
        mCurrentVideoValues[index].put(Video.Media.DATA, path);
        MrTempName[index] = tmpPath;
    }

	public void startRecorder(int index){
		try { 
			camera[index].unlock();
			mrs[index] = new MediaRecorder(); 
			mrs[index].reset();
			mrs[index].setCamera(camera[index]);
			mrs[index].setVideoSource(MediaRecorder.VideoSource.CAMERA);
			String starttime;
			String endtime;				
			/*//����audio�ı����ʽ
			mrs[index].setAudioSource(MediaRecorder.AudioSource.MIC);
			mrs[index].setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);*/
			//1 T3 2 һ�ױ�����Ӿ�  3 �з����Ӿ�
			Log.d("CMD", " startRecorder "+index);			
			mrs[index].setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); 
			mrs[index].setVideoEncoder(MediaRecorder.VideoEncoder.H264); 
			if(cid[index]>3){
				mrs[index].setVideoSize(720, 576); 
				mrs[index].setVideoEncodingBitRate(4*720*576);
			}else if(cid[index]>-1&&cid[index]<4){
				
				mrs[index].setVideoSize(Constants.RECORD_VIDEO_WIDTH, Constants.RECORD_VIDEO_HEIGHT); 
				mrs[index].setVideoEncodingBitRate(3*Constants.RECORD_VIDEO_WIDTH*Constants.RECORD_VIDEO_HEIGHT/2);
			}
			mrs[index].setVideoFrameRate(framerate); 
			mrs[index].setOnErrorListener(new MediaRecorderErrorListener(index));
			//camera[index].startWaterMark();					
			generateVideoFilename(index, MediaRecorder.OutputFormat.MPEG_4 );
			mrs[index].setOutputFile( MrTempName[index]);			
			Log.d("CMD", "generate filename"+MrTempName[index]);	
			mrs[index].prepare(); 
			mrs[index].start(); 
			Constants.CAMERA_RECORD[index] = true;
		} catch (Exception e) { 
			e.printStackTrace(); 
		}

	}
	//��������ͷidֹͣ¼��
	void stoprecorder(int index,int i){
		try {
			if(camera[rules[i]]!=null){
				recoTime = -1;
				if (mrs[index] != null) { 
					try {
						mrs[index].setOnErrorListener(null);  
						mrs[index].setOnInfoListener(null);    
						mrs[index].setPreviewDisplay(null);  
						mrs[index].stop(); 
					} catch (Exception e) {
						e.printStackTrace();
					}
					Log.d("CMD", String.format(" stop record:"));
					mrs[index].release(); 
					mrs[index] = null; 
					camera[index].lock();
					addVideo(MrTempName[index], mCurrentVideoValues[index]);
					
				} 
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	public class CameraErrorCallback implements android.hardware.Camera.ErrorCallback {
		private int mCameraId = -1;
		private Object switchLock = new Object();
		public CameraErrorCallback(int cameraId) {
			mCameraId = cameraId;
		}
		@Override
		public void onError(int error, android.hardware.Camera camera) {
			if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {        //�ײ�cameraʵ���ҵ���
				// We are not sure about the current state of the app (in preview or snapshot or recording). Closing the app is better than creating a new Camera object.                                 
				//�����mipi�ҵ��ˣ�usb�ϵ磬Ȼ��ɱ���Լ����ڵĽ��̣����������㲥�����Լ�
				//usb camera�ҵ��ˣ��ȶϵ�Ȼ�����ϵ�
				//Toast.makeText(c, "����ͷ��error="+error+",mCameraId="+mCameraId, Toast.LENGTH_LONG).show();
			}
			Log.d("	error!!!", "code!!!!:"+error);	
		}
	}

	private class MediaRecorderErrorListener implements MediaRecorder.OnErrorListener {                 //�ײ�mediaRecorder�ϱ�������Ϣ
		private int mCameraId = -1;
		public MediaRecorderErrorListener(int cameraId) {
			mCameraId = cameraId;
		}    
		@Override
		public void onError(MediaRecorder mr, int what, int extra) {                              
			//��ֹͣ��¼��
			if(what == MediaRecorder.MEDIA_ERROR_SERVER_DIED){      //MediaRecorder.MEDIA_ERROR_SERVER_DIED--100��˵��mediaService���ˣ���Ҫ�ͷ�MediaRecorder

				btiv1.setImageResource(R.drawable.a02);
				//�����ܿ����飬ֹͣ¼��
				for (int i = 0; i < rules.length; i++) {
					stoprecorder(rules[i],i);
					openCamera(i,1);
				}
				isRecording = false;
			}

		}
	}
	
	private boolean isTabletDevice(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >=
				Configuration.SCREENLAYOUT_SIZE_LARGE;
	}	
	
}
