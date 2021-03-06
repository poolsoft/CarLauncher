package com.tchip.carlauncher.ui.activity;

import java.io.File;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.MyApplication;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.lib.filemanager.FolderActivity;
import com.tchip.carlauncher.model.DriveVideo;
import com.tchip.carlauncher.model.DriveVideoDbHelper;
import com.tchip.carlauncher.model.Typefaces;
import com.tchip.carlauncher.service.BrightAdjustService;
import com.tchip.carlauncher.service.ConnectWifiService;
import com.tchip.carlauncher.service.RouteRecordService;
import com.tchip.carlauncher.service.SensorWatchService;
import com.tchip.carlauncher.service.SpeakService;
import com.tchip.carlauncher.service.WeatherService;
import com.tchip.carlauncher.util.AudioPlayUtil;
import com.tchip.carlauncher.util.ClickUtil;
import com.tchip.carlauncher.util.DateUtil;
import com.tchip.carlauncher.util.NetworkUtil;
import com.tchip.carlauncher.util.SettingUtil;
import com.tchip.carlauncher.util.StorageUtil;
import com.tchip.carlauncher.util.WeatherUtil;
import com.tchip.carlauncher.util.SignalUtil;
import com.tchip.carlauncher.view.AudioRecordDialog;
import com.tchip.speech.SpeechService;
import com.tchip.speech.WakeUpCloudAsr;
import com.tchip.tachograph.TachographCallback;
import com.tchip.tachograph.TachographRecorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

public class MainActivity extends Activity implements TachographCallback,
		Callback {

	private SharedPreferences sharedPreferences;
	private Editor editor;

	private DriveVideoDbHelper videoDb;

	private LocationClient mLocationClient;

	private SurfaceView surfaceCamera;
	private boolean isSurfaceLarge = false;
	private MapView mainMapView;
	private BaiduMap baiduMap;
	private com.baidu.mapapi.map.MyLocationConfiguration.LocationMode currentMode;
	boolean isFirstLoc = true;// 是否首次定位

	private int scanSpan = 1000; // 轨迹点采集间隔(ms)

	private ImageView smallVideoRecord, smallVideoLock, smallVideoCamera;
	private RelativeLayout layoutLargeButton;
	private TextView textTemp, textLocation, textTodayWeather, textRecordTime;
	private ImageView imageTodayWeather;

	private ImageView imageWifiLevel; // WiFi状态图标
	private IntentFilter wifiIntentFilter; // WiFi状态监听器

	private ImageView imageShadowRight, imageShadowLeft;

	private HorizontalScrollView hsvMain;

	// Record
	private ImageView largeVideoSize, largeVideoTime, largeVideoLock,
			largeVideoMute, largeVideoRecord, largeVideoCamera;
	private SurfaceHolder mHolder;
	private TachographRecorder mMyRecorder;
	private Camera mCamera;

	private ImageView imageDefault; // 未联网时导航的背景图

	private int mResolutionState, mRecordState, mIntervalState, mPathState,
			mSecondaryState, mOverlapState, mMuteState;

	private LinearLayout layoutVideoSize, layoutVideoTime, layoutVideoLock,
			layoutVideoMute, layoutVideoRecord, layoutVideoCamera,
			layoutVideoRecordSmall, layoutVideoCameraSmall,
			layoutVideoLockSmall;

	private ImageView imageSignalLevel, image3G;

	private TelephonyManager Tel;
	private int simState;
	private MyPhoneStateListener MyListener;

	private AudioRecordDialog audioRecordDialog;

	private String strNotLocate;

	private WifiManager wifiManager;
	private ConnectivityManager connManager;
	private NetworkInfo mWifi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.activity_main);

		sharedPreferences = getSharedPreferences(
				Constant.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		editor = sharedPreferences.edit();

		videoDb = new DriveVideoDbHelper(getApplicationContext());

		// Dialog
		audioRecordDialog = new AudioRecordDialog(MainActivity.this);

		strNotLocate = getResources().getString(R.string.not_locate);
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		initialLayout();
		initialCameraButton();
		initialService();

		// 录像
		setupRecordDefaults();
		setupRecordViews();

		// 开机自动录像
		if (Constant.Record.autoRecord) {
			new Thread(new AutoRecordThread()).start();
		}

		// 开机尝试连接WiFi
		if (!Constant.Module.isWifiSystem) {
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (wifiManager.isWifiEnabled()
					&& (!mWifi.isConnectedOrConnecting())) {
				Intent intentWiFi = new Intent(getApplicationContext(),
						ConnectWifiService.class);
				startService(intentWiFi);
				// Log.v(Constant.TAG, "Start Connect Wifi...");
			} else {
				// Log.v(Constant.TAG, "Wifi is Connected or disable");
			}
		}

		// 3G信号
		MyListener = new MyPhoneStateListener();
		Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		// SIM卡状态
		simState = Tel.getSimState();
		Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		// 初始化fm发射
		// initFmTransmit();

		// 启动思必驰语音服务
		// if(!Constant.Module.isVoiceXunfei){
		Intent intent = new Intent(this, SpeechService.class);
		startService(intent);
		// }
	}

	public class AutoRecordThread implements Runnable {

		@Override
		public void run() {
			try {
				Thread.sleep(Constant.Record.autoRecordDelay);
				Message message = new Message();
				message.what = 1;
				autoRecordHandler.sendMessage(message);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	final Handler autoRecordHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				startOrStopRecord();
				break;

			default:
				break;
			}
		}
	};

	private class MyPhoneStateListener extends PhoneStateListener {

		/**
		 * 更新3G信号强度
		 */
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			update3GState(signalStrength.getGsmSignalStrength());
		}

		@Override
		public void onDataConnectionStateChanged(int state) {

			switch (state) {
			case TelephonyManager.DATA_DISCONNECTED:// 网络断开
				Log.v(Constant.TAG, "3G TelephonyManager.DATA_DISCONNECTED");
				image3G.setVisibility(View.GONE);
				break;

			case TelephonyManager.DATA_CONNECTING:// 网络正在连接
				Log.v(Constant.TAG, "3G TelephonyManager.DATA_CONNECTING");
				image3G.setVisibility(View.VISIBLE);
				break;

			case TelephonyManager.DATA_CONNECTED:// 网络连接上
				Log.v(Constant.TAG, "3G TelephonyManager.DATA_CONNECTED");
				image3G.setVisibility(View.VISIBLE);
				break;
			}
		}
	}

	/**
	 * 更新3G状态
	 * 
	 * SIM_STATE_UNKNOWN = 0:Unknown.
	 * 
	 * SIM_STATE_ABSENT = 1:no SIM card is available in the device
	 * 
	 * SIM_STATE_PIN_REQUIRED = 2:requires the user's SIM PIN to unlock
	 * 
	 * SIM_STATE_PUK_REQUIRED = 3:requires the user's SIM PUK to unlock
	 * 
	 * SIM_STATE_NETWORK_LOCKED = 4:requires a network PIN to unlock
	 * 
	 * SIM_STATE_READY = 5:Ready
	 * 
	 */
	private void update3GState(int signal) {
		// imageSignalLevel,image3G.setVisibility(View.GONE);
		simState = Tel.getSimState();
		Log.v(Constant.TAG, "SIM State:" + simState);
		if (simState == TelephonyManager.SIM_STATE_READY) {

			imageSignalLevel.setBackground(getResources().getDrawable(
					SignalUtil.get3GImageBySignal(signal)));
			if (signal > 0 && signal < 31)
				image3G.setVisibility(View.VISIBLE);
			else
				image3G.setVisibility(View.GONE);
		} else if (simState == TelephonyManager.SIM_STATE_UNKNOWN
				|| simState == TelephonyManager.SIM_STATE_ABSENT) {
			image3G.setVisibility(View.GONE);
			imageSignalLevel.setBackground(getResources().getDrawable(
					R.drawable.ic_qs_signal_no_signal));
		}
	}

	/**
	 * 初始化服务
	 */
	private void initialService() {
		// 亮度自动调整服务
		if (Constant.Module.hasBrightAdjust) {
			Intent intentBrightness = new Intent(this,
					BrightAdjustService.class);
			startService(intentBrightness);
		}

		// 轨迹记录服务
		Intent intentRoute = new Intent(this, RouteRecordService.class);
		startService(intentRoute);

		// 碰撞侦测服务
		Intent intentSensor = new Intent(this, SensorWatchService.class);
		startService(intentSensor);

		// importOfflineMapFromSDCard();
	}

	/**
	 * 初始化布局
	 */
	private void initialLayout() {
		// 未联网时导航背景
		imageDefault = (ImageView) findViewById(R.id.imageDefault);
		if (NetworkUtil.isNetworkConnected(getApplicationContext())) {
			imageDefault.setVisibility(View.GONE);
		}

		// 录像窗口
		if (Constant.Record.hasCamera) {
			surfaceCamera = (SurfaceView) findViewById(R.id.surfaceCamera);
			surfaceCamera.setOnClickListener(new MyOnClickListener());
			surfaceCamera.getHolder().addCallback(this);
		}

		textRecordTime = (TextView) findViewById(R.id.textRecordTime);

		// 增大点击区域
		layoutVideoSize = (LinearLayout) findViewById(R.id.layoutVideoSize);
		layoutVideoSize.setOnClickListener(new MyOnClickListener());

		layoutVideoTime = (LinearLayout) findViewById(R.id.layoutVideoTime);
		layoutVideoTime.setOnClickListener(new MyOnClickListener());

		layoutVideoLock = (LinearLayout) findViewById(R.id.layoutVideoLock);
		layoutVideoLock.setOnClickListener(new MyOnClickListener());

		layoutVideoMute = (LinearLayout) findViewById(R.id.layoutVideoMute);
		layoutVideoMute.setOnClickListener(new MyOnClickListener());

		layoutVideoRecord = (LinearLayout) findViewById(R.id.layoutVideoRecord);
		layoutVideoRecord.setOnClickListener(new MyOnClickListener());

		layoutVideoCamera = (LinearLayout) findViewById(R.id.layoutVideoCamera);
		layoutVideoCamera.setOnClickListener(new MyOnClickListener());

		layoutVideoRecordSmall = (LinearLayout) findViewById(R.id.layoutVideoRecordSmall);
		layoutVideoRecordSmall.setOnClickListener(new MyOnClickListener());

		layoutVideoCameraSmall = (LinearLayout) findViewById(R.id.layoutVideoCameraSmall);
		layoutVideoCameraSmall.setOnClickListener(new MyOnClickListener());

		layoutVideoLockSmall = (LinearLayout) findViewById(R.id.layoutVideoLockSmall);
		layoutVideoLockSmall.setOnClickListener(new MyOnClickListener());

		// 天气预报和时钟,状态图标
		RelativeLayout layoutWeather = (RelativeLayout) findViewById(R.id.layoutWeather);
		layoutWeather.setOnClickListener(new MyOnClickListener());
		TextClock textClock = (TextClock) findViewById(R.id.textClock);
		textClock.setTypeface(Typefaces.get(this, Constant.Path.FONT
				+ "Font-Helvetica-Neue-LT-Pro.otf"));

		TextClock textDate = (TextClock) findViewById(R.id.textDate);
		textDate.setTypeface(Typefaces.get(this, Constant.Path.FONT
				+ "Font-Helvetica-Neue-LT-Pro.otf"));

		TextClock textWeek = (TextClock) findViewById(R.id.textWeek);
		textWeek.setTypeface(Typefaces.get(this, Constant.Path.FONT
				+ "Font-Helvetica-Neue-LT-Pro.otf"));

		textTemp = (TextView) findViewById(R.id.textTemp);
		textTemp.setTypeface(Typefaces.get(this, Constant.Path.FONT
				+ "Font-Helvetica-Neue-LT-Pro.otf"));

		imageTodayWeather = (ImageView) findViewById(R.id.imageTodayWeather);
		textTodayWeather = (TextView) findViewById(R.id.textTodayWeather);
		textLocation = (TextView) findViewById(R.id.textLocation);

		LinearLayout layoutWiFi = (LinearLayout) findViewById(R.id.layoutWiFi);
		layoutWiFi.setOnClickListener(new MyOnClickListener());

		// WiFi状态信息
		imageWifiLevel = (ImageView) findViewById(R.id.imageWifiLevel);

		wifiIntentFilter = new IntentFilter();
		wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		wifiIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		updateWiFiState();

		// 3G状态信息
		imageSignalLevel = (ImageView) findViewById(R.id.imageSignalLevel);
		image3G = (ImageView) findViewById(R.id.image3G);
		image3G.setVisibility(View.GONE);

		// 更新天气与位置信息
		updateLocationAndWeather();
		// updateProgress.setVisibility(View.VISIBLE);

		// 定位地图
		mainMapView = (MapView) findViewById(R.id.mainMapView);
		// 去掉缩放控件和百度Logo
		int count = mainMapView.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = mainMapView.getChildAt(i);
			if (child instanceof ImageView || child instanceof ZoomControls) {
				child.setVisibility(View.INVISIBLE);
			}
		}
		baiduMap = mainMapView.getMap();
		// 开启定位图层
		baiduMap.setMyLocationEnabled(true);

		// 自定义Marker
		// BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory
		// .fromResource(R.drawable.icon_arrow_up);

		// 设置地图放大级别 0-19
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15);
		baiduMap.animateMapStatus(msu);

		View mapHideView = findViewById(R.id.mapHideView);
		mapHideView.setOnClickListener(new MyOnClickListener());

		ImageView imageNavi = (ImageView) findViewById(R.id.imageNavi);
		imageNavi.setOnClickListener(new MyOnClickListener());

		// 在线音乐
		ImageView imageMusicOL = (ImageView) findViewById(R.id.imageMusicOL);
		imageMusicOL.setOnClickListener(new MyOnClickListener());

		// 多媒体
		ImageView imageMultimedia = (ImageView) findViewById(R.id.imageMultimedia);
		imageMultimedia.setOnClickListener(new MyOnClickListener());

		// 文件管理
		ImageView imageFileExplore = (ImageView) findViewById(R.id.imageFileExplore);
		imageFileExplore.setOnClickListener(new MyOnClickListener());

		RelativeLayout layoutFileExplore = (RelativeLayout) findViewById(R.id.layoutFileExplore);
		if (Constant.Module.hasFileManager) {
			layoutFileExplore.setVisibility(View.VISIBLE);
		} else {
			layoutFileExplore.setVisibility(View.GONE);
		}

		// 周边搜索
		ImageView imageNearSearch = (ImageView) findViewById(R.id.imageNearSearch);
		imageNearSearch.setOnClickListener(new MyOnClickListener());

		// 语音助手
		ImageView imageVoiceChat = (ImageView) findViewById(R.id.imageVoiceChat);
		imageVoiceChat.setOnClickListener(new MyOnClickListener());

		// 行驶轨迹
		ImageView imageRouteTrack = (ImageView) findViewById(R.id.imageRouteTrack);
		imageRouteTrack.setOnClickListener(new MyOnClickListener());

		// FM发射
		ImageView imageFmTransmit = (ImageView) findViewById(R.id.imageFmTransmit);
		imageFmTransmit.setOnClickListener(new MyOnClickListener());

		// 路径规划（摄像头，红绿灯）
		ImageView imageRoutePlan = (ImageView) findViewById(R.id.imageRoutePlan);
		imageRoutePlan.setOnClickListener(new MyOnClickListener());

		// 拨号
		ImageView imageDialer = (ImageView) findViewById(R.id.imageDialer);
		imageDialer.setOnClickListener(new MyOnClickListener());

		// 短信
		ImageView imageMessage = (ImageView) findViewById(R.id.imageMessage);
		imageMessage.setOnClickListener(new MyOnClickListener());

		// 设置
		ImageView imageSetting = (ImageView) findViewById(R.id.imageSetting);
		imageSetting.setOnClickListener(new MyOnClickListener());

		// HorizontalScrollView，左右两侧阴影
		imageShadowLeft = (ImageView) findViewById(R.id.imageShadowLeft);
		imageShadowRight = (ImageView) findViewById(R.id.imageShadowRight);

		hsvMain = (HorizontalScrollView) findViewById(R.id.hsvMain);
		hsvMain.setDrawingCacheEnabled(true);
		if (Constant.Module.isHsvTouch) {
			hsvMain.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_MOVE:
						View childFirst = ((HorizontalScrollView) v)
								.getChildAt(0);

						// 右侧阴影
						if (v.getScrollX() + v.getWidth() + 20 >= childFirst
								.getMeasuredWidth()) {
							imageShadowRight.setVisibility(View.INVISIBLE);
						} else {
							imageShadowRight.setVisibility(View.VISIBLE);
						}
						// 左侧阴影
						if (v.getScrollX() >= 20) {
							imageShadowLeft.setVisibility(View.VISIBLE);
						} else {
							imageShadowLeft.setVisibility(View.INVISIBLE);
						}
						break;
					default:
						break;
					}
					return false;
				}
			});
		}
	}

	private void startSpeak(String content) {
		Intent intent = new Intent(MainActivity.this, SpeakService.class);
		intent.putExtra("content", content);
		startService(intent);
	}

	/**
	 * 初始化导航实例
	 */
	private String mSDCardPath = null;
	private static final String APP_FOLDER_NAME = "CarLauncher";

	private void initialNaviInstance() {
		if (initDirs()) {
		}
		initNavi();
	}

	private boolean initDirs() {
		mSDCardPath = getSdcardDir();
		if (mSDCardPath == null) {
			return false;
		}
		File f = new File(mSDCardPath, APP_FOLDER_NAME);
		if (!f.exists()) {
			try {
				f.mkdir();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	String authinfo = null;
	public static final String ROUTE_PLAN_NODE = "routePlanNode";

	private void initNavi() {
		BaiduNaviManager.getInstance().setNativeLibraryPath(
				mSDCardPath + "/BaiduNaviSDK_SO");
		BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME,
				new NaviInitListener() {
					@Override
					public void onAuthResult(int status, String msg) {
						if (0 == status) {
							authinfo = "Success!";
							MyApplication.isNaviAuthSuccess = true;
						} else {
							authinfo = "Fail:" + msg;
							MyApplication.isNaviAuthSuccess = false;
						}

						Log.v(Constant.TAG, "Baidu Navi:Key auth " + authinfo);
					}

					public void initSuccess() {
						// 导航初始化是异步的，需要一小段时间，以这个标志来识别引擎是否初始化成功，为true时候才能发起导航
						MyApplication.isNaviInitialSuccess = true;
						if (Constant.isDebug) {
							Log.v(Constant.TAG, "Baidu Navi:Initial Success!");
						}
					}

					public void initStart() {
						if (Constant.isDebug) {
							Log.v(Constant.TAG, "Baidu Navi:Initial Start!");
						}
					}

					public void initFailed() {
						MyApplication.isNaviInitialSuccess = false;
						if (Constant.isDebug) {
							Log.v(Constant.TAG, "Baidu Navi:Initial Fail!");
						}
					}
				}, /* null */mTTSCallback);
	}

	BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {

		@Override
		public void stopTTS() {

		}

		@Override
		public void resumeTTS() {

		}

		@Override
		public void releaseTTSPlayer() {

		}

		@Override
		public int playTTSText(String text, int arg1) {
			startSpeak(text);
			return 0;
		}

		@Override
		public void phoneHangUp() {

		}

		@Override
		public void phoneCalling() {

		}

		@Override
		public void pauseTTS() {

		}

		@Override
		public void initTTSPlayer() {

		}

		@Override
		public int getTTSState() {
			return 1;
		}
	};

	private String getSdcardDir() {
		if (Environment.getExternalStorageState().equalsIgnoreCase(
				Environment.MEDIA_MOUNTED)) {
			return Environment.getExternalStorageDirectory().toString();
		}
		return null;
	}

	/**
	 * 初始化录像按钮
	 */
	private void initialCameraButton() {
		// ********** 小视图 **********
		// 录制
		smallVideoRecord = (ImageView) findViewById(R.id.smallVideoRecord);
		smallVideoRecord.setOnClickListener(new MyOnClickListener());

		// 锁定
		smallVideoLock = (ImageView) findViewById(R.id.smallVideoLock);
		smallVideoLock.setOnClickListener(new MyOnClickListener());

		// 拍照
		smallVideoCamera = (ImageView) findViewById(R.id.smallVideoCamera);
		smallVideoCamera.setOnClickListener(new MyOnClickListener());

		// ********** 大视图 **********
		layoutLargeButton = (RelativeLayout) findViewById(R.id.layoutLargeButton);

		// 视频尺寸
		largeVideoSize = (ImageView) findViewById(R.id.largeVideoSize);
		largeVideoSize.setOnClickListener(new MyOnClickListener());

		// 视频分段长度
		largeVideoTime = (ImageView) findViewById(R.id.largeVideoTime);
		largeVideoTime.setOnClickListener(new MyOnClickListener());

		// 锁定
		largeVideoLock = (ImageView) findViewById(R.id.largeVideoLock);
		largeVideoLock.setOnClickListener(new MyOnClickListener());

		// 静音
		largeVideoMute = (ImageView) findViewById(R.id.largeVideoMute);
		largeVideoMute.setOnClickListener(new MyOnClickListener());

		// 录制
		largeVideoRecord = (ImageView) findViewById(R.id.largeVideoRecord);
		largeVideoRecord.setOnClickListener(new MyOnClickListener());

		// 拍照
		largeVideoCamera = (ImageView) findViewById(R.id.largeVideoCamera);
		largeVideoCamera.setOnClickListener(new MyOnClickListener());

		updateButtonState(isSurfaceLarge());
	}

	/**
	 * 更新录像大小按钮显示状态
	 * 
	 * @param isSurfaceLarge
	 */
	private void updateButtonState(boolean isSurfaceLarge) {
		if (isSurfaceLarge) {
			smallVideoRecord.setVisibility(View.INVISIBLE);
			smallVideoLock.setVisibility(View.INVISIBLE);
			smallVideoCamera.setVisibility(View.INVISIBLE);
			layoutLargeButton.setVisibility(View.VISIBLE);
		} else {
			smallVideoRecord.setVisibility(View.VISIBLE);
			smallVideoLock.setVisibility(View.VISIBLE);
			smallVideoCamera.setVisibility(View.VISIBLE);
			layoutLargeButton.setVisibility(View.INVISIBLE);
		}
	}

	private boolean isSurfaceLarge() {
		return isSurfaceLarge;
	}

	/**
	 * 更新位置和天气
	 */
	private void updateLocationAndWeather() {

		if (strNotLocate.equals(sharedPreferences.getString("cityName",
				strNotLocate))) {
			String cityName = sharedPreferences.getString("cityNameRealButOld",
					strNotLocate);
			if (strNotLocate.equals(cityName)) {
				String addrStr = sharedPreferences.getString("addrStr",
						strNotLocate);
				if (addrStr.contains("省") && addrStr.contains("市")) {
					cityName = addrStr.split("省")[1].split("市")[0];
				} else if ((!addrStr.contains("省")) && addrStr.contains("市")) {
					cityName = addrStr.split("市")[0];
				} else {
					cityName = addrStr;
				}
			}
			editor.putString("cityNameRealButOld", cityName);
			editor.commit();
			textLocation.setText(cityName);
		} else {
			textLocation.setText(sharedPreferences.getString("cityName",
					strNotLocate));
		}

		String weatherToday = sharedPreferences.getString("day0weather",
				getResources().getString(R.string.unknown));
		textTodayWeather.setText(weatherToday);
		imageTodayWeather.setImageResource(WeatherUtil
				.getWeatherDrawable(WeatherUtil.getTypeByStr(weatherToday)));
		String day0tmpLow = sharedPreferences.getString("day0tmpLow", "15℃");
		String day0tmpHigh = sharedPreferences.getString("day0tmpHigh", "25℃");
		day0tmpLow = day0tmpLow.split("℃")[0];
		textTemp.setText(day0tmpLow + "~" + day0tmpHigh);
	}

	private int secondCount = -1;

	public class updateRecordTimeThread implements Runnable {

		@Override
		public void run() {
			// 解决录像时，快速点击录像按钮两次，线程叠加跑秒过快的问题
			synchronized (updateRecordTimeHandler) {
				do {
					if (MyApplication.isVideoCardEject
							|| (!MyApplication.isPowerConnect)) {
						// 录像时视频SD卡拔出,电源断开保存视频
						Log.e(Constant.TAG,
								"SD card remove badly or power unconnected, stop record!");
						Message messageEject = new Message();
						messageEject.what = 2;
						updateRecordTimeHandler.sendMessage(messageEject);
						break;
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Message messageSecond = new Message();
						messageSecond.what = 1;
						updateRecordTimeHandler.sendMessage(messageSecond);
					}
				} while (MyApplication.isVideoReording);
			}
		}
	}

	final Handler updateRecordTimeHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				secondCount++;
				textRecordTime.setText(DateUtil
						.getFormatTimeBySecond(secondCount));
				break;

			case 2:
				// 停止录像
				if (stopRecorder() == 0) {
					mRecordState = Constant.Record.STATE_RECORD_STOPPED;
					MyApplication.isVideoReording = false;
					setupRecordViews();
				} else {
					if (stopRecorder() == 0) {
						mRecordState = Constant.Record.STATE_RECORD_STOPPED;
						MyApplication.isVideoReording = false;
						setupRecordViews();
					}
				}

				String strVideoCardEject = getResources().getString(
						R.string.sd_remove_badly);
				Toast.makeText(getApplicationContext(), strVideoCardEject,
						Toast.LENGTH_SHORT).show();
				Log.e(Constant.TAG, "CardEjectReceiver:Video SD Removed");
				startSpeak(strVideoCardEject);
				audioRecordDialog.showErrorDialog(strVideoCardEject);
				new Thread(new dismissDialogThread()).start();
				break;

			default:
				break;
			}
		}
	};

	/**
	 * 更新WiF状态
	 */
	private void updateWiFiState() {
		if (wifiManager.isWifiEnabled() && mWifi.isConnected()) {
			int level = ((WifiManager) getSystemService(WIFI_SERVICE))
					.getConnectionInfo().getRssi();// Math.abs()
			imageWifiLevel.setImageResource(SignalUtil
					.getWifiImageBySignal(level));

			// 隐藏导航背景图
			imageDefault.setVisibility(View.GONE);
		} else {
			imageWifiLevel.setImageResource(R.drawable.ic_qs_wifi_no_network);
			// 显示导航背景图:当有离线地图时不需要显示
			// imageDefault.setVisibility(View.VISIBLE);
		}
	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.surfaceCamera:
				if (!isSurfaceLarge) {
					int widthFull = 854;
					int heightFull = 480;
					surfaceCamera
							.setLayoutParams(new RelativeLayout.LayoutParams(
									widthFull, heightFull));
					isSurfaceLarge = true;

					// 更新HorizontalScrollView阴影
					imageShadowLeft.setVisibility(View.GONE);
					imageShadowRight.setVisibility(View.GONE);

					updateButtonState(true);
				} else {
					int widthSmall = 480;
					int heightSmall = 270;
					surfaceCamera
							.setLayoutParams(new RelativeLayout.LayoutParams(
									widthSmall, heightSmall));
					isSurfaceLarge = false;

					// 更新HorizontalScrollView阴影
					hsvMain.scrollTo(0, 0);
					imageShadowLeft.setVisibility(View.GONE);
					imageShadowRight.setVisibility(View.VISIBLE);

					updateButtonState(false);
				}
				break;

			case R.id.smallVideoRecord:
			case R.id.largeVideoRecord:
			case R.id.layoutVideoRecord:
			case R.id.layoutVideoRecordSmall:
				if (!ClickUtil.isQuickClick(1000)) {
					startOrStopRecord();
				}
				break;

			case R.id.smallVideoLock:
			case R.id.largeVideoLock:
			case R.id.layoutVideoLock:
			case R.id.layoutVideoLockSmall:
				if (!ClickUtil.isQuickClick(800)) {
					if (!MyApplication.isVideoLock) {
						MyApplication.isVideoLock = true;
						startSpeak(getResources()
								.getString(R.string.video_lock));
					} else {
						MyApplication.isVideoLock = false;
						startSpeak(getResources().getString(
								R.string.video_unlock));
					}
					setupRecordViews();
				}
				break;

			case R.id.largeVideoSize:
			case R.id.layoutVideoSize:
				if (!ClickUtil.isQuickClick(1500)) {
					// 切换分辨率录像停止，需要重置时间
					MyApplication.isVideoReording = false;
					secondCount = -1; // 录制时间秒钟复位
					textRecordTime.setText("00:00:00");
					textRecordTime.setVisibility(View.INVISIBLE);

					if (mResolutionState == Constant.Record.STATE_RESOLUTION_1080P) {
						setResolution(Constant.Record.STATE_RESOLUTION_720P);
						editor.putString("videoSize", "720");
						mRecordState = Constant.Record.STATE_RECORD_STOPPED;
						startSpeak(getResources().getString(
								R.string.hint_video_size_720));
					} else if (mResolutionState == Constant.Record.STATE_RESOLUTION_720P) {
						setResolution(Constant.Record.STATE_RESOLUTION_1080P);
						editor.putString("videoSize", "1080");
						mRecordState = Constant.Record.STATE_RECORD_STOPPED;
						startSpeak(getResources().getString(
								R.string.hint_video_size_1080));
					}
					editor.commit();
					setupRecordViews();
				}
				break;

			case R.id.largeVideoTime:
			case R.id.layoutVideoTime:
				if (!ClickUtil.isQuickClick(800)) {
					if (mIntervalState == Constant.Record.STATE_INTERVAL_3MIN) {
						if (setInterval(5 * 60) == 0) {
							mIntervalState = Constant.Record.STATE_INTERVAL_5MIN;
							editor.putString("videoTime", "5");
							startSpeak(getResources().getString(
									R.string.hint_video_time_5));
						}
					} else if (mIntervalState == Constant.Record.STATE_INTERVAL_5MIN) {
						if (setInterval(3 * 60) == 0) {
							mIntervalState = Constant.Record.STATE_INTERVAL_3MIN;
							editor.putString("videoTime", "3");
							startSpeak(getResources().getString(
									R.string.hint_video_time_3));
						}
					}
					editor.commit();
					setupRecordViews();
				}
				break;

			case R.id.largeVideoMute:
			case R.id.layoutVideoMute:
				if (!ClickUtil.isQuickClick(800)) {
					if (mMuteState == Constant.Record.STATE_MUTE) {
						if (setMute(false) == 0) {
							mMuteState = Constant.Record.STATE_UNMUTE;
							startSpeak(getResources().getString(
									R.string.hint_video_mute_off));
						}
					} else if (mMuteState == Constant.Record.STATE_UNMUTE) {
						if (setMute(true) == 0) {
							mMuteState = Constant.Record.STATE_MUTE;
							startSpeak(getResources().getString(
									R.string.hint_video_mute_on));
						}
					}
					setupRecordViews();
				}
				break;

			case R.id.smallVideoCamera:
			case R.id.largeVideoCamera:
			case R.id.layoutVideoCamera:
			case R.id.layoutVideoCameraSmall:
				if (!ClickUtil.isQuickClick(500)) {
					takePhoto();
					AudioPlayUtil.playAudio(getApplicationContext(),
							FILE_TYPE_IMAGE);
				}
				break;

			case R.id.layoutWeather:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentWeather = new Intent(MainActivity.this,
							WeatherActivity.class);
					startActivity(intentWeather);
					overridePendingTransition(R.anim.zms_translate_down_out,
							R.anim.zms_translate_down_in);
				}
				break;

			case R.id.mapHideView:
			case R.id.imageNavi:
				// try {
				// ComponentName componentMap = new ComponentName(
				// "com.baidu.BaiduMap",
				// "com.baidu.baidumaps.WelcomeScreen");
				// Intent intentMap = new Intent();
				// intentMap.setComponent(componentMap);
				// startActivity(intentMap);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentNavi = new Intent(MainActivity.this,
							NavigationActivity.class);
					startActivity(intentNavi);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageMusicOL:
				if (!ClickUtil.isQuickClick(800)) {
					ComponentName componentMusic;
					if (Constant.Module.isOnlineMusicKuwo) {
						componentMusic = new ComponentName("cn.kuwo.kwmusichd",
								"cn.kuwo.kwmusichd.WelcomeActivity");
					} else {
						componentMusic = new ComponentName(
								"com.kugou.playerHD2",
								"com.kugou.playerHD.activity.SplashActivity");
					}
					Intent intentMusic = new Intent();
					intentMusic.setComponent(componentMusic);
					startActivity(intentMusic);
				}
				break;

			case R.id.imageMultimedia:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentMultimedia = new Intent(MainActivity.this,
							MultimediaActivity.class);
					startActivity(intentMultimedia);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageRouteTrack:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentRouteTrack = new Intent(MainActivity.this,
							RouteListActivity.class);
					startActivity(intentRouteTrack);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageRoutePlan:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentRoutePlan = new Intent(MainActivity.this,
							RoutePlanActivity.class);
					startActivity(intentRoutePlan);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageFmTransmit:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentFmTransmit = new Intent(MainActivity.this,
							FmTransmitActivity.class);
					startActivity(intentFmTransmit);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageFileExplore:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentFileExplore = new Intent(MainActivity.this,
							FolderActivity.class);
					// Intent intentFileExplore = new Intent(MainActivity.this,
					// VideoListActivity.class);
					startActivity(intentFileExplore);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageNearSearch:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentNearSearch = new Intent(MainActivity.this,
							NearActivity.class);
					startActivity(intentNearSearch);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageVoiceChat:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentVoiceChat;
					if (Constant.Module.isVoiceXunfei) {
						// 讯飞语音
						intentVoiceChat = new Intent(MainActivity.this,
								ChatActivity.class);
					} else {
						// 思必驰语音
						intentVoiceChat = new Intent(MainActivity.this,
								WakeUpCloudAsr.class);
					}
					startActivity(intentVoiceChat);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageDialer:
				// try {
				// ComponentName componentDialer = new ComponentName(
				// "com.android.dialer",
				// "com.android.dialer.DialtactsActivity");
				// Intent intentDialer = new Intent();
				// intentDialer.setComponent(componentDialer);
				// startActivity(intentDialer);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentBTDialer = new Intent(MainActivity.this,
							BluetoothDialerActivity.class);
					startActivity(intentBTDialer);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.imageMessage:
				if (!ClickUtil.isQuickClick(800)) {
					try {
						ComponentName componentMessage = new ComponentName(
								"com.android.mms",
								"com.android.mms.ui.BootActivity");
						Intent intentMessage = new Intent();
						intentMessage.setComponent(componentMessage);
						startActivity(intentMessage);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;

			case R.id.imageSetting:
				if (!ClickUtil.isQuickClick(800)) {
					Intent intentSetting = new Intent(MainActivity.this,
							SettingActivity.class);
					startActivity(intentSetting);
					overridePendingTransition(R.anim.zms_translate_up_out,
							R.anim.zms_translate_up_in);
				}
				break;

			case R.id.layoutWiFi:
				if (!ClickUtil.isQuickClick(800)) {
					if (Constant.Module.isWifiSystem) {
						startActivity(new Intent(
								android.provider.Settings.ACTION_WIFI_SETTINGS));
					} else {
						Intent intentWiFi = new Intent(MainActivity.this,
								WifiListActivity.class);
						startActivity(intentWiFi);
					}
				}
				break;

			default:
				break;
			}
		}
	}

	/**
	 * 开启或关闭录像
	 */
	private void startOrStopRecord() {
		if (mRecordState == Constant.Record.STATE_RECORD_STOPPED) {
			if (startRecorder() == 0) {
				mRecordState = Constant.Record.STATE_RECORD_STARTED;
				MyApplication.isVideoReording = true;
			} else {
				Log.e(Constant.TAG, "Start Record failed");
			}
		} else if (mRecordState == Constant.Record.STATE_RECORD_STARTED) {
			if (stopRecorder() == 0) {
				mRecordState = Constant.Record.STATE_RECORD_STOPPED;
				MyApplication.isVideoReording = false;
			}
		}
		AudioPlayUtil.playAudio(getApplicationContext(), FILE_TYPE_VIDEO);
		setupRecordViews();
		Log.v(Constant.TAG, "MyApplication.isVideoReording:"
				+ MyApplication.isVideoReording);
	}

	class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {

			boolean hasNetwork = NetworkUtil
					.isNetworkConnected(getApplicationContext());
			// MapView 销毁后不在处理新接收的位置
			if (location == null || mainMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(0)
					// accuracy设为0去掉蓝色精度圈，RAW:.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			baiduMap.setMyLocationData(locData);

			// if (isFirstLoc) {
			LatLng ll = new LatLng(location.getLatitude(),
					location.getLongitude());
			MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
			baiduMap.animateMapStatus(u);
			// }

			// 存储非“未定位”的城市信息
			if (!strNotLocate.equals(location.getCity())) {
				editor.putString("cityNameRealButOld", location.getCity());
				editor.commit();
			}

			// 城市名发生变化，需要更新位置和天气
			if (!sharedPreferences.getString("cityName", strNotLocate).equals(
					location.getCity())
					&& hasNetwork) {
				startWeatherService();
				editor.putString("cityName", location.getCity());

				editor.commit();
			}

			// 如果初次定位未联网，则不将isFirstLoc置为false
			if (hasNetwork && isFirstLoc) {
				isFirstLoc = false;
				// 更新天气
				startWeatherService();
				// 更新位置和天气信息
				updateLocationAndWeather();
			}
			String cityName = location.getCity();
			if (hasNetwork && (cityName != null)
					&& (!cityName.equals(strNotLocate))) {

				// editor.putLong("cityCode", cityCode);
				editor.putString("cityName", cityName);
				editor.putString("cityNameRealButOld", cityName);
				editor.putString("latitude", "" + location.getLatitude());
				editor.putString("longitude", "" + location.getLongitude());
				editor.putString("district", location.getDistrict());
				// editor.putString("floor", location.getFloor());
				editor.putString("addrStr", location.getAddrStr());
				editor.putString("street", location.getStreet());
				editor.putString("streetNum", location.getStreetNumber());
				// editor.putFloat("speed", location.getSpeed());
				editor.putString("altitude", "" + location.getAltitude());
				editor.putString("lbsTime", location.getTime());
				editor.commit();

			}
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	/**
	 * 
	 * @param tempMode
	 *            LocationMode.Hight_Accuracy-高精度
	 *            LocationMode.Battery_Saving-低功耗
	 *            LocationMode.Device_Sensors-仅设备
	 * @param tempCoor
	 *            gcj02-国测局加密经纬度坐标 bd09ll-百度加密经纬度坐标 bd09-百度加密墨卡托坐标
	 * @param frequence
	 *            MIN_SCAN_SPAN = 1000; MIN_SCAN_SPAN_NETWORK = 3000;
	 * @param isNeedAddress
	 *            是否需要地址
	 */
	private void InitLocation(
			com.baidu.location.LocationClientOption.LocationMode tempMode,
			String tempCoor, int frequence, boolean isNeedAddress) {

		mLocationClient = new LocationClient(this.getApplicationContext());
		mLocationClient.registerLocationListener(new MyLocationListener());
		// mGeofenceClient = new GeofenceClient(getApplicationContext());

		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(tempMode);
		option.setCoorType(tempCoor);
		option.setScanSpan(frequence);
		option.setOpenGps(true); // 打开gps
		option.setIsNeedAddress(isNeedAddress);
		mLocationClient.setLocOption(option);

		mLocationClient.start();
	}

	private int oldWifiLevel = 0;

	/**
	 * WiFi状态Receiver
	 */
	private BroadcastReceiver wifiIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int wifi_state = intent.getIntExtra("wifi_state", 0);
			int level = ((WifiManager) getSystemService(WIFI_SERVICE))
					.getConnectionInfo().getRssi();// Math.abs()
			if (oldWifiLevel != level) {
				updateWiFiState();
				oldWifiLevel = level;
				Log.v(Constant.TAG, "wifiIntentReceiver, Wifi Level:" + level);
			}

			switch (wifi_state) {
			case WifiManager.WIFI_STATE_ENABLED:
			case WifiManager.WIFI_STATE_ENABLING:
			case WifiManager.WIFI_STATE_DISABLING:
			case WifiManager.WIFI_STATE_DISABLED:
			case WifiManager.WIFI_STATE_UNKNOWN:
				updateWiFiState();
				break;
			}
		}
	};

	/**
	 * 更新天气
	 */
	private void startWeatherService() {
		Intent intent = new Intent(this, WeatherService.class);
		startService(intent);
	}

	@Override
	protected void onPause() {
		Log.v(Constant.TAG, "MainActivity:MapView onPause");
		mainMapView.onPause();

		// 销毁定位
		mLocationClient.stop();

		// 3G信号
		Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);

		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.v(Constant.TAG, "MainActivity:MapView onResume");

		mainMapView.onResume();

		// LocationMode 跟随：FOLLOWING 普通：NORMAL 罗盘：COMPASS
		currentMode = com.baidu.mapapi.map.MyLocationConfiguration.LocationMode.NORMAL;
		baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
				currentMode, true, null));
		InitLocation(
				com.baidu.location.LocationClientOption.LocationMode.Hight_Accuracy,
				"bd09ll", scanSpan, true);

		// 注册wifi消息处理器
		registerReceiver(wifiIntentReceiver, wifiIntentFilter);

		// 更新录像界面按钮状态
		refreshRecordButton();
		setupRecordViews();

		// 3G信号
		Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		// 导航实例
		if (MyApplication.isNaviAuthSuccess) {
			Log.v(Constant.TAG, "Navi Instance Already Initial Success");
		} else {
			initialNaviInstance();
			Log.v(Constant.TAG, "Navi Instance is Initialing...");
		}

		// 初始化fm发射
		initFmTransmit();

		super.onResume();
	}

	@Override
	protected void onDestroy() {

		// 关闭定位图层
		baiduMap.setMyLocationEnabled(false);
		mainMapView.onDestroy();
		mainMapView = null;

		// 取消注册wifi消息处理器
		unregisterReceiver(wifiIntentReceiver);
		super.onDestroy();

		// 录像区域
		release();
	}

	// *********** Record ***********

	/**
	 * 设置录制初始值
	 */
	private void setupRecordDefaults() {
		refreshRecordButton();

		mRecordState = Constant.Record.STATE_RECORD_STOPPED;
		MyApplication.isVideoReording = false;

		mPathState = Constant.Record.STATE_PATH_ZERO;
		mSecondaryState = Constant.Record.STATE_SECONDARY_DISABLE;
		mOverlapState = Constant.Record.STATE_OVERLAP_FIVE;

		mMuteState = Constant.Record.STATE_UNMUTE;
	}

	private void refreshRecordButton() {
		// 视频尺寸
		String videoSizeStr = sharedPreferences.getString("videoSize", "720");
		if ("1080".equals(videoSizeStr)) {
			mResolutionState = Constant.Record.STATE_RESOLUTION_1080P;
		} else {
			mResolutionState = Constant.Record.STATE_RESOLUTION_720P;
		}

		// 视频分段
		String videoTimeStr = sharedPreferences.getString("videoTime", "5");
		if ("3".equals(videoTimeStr)) {
			mIntervalState = Constant.Record.STATE_INTERVAL_3MIN;
		} else {
			mIntervalState = Constant.Record.STATE_INTERVAL_5MIN;
		}
	}

	private void setupRecordViews() {
		// 视频分辨率
		if (mResolutionState == Constant.Record.STATE_RESOLUTION_720P) {
			largeVideoSize.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_size_720));
		} else if (mResolutionState == Constant.Record.STATE_RESOLUTION_1080P) {
			largeVideoSize.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_size_1080));
		}

		// 录像按钮
		if (mRecordState == Constant.Record.STATE_RECORD_STOPPED) {
			largeVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_record));
			smallVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_record));
		} else if (mRecordState == Constant.Record.STATE_RECORD_STARTED) {
			largeVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_pause));
			smallVideoRecord.setBackground(getResources().getDrawable(
					R.drawable.ui_main_video_pause));
		}

		// 视频分段
		if (mIntervalState == Constant.Record.STATE_INTERVAL_3MIN) {
			largeVideoTime.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_time_3));
		} else if (mIntervalState == Constant.Record.STATE_INTERVAL_5MIN) {
			largeVideoTime.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_time_5));
		}

		// 视频加锁
		if (MyApplication.isVideoLock) {
			smallVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_lock));
			largeVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_lock));
		} else {
			smallVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_unlock));
			largeVideoLock.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_unlock));
		}

		// 静音
		if (mMuteState == Constant.Record.STATE_MUTE) {
			largeVideoMute.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_sound_off));
		} else if (mMuteState == Constant.Record.STATE_UNMUTE) {
			largeVideoMute.setBackground(getResources().getDrawable(
					R.drawable.ui_camera_video_sound_on));
		}

		// 路径
		// if (mPathState == STATE_PATH_ZERO) {
		// mPathBtn.setText(R.string.path_zero);
		// } else if (mPathState == STATE_PATH_ONE) {
		// mPathBtn.setText(R.string.path_one);
		// } else if (mPathState == STATE_PATH_TWO) {
		// mPathBtn.setText(R.string.path_two);
		// }

		// if (mSecondaryState == STATE_SECONDARY_DISABLE) {
		// mSecondaryBtn.setText(R.string.secondary_disable);
		// } else if (mSecondaryState == STATE_SECONDARY_ENABLE) {
		// mSecondaryBtn.setText(R.string.secondary_enable);
		// }

		// if (mOverlapState == STATE_OVERLAP_ZERO) {
		// mOverlapBtn.setText(R.string.nooverlap);
		// } else if (mOverlapState == STATE_OVERLAP_FIVE) {
		// mOverlapBtn.setText(R.string.overlap_5s);
		// }

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// mHolder = holder;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		if (mCamera == null) {
			mHolder = holder;
			setup();
		} else {
			try {
				mCamera.lock();
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
				mCamera.unlock();
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	private boolean openCamera() {
		if (mCamera != null) {
			closeCamera();
		}
		try {
			mCamera = Camera.open(0);
			mCamera.lock();

			// 设置系统Camera参数
			// Camera.Parameters para = mCamera.getParameters();
			// para.unflatten(Constant.CAMERA_PARAMS);
			// mCamera.setParameters(para);

			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
			mCamera.unlock();
			return true;
		} catch (Exception ex) {
			closeCamera();
			return false;
		}
	}

	private boolean closeCamera() {
		if (mCamera == null)
			return true;
		try {
			mCamera.lock();
			mCamera.stopPreview();
			mCamera.setPreviewDisplay(null);
			mCamera.release();
			mCamera.unlock();
			mCamera = null;
			return true;
		} catch (Exception ex) {
			mCamera = null;
			return false;
		}
	}

	/**
	 * 删除最旧视频
	 */
	private boolean deleteOldestUnlockVideo() {
		try {
			String sdcardPath = Constant.Path.SDCARD_1 + File.separator;// "/storage/sdcard1/";
			if (Constant.Record.saveVideoToSD2) {
				sdcardPath = Constant.Path.SDCARD_2 + File.separator;// "/storage/sdcard2/";
			}
			// sharedPreferences.getString("sdcardPath","/mnt/sdcard2");
			float sdFree = StorageUtil.getSDAvailableSize(sdcardPath);
			float sdTotal = StorageUtil.getSDTotalSize(sdcardPath);
			while (sdFree < sdTotal * Constant.Record.SD_MIN_FREE_PERCENT) {
				int oldestUnlockVideoId = videoDb.getOldestUnlockVideoId();
				// 删除较旧未加锁视频文件
				if (oldestUnlockVideoId != -1) {
					String oldestUnlockVideoName = videoDb
							.getVideNameById(oldestUnlockVideoId);
					File f = new File(sdcardPath + "tachograph/"
							+ oldestUnlockVideoName.split("_")[0]
							+ File.separator + oldestUnlockVideoName);
					if (f.exists() && f.isFile()) {
						f.delete();
						if (Constant.isDebug) {
							Log.d(Constant.TAG,
									"Delete Old Unlock Video:" + f.getName());
						}
					}
					// 删除数据库记录
					videoDb.deleteDriveVideoById(oldestUnlockVideoId);
				} else {
					int oldestVideoId = videoDb.getOldestVideoId();
					if (oldestVideoId == -1) {
						/**
						 * 有一种情况：数据库中无视频信息。导致的原因：
						 * 1：升级时选Download的话，不会清理USB存储空间，应用数据库被删除； 2：应用被清除数据
						 * 这种情况下旧视频无法直接删除， 此时如果满存储，需要直接删除
						 */
						File file = new File(sdcardPath + "tachograph/");
						RecursionDeleteFile(file);
						Log.e(Constant.TAG, "!!! Delete tachograph/ Directory");
						sdFree = StorageUtil.getSDAvailableSize(sdcardPath);
						if (sdFree < sdTotal
								* Constant.Record.SD_MIN_FREE_PERCENT) {
							// 此时若空间依然不足,提示用户清理存储（已不是行车视频的原因）
							Log.e(Constant.TAG, "Storage is full...");

							String strNoStorage = getResources().getString(
									R.string.storage_full_cause_by_other);

							audioRecordDialog.showErrorDialog(strNoStorage);
							// new Thread(new dismissDialogThread()).start();
							startSpeak(strNoStorage);

							return false;
						}
					} else {
						// 提示用户清理空间，删除较旧的视频（加锁）
						String strStorageFull = getResources().getString(
								R.string.storage_full_and_delete_lock);
						startSpeak(strStorageFull);
						Toast.makeText(getApplicationContext(), strStorageFull,
								Toast.LENGTH_SHORT).show();

						String oldestVideoName = videoDb
								.getVideNameById(oldestVideoId);
						File f = new File(sdcardPath + "tachograph/"
								+ oldestVideoName.split("_")[0]
								+ File.separator + oldestVideoName);
						if (f.exists() && f.isFile()) {
							f.delete();
							if (Constant.isDebug) {
								Log.d(Constant.TAG, "Delete Old Lock Video:"
										+ f.getName());
							}
						}
						// 删除数据库记录
						videoDb.deleteDriveVideoById(oldestVideoId);
					}
				}
				// 更新剩余空间
				sdFree = StorageUtil.getSDAvailableSize(sdcardPath);
			}
			return true;
		} catch (Exception e) {
			/*
			 * 异常原因：1.文件由用户手动删除
			 */
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 递归删除文件和文件夹
	 * 
	 * @param file
	 *            要删除的根目录
	 */
	public static void RecursionDeleteFile(File file) {
		if (file.isFile()) {
			file.delete();
			return;
		}
		if (file.isDirectory()) {
			File[] childFile = file.listFiles();
			if (childFile == null || childFile.length == 0) {
				file.delete();
				return;
			}
			for (File f : childFile) {
				RecursionDeleteFile(f);
			}
			file.delete();
		}
	}

	public int startRecorder() {

		if (!StorageUtil.isVideoCardExists()) {
			// SDCard2不存在
			String strNoSD = getResources().getString(R.string.sd1_not_exist);
			if (Constant.Record.saveVideoToSD2) {
				strNoSD = getResources().getString(R.string.sd2_not_exist);
			}
			audioRecordDialog.showErrorDialog(strNoSD);
			new Thread(new dismissDialogThread()).start();
			startSpeak(strNoSD);
			return -1;
		} else if (mMyRecorder != null) {
			if (deleteOldestUnlockVideo()) {
				textRecordTime.setVisibility(View.VISIBLE);
				new Thread(new updateRecordTimeThread()).start(); // 更新录制时间
				if (Constant.isDebug) {
					Log.d(Constant.TAG, "Record Start");
				}
				// 设置保存路径
				if (Constant.Record.saveVideoToSD2) {
					setDirectory(Constant.Path.SDCARD_2);
				} else {
					setDirectory(Constant.Path.SDCARD_1);
				}
				return mMyRecorder.start();
			}
		}
		return -1;
	}

	public class dismissDialogThread implements Runnable {
		@Override
		public void run() {
			synchronized (dismissDialogHandler) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Message messageEject = new Message();
				messageEject.what = 1;
				dismissDialogHandler.sendMessage(messageEject);
			}
		}
	}

	final Handler dismissDialogHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				audioRecordDialog.dismissDialog();
				break;

			default:
				break;
			}
		}
	};

	public int stopRecorder() {
		if (mMyRecorder != null) {
			secondCount = -1; // 录制时间秒钟复位
			textRecordTime.setText("00:00:00");
			textRecordTime.setVisibility(View.INVISIBLE);
			if (Constant.isDebug) {
				Log.d(Constant.TAG, "Record Stop");
			}
			return mMyRecorder.stop();
		}

		return -1;
	}

	public int setInterval(int seconds) {
		if (mMyRecorder != null) {
			return mMyRecorder.setVideoSeconds(seconds);
		}
		return -1;
	}

	public int setOverlap(int seconds) {
		if (mMyRecorder != null) {
			return mMyRecorder.setVideoOverlap(seconds);
		}
		return -1;
	}

	public int takePhoto() {
		if (!StorageUtil.isVideoCardExists()) {
			// SDCard不存在
			String strNoSD = getResources().getString(R.string.sd1_not_exist);
			if (Constant.Record.saveVideoToSD2) {
				strNoSD = getResources().getString(R.string.sd2_not_exist);
			}
			audioRecordDialog.showErrorDialog(strNoSD);
			new Thread(new dismissDialogThread()).start();
			startSpeak(strNoSD);
			return -1;
		} else if (mMyRecorder != null) {
			return mMyRecorder.takePicture();
		}
		return -1;
	}

	public int setDirectory(String dir) {
		if (mMyRecorder != null) {
			return mMyRecorder.setDirectory(dir);
		}
		return -1;
	}

	private int setMute(boolean mute) {
		if (mMyRecorder != null) {
			return mMyRecorder.setMute(mute);
		}
		return -1;
	}

	public int setResolution(int state) {
		if (state != mResolutionState) {
			mResolutionState = state;
			release();
			if (openCamera()) {
				setupRecorder();
			}
		}
		return -1;
	}

	public int setSecondary(int state) {
		if (state == Constant.Record.STATE_SECONDARY_DISABLE) {
			if (mMyRecorder != null) {
				return mMyRecorder.setSecondaryVideoEnable(false);
			}
		} else if (state == Constant.Record.STATE_SECONDARY_ENABLE) {
			if (mMyRecorder != null) {
				mMyRecorder.setSecondaryVideoSize(320, 240);
				mMyRecorder.setSecondaryVideoFrameRate(30);
				mMyRecorder.setSecondaryVideoBiteRate(120000);
				return mMyRecorder.setSecondaryVideoEnable(true);
			}
		}
		return -1;
	}

	private void setupRecorder() {
		releaseRecorder();
		mMyRecorder = new TachographRecorder();
		mMyRecorder.setTachographCallback(this);
		mMyRecorder.setCamera(mCamera);
		mMyRecorder.setClientName(this.getPackageName());
		if (mResolutionState == Constant.Record.STATE_RESOLUTION_1080P) {
			mMyRecorder.setVideoSize(1920, 1088); // 16倍数
			mMyRecorder.setVideoFrameRate(30);
			mMyRecorder.setVideoBiteRate(9000000 * 2); // 8500000
		} else {
			mMyRecorder.setVideoSize(1280, 720);
			mMyRecorder.setVideoFrameRate(30);
			mMyRecorder.setVideoBiteRate(9000000); // 3500000
		}
		if (mSecondaryState == Constant.Record.STATE_SECONDARY_ENABLE) {
			mMyRecorder.setSecondaryVideoEnable(true);
			mMyRecorder.setSecondaryVideoSize(320, 240);
			mMyRecorder.setSecondaryVideoFrameRate(30);
			mMyRecorder.setSecondaryVideoBiteRate(120000);
		} else {
			mMyRecorder.setSecondaryVideoEnable(false);
		}
		if (mIntervalState == Constant.Record.STATE_INTERVAL_5MIN) {
			mMyRecorder.setVideoSeconds(5 * 60);
		} else {
			mMyRecorder.setVideoSeconds(3 * 60);
		}
		if (mOverlapState == Constant.Record.STATE_OVERLAP_FIVE) {
			mMyRecorder.setVideoOverlap(5);
		}
		mMyRecorder.prepare();
	}

	private void releaseRecorder() {
		if (mMyRecorder != null) {
			mMyRecorder.stop();
			mMyRecorder.close();
			mMyRecorder.release();
			mMyRecorder = null;
			if (Constant.isDebug) {
				Log.d(Constant.TAG, "Record Release");
			}
		}
	}

	@Override
	public void onError(int err) {
		if (Constant.isDebug) {
			Log.e(Constant.TAG, "Record Error : " + err);
		}
	}

	@Override
	public void onFileSave(int type, String path) {
		/**
		 * [Type] 0-图片 1-视频
		 * 
		 * [Path] 视频：/mnt/sdcard/tachograph/2015-07-01/2015-07-01_105536.mp4
		 * 图片:/mnt/sdcard/tachograph/camera_shot/2015-07-01_105536.jpg
		 */
		deleteOldestUnlockVideo();
		Log.v(Constant.TAG, "Save path:" + path);
		if (type == 1) {
			String videoName = path.split("/")[5];
			editor.putString("sdcardPath", "/mnt/" + path.split("/")[2] + "/");
			editor.commit();
			int videoResolution = 720;
			int videoLock = 0;

			if (mResolutionState == Constant.Record.STATE_RESOLUTION_1080P) {
				videoResolution = 1080;
			}
			if (MyApplication.isVideoLock) {
				videoLock = 1;
				MyApplication.isVideoLock = false; // 还原
				setupRecordViews(); // 更新录制按钮状态
			}
			DriveVideo driveVideo = new DriveVideo(videoName, videoLock,
					videoResolution);
			videoDb.addDriveVideo(driveVideo);
		} else {
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.photo_save),
					Toast.LENGTH_SHORT).show();
		}

		// 更新Media Database
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
				Uri.parse("file://" + path)));
		if (Constant.isDebug) {
			Log.d(Constant.TAG, "File Save, Type=" + type);
		}
	}

	public void setup() {
		release();
		if (openCamera()) {
			setupRecorder();
		}
	}

	public void release() {
		releaseRecorder();
		closeCamera();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// 如果视频全屏预览开启，返回关闭
			if (isSurfaceLarge()) {
				int widthSmall = 480;
				int heightSmall = 270;
				surfaceCamera.setLayoutParams(new RelativeLayout.LayoutParams(
						widthSmall, heightSmall));
				isSurfaceLarge = false;
				// 更新HorizontalScrollView阴影
				hsvMain.scrollTo(0, 0);
				imageShadowLeft.setVisibility(View.GONE);
				imageShadowRight.setVisibility(View.VISIBLE);

				updateButtonState(false);
			}
			return true;
		} else
			return super.onKeyDown(keyCode, event);
	}

	/**
	 * 启动时初始化fm发射
	 */
	// 频率节点 频率范围：7600~10800:8750-10800

	private void initFmTransmit() {
		// if (isFmTransmitOn())
		{
			int freq = SettingUtil.getFmFrequceny(this);
			// Toast.makeText(this, "freq : " + freq, Toast.LENGTH_LONG).show();
			if (freq >= 8750 && freq <= 10800)
				SettingUtil.setFmFrequency(this, freq);
			else
				SettingUtil.setFmFrequency(this, 8750);
		}
	}
}
