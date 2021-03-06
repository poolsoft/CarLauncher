package com.tchip.carlauncher.ui.activity;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionResult.SuggestionInfo;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.utils.DistanceUtil;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.MyApplication;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.adapter.NaviHistoryAdapter;
import com.tchip.carlauncher.adapter.NaviResultAdapter;
import com.tchip.carlauncher.model.NaviHistory;
import com.tchip.carlauncher.model.NaviHistoryDbHelper;
import com.tchip.carlauncher.model.NaviResultInfo;
import com.tchip.carlauncher.service.SpeakService;
import com.tchip.carlauncher.util.NetworkUtil;
import com.tchip.carlauncher.view.AudioRecordDialog;

import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.baidu.navisdk.adapter.BaiduNaviManager.RoutePlanListener;

public class NavigationActivity extends FragmentActivity implements
		OnGetPoiSearchResultListener, OnGetSuggestionResultListener {

	private PoiSearch mPoiSearch = null;
	private SuggestionSearch mSuggestionSearch = null;
	private BaiduMap mBaiduMap = null;

	private ArrayAdapter<String> sugAdapter = null;

	private double mLatitude, mLongitude;
	private LatLng nowLatLng;

	private EditText etHistoryWhere, etHistoryCity;
	private LinearLayout layoutNearAdvice, layoutShowHistory,
			layoutStarContent, layoutStarEditWork, layoutStarEditHome;
	private RelativeLayout layoutNaviVoice, layoutNear, layoutHistory,
			layoutHistoryBack, layoutRoadCondition, layoutStar,
			layoutStarNaviWork, layoutStarNaviHome, layoutResult,
			layoutPagePriv, layoutPageNext;

	private AudioRecordDialog audioRecordDialog;

	// 语义理解对象（语音到语义）。
	private SpeechUnderstander mSpeechUnderstander;

	private ListView listResult, listHistory;
	private ArrayList<NaviResultInfo> naviArray;
	private NaviResultAdapter naviResultAdapter;

	private boolean isResultListShow = false;
	private boolean isNearLayoutShow = false;
	private boolean isHistoryLayoutShow = false;
	private boolean isStarPannelShow = false;

	private Button btnHistoryNavi, btnCloseHistory;

	private NaviHistoryDbHelper naviDb;
	private NaviHistoryAdapter naviHistoryAdapter;
	private ArrayList<NaviHistory> naviHistoryArray;

	private ImageView imageRoadCondition, imgVoiceSearch;

	private ProgressBar progressVoice;

	private MapView mMapView;
	private LocationClient mLocationClient;
	private SharedPreferences preference;
	private Editor editor;
	private String naviDesFromVoice = "";

	private boolean isFirstLoc = true;

	private String strAuthFail, strInitFail;

	/**
	 * 设置每页容量，默认为每页条数
	 */
	private int pageCapacity = 6;
	private int pageIndex = 0;

	private String doingWhere;
	private LatLng doingLatLng;
	private boolean doingIsNear;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
		setContentView(R.layout.activity_navigation);

		strAuthFail = getResources().getString(R.string.hint_navi_auth_fail);
		strInitFail = getResources().getString(R.string.hint_navi_init_fail);

		naviDb = new NaviHistoryDbHelper(getApplicationContext());

		audioRecordDialog = new AudioRecordDialog(NavigationActivity.this);

		// 获取当前经纬度
		preference = getSharedPreferences(Constant.SHARED_PREFERENCES_NAME,
				Context.MODE_PRIVATE);
		editor = preference.edit();
		mLatitude = Double
				.parseDouble(preference.getString("latitude", "0.00"));
		mLongitude = Double.parseDouble(preference.getString("longitude",
				"0.00"));

		// 初始化搜索模块，注册搜索事件监听
		mPoiSearch = PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
		mSuggestionSearch = SuggestionSearch.newInstance();
		mSuggestionSearch.setOnGetSuggestionResultListener(this);

		Button btnToNearFromResult = (Button) findViewById(R.id.btnToNearFromResult);
		btnToNearFromResult.setOnClickListener(new MyOnClickListener());

		initialLayout();

		// 接收来自语音的导航目的地
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			naviDesFromVoice = extras.getString("destionation");
			if (naviDesFromVoice.trim().length() > 0
					&& naviDesFromVoice != null) {
				setLayoutHistoryVisibility(true);
				setDestinationText(naviDesFromVoice);
				startSearchPlace(naviDesFromVoice, nowLatLng, false, false);
			}
		}
	}

	private void initialLayout() {
		layoutShowHistory = (LinearLayout) findViewById(R.id.layoutShowHistory);
		layoutShowHistory.setOnClickListener(new MyOnClickListener());

		layoutNaviVoice = (RelativeLayout) findViewById(R.id.layoutNaviVoice);
		layoutNaviVoice.setOnClickListener(new MyOnClickListener());

		layoutNear = (RelativeLayout) findViewById(R.id.layoutNear);
		layoutNear.setOnClickListener(new MyOnClickListener());

		// 周边搜索
		layoutNearAdvice = (LinearLayout) findViewById(R.id.layoutNearAdvice);
		RelativeLayout layoutNearOilStation = (RelativeLayout) findViewById(R.id.layoutNearOilStation);
		layoutNearOilStation.setOnClickListener(new MyOnClickListener());

		RelativeLayout layoutNearParking = (RelativeLayout) findViewById(R.id.layoutNearParking);
		layoutNearParking.setOnClickListener(new MyOnClickListener());

		RelativeLayout layoutNear4s = (RelativeLayout) findViewById(R.id.layoutNear4s);
		layoutNear4s.setOnClickListener(new MyOnClickListener());

		RelativeLayout layoutNearBank = (RelativeLayout) findViewById(R.id.layoutNearBank);
		layoutNearBank.setOnClickListener(new MyOnClickListener());

		RelativeLayout layoutShop = (RelativeLayout) findViewById(R.id.layoutShop);
		layoutShop.setOnClickListener(new MyOnClickListener());

		RelativeLayout layoutNearHotel = (RelativeLayout) findViewById(R.id.layoutNearHotel);
		layoutNearHotel.setOnClickListener(new MyOnClickListener());

		listResult = (ListView) findViewById(R.id.listResult);

		mMapView = (MapView) findViewById(R.id.map);

		// 去掉百度Logo
		int count = mMapView.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = mMapView.getChildAt(i);
			if (child instanceof ImageView) {
				child.setVisibility(View.INVISIBLE);
			}
		}
		mBaiduMap = mMapView.getMap();

		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		// 自定义Maker
		// BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory
		// .fromResource(R.drawable.icon_arrow_up);

		// 初始化地图位置,设置nowLoction数据以防NullPointer
		nowLatLng = new LatLng(mLatitude, mLongitude);
		// MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(nowLatLng);
		// mBaiduMap.animateMapStatus(u);

		// 设置地图放大级别 0-19
		MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(14);
		mBaiduMap.animateMapStatus(msu);

		mBaiduMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public boolean onMapPoiClick(MapPoi mapPoi) {
				setStarPannelVisibility(false);
				return false;
			}

			@Override
			public void onMapClick(LatLng latLng) {
				setStarPannelVisibility(false);
			}
		});

		// 导航搜索历史记录
		layoutHistory = (RelativeLayout) findViewById(R.id.layoutHistory);
		layoutHistoryBack = (RelativeLayout) findViewById(R.id.layoutHistoryBack);
		layoutHistoryBack.setOnClickListener(new MyOnClickListener());
		btnCloseHistory = (Button) findViewById(R.id.btnCloseHistory);
		btnCloseHistory.setOnClickListener(new MyOnClickListener());

		etHistoryWhere = (EditText) findViewById(R.id.etHistoryWhere);
		etHistoryCity = (EditText) findViewById(R.id.etHistoryCity);
		btnHistoryNavi = (Button) findViewById(R.id.btnHistoryNavi);
		btnHistoryNavi.setOnClickListener(new MyOnClickListener());

		listHistory = (ListView) findViewById(R.id.listHistory);

		// 路况
		layoutRoadCondition = (RelativeLayout) findViewById(R.id.layoutRoadCondition);
		layoutRoadCondition.setOnClickListener(new MyOnClickListener());

		imageRoadCondition = (ImageView) findViewById(R.id.imageRoadCondition);

		// 语音识别进度
		progressVoice = (ProgressBar) findViewById(R.id.progressVoice);
		imgVoiceSearch = (ImageView) findViewById(R.id.imgVoiceSearch);

		// 收藏
		layoutStarContent = (LinearLayout) findViewById(R.id.layoutStarContent);

		layoutStar = (RelativeLayout) findViewById(R.id.layoutStar);
		layoutStar.setOnClickListener(new MyOnClickListener());

		layoutStarNaviWork = (RelativeLayout) findViewById(R.id.layoutStarNaviWork);
		layoutStarNaviWork.setOnClickListener(new MyOnClickListener());

		layoutStarEditWork = (LinearLayout) findViewById(R.id.layoutStarEditWork);
		layoutStarEditWork.setOnClickListener(new MyOnClickListener());

		layoutStarEditHome = (LinearLayout) findViewById(R.id.layoutStarEditHome);
		layoutStarEditHome.setOnClickListener(new MyOnClickListener());

		layoutStarNaviHome = (RelativeLayout) findViewById(R.id.layoutStarNaviHome);
		layoutStarNaviHome.setOnClickListener(new MyOnClickListener());

		// 更新离线地图
		RelativeLayout layoutOffline = (RelativeLayout) findViewById(R.id.layoutOffline);
		layoutOffline.setOnClickListener(new MyOnClickListener());

		// 回到当前位置
		RelativeLayout layoutLocate = (RelativeLayout) findViewById(R.id.layoutLocate);
		layoutLocate.setOnClickListener(new MyOnClickListener());

		layoutResult = (RelativeLayout) findViewById(R.id.layoutResult);
		layoutResult.setOnClickListener(new MyOnClickListener());

		layoutPagePriv = (RelativeLayout) findViewById(R.id.layoutPagePriv);
		layoutPagePriv.setOnClickListener(new MyOnClickListener());

		layoutPageNext = (RelativeLayout) findViewById(R.id.layoutPageNext);
		layoutPageNext.setOnClickListener(new MyOnClickListener());
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
		mLocationClient.setLocOption(option);

		mLocationClient.start();
	}

	private void whereAmI(LatLng latLng) {
		MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(latLng);
		mBaiduMap.animateMapStatus(u);
	}

	class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			// MapView 销毁后不在处理新接收的位置
			if (location == null || mMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(0)
					// accuracy设为0去掉蓝色精度圈，RAW:.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);

			// 更新当前位置用作导航起点
			nowLatLng = new LatLng(location.getLatitude(),
					location.getLongitude());
			// 初次定位和移动过程，更新当前位置到地图中心
			if (isFirstLoc || location.getSpeed() > 0) {
				isFirstLoc = false;
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(nowLatLng);
				mBaiduMap.animateMapStatus(u);
			}

			// 存储位置到SharedPreference
			editor.putString("latitude", "" + location.getLatitude());
			editor.putString("longitude", "" + location.getLongitude());
			editor.commit();
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnToNearFromResult:
				if (isStarPannelShow || isNearLayoutShow || isResultListShow) {
					setStarPannelVisibility(false);
					setLayoutNearVisibility(false);
					setLayoutHistoryVisibility(false);
				} else {
					finish();
				}
				break;

			case R.id.layoutNaviVoice:
				isNearLayoutShow = false;
				layoutNearAdvice.setVisibility(View.GONE);

				startVoiceUnderstand();
				break;

			case R.id.layoutShowHistory:
				setLayoutHistoryVisibility(!isHistoryLayoutShow);
				break;

			// 周边搜索
			case R.id.layoutNear:
				setLayoutNearVisibility(!isNearLayoutShow);
				break;

			case R.id.layoutNearOilStation:
				searchNear(getResources().getString(R.string.near_oil_station));
				break;

			case R.id.layoutNearParking:
				searchNear(getResources().getString(R.string.near_parking));
				break;

			case R.id.layoutNear4s:
				searchNear(getResources().getString(R.string.near_4s));
				break;

			case R.id.layoutNearBank:
				searchNear(getResources().getString(R.string.near_atm));
				break;

			case R.id.layoutShop:
				searchNear(getResources().getString(R.string.near_market));
				break;

			case R.id.layoutNearHotel:
				searchNear(getResources().getString(R.string.near_hotel));
				break;

			// 历史记录
			case R.id.layoutHistoryBack:
			case R.id.btnCloseHistory:
				setLayoutHistoryVisibility(false);
				break;

			case R.id.btnHistoryNavi:
				String strContent = etHistoryWhere.getText().toString();
				if (strContent.trim().length() > 0 && strContent != null) {
					if (Constant.MagicCode.DEVICE_TEST.equals(strContent)) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setClassName("com.DeviceTest",
								"com.DeviceTest.DeviceTest");
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} else if (Constant.MagicCode.ENGINEER_MODE
							.equals(strContent)) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setClassName("com.mediatek.engineermode",
								"com.mediatek.engineermode.EngineerMode");
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} else if (Constant.MagicCode.SETTING.equals(strContent)) {
						ComponentName componentImage = new ComponentName(
								"com.android.settings",
								"com.android.settings.Settings");
						Intent intentImage = new Intent();
						intentImage.setComponent(componentImage);
						startActivity(intentImage);
					} else {
						isNearLayoutShow = false;
						layoutNearAdvice.setVisibility(View.GONE);

						startSearchPlace(strContent, nowLatLng, false, false);

						// 使用‘百度导航’进行导航
						// try {
						// Uri uri = Uri.parse("bdnavi://query?name="
						// + strContent + "&src="
						// + "com.tchip.carlauncher");
						// Intent intent = new Intent(
						// "com.baidu.navi.action.START", uri);
						// startActivity(intent);
						// } catch (Exception e) {
						// }
					}
				}
				break;

			case R.id.layoutRoadCondition:
				openOrCloseRoadCondition();
				break;

			case R.id.layoutStar:
				setStarPannelVisibility(!isStarPannelShow);
				break;

			case R.id.layoutStarNaviWork:
				// 导航到公司
				setStarPannelVisibility(false);

				if (preference.getBoolean("workSet", false)) {
					String workAddress = preference
							.getString("workAddress", "");

					double workLat = Double.parseDouble(preference.getString(
							"workLat", "0.00"));
					double workLng = Double.parseDouble(preference.getString(
							"workLng", "0.00"));

					if (!MyApplication.isNaviAuthSuccess) {
						Log.e(Constant.TAG, "Navigation:Auth Fail");
						Toast.makeText(getApplicationContext(), strAuthFail,
								Toast.LENGTH_SHORT).show();
					} else if (!MyApplication.isNaviInitialSuccess) {
						Log.e(Constant.TAG, "Navigation:Initial Fail");
						Toast.makeText(getApplicationContext(), strInitFail,
								Toast.LENGTH_SHORT).show();
					} else {
						routeplanToNavi(CoordinateType.GCJ02,
								nowLatLng.latitude, nowLatLng.longitude,
								getResources()
										.getString(R.string.location_here),
								workLat, workLng, workAddress);
					}
				} else {
					// 未设置，跳转到设置界面
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(
									R.string.set_location_company),
							Toast.LENGTH_SHORT).show();
					setStarPlace(StarType.TYPE_WORK);
				}
				break;

			case R.id.layoutStarEditWork:
				setStarPlace(StarType.TYPE_WORK);
				break;

			case R.id.layoutStarNaviHome:
				// 导航回家
				setStarPannelVisibility(false);
				if (preference.getBoolean("homeSet", false)) {
					String homeAddress = preference
							.getString("homeAddress", "");

					double homeLat = Double.parseDouble(preference.getString(
							"homeLat", "0.00"));
					double homeLng = Double.parseDouble(preference.getString(
							"homeLng", "0.00"));

					MyApplication.isNaviAuthSuccess = false;
					if (!MyApplication.isNaviAuthSuccess) {
						// TODO:
						Log.e(Constant.TAG, "Navigation:Auth Fail");
						Toast.makeText(getApplicationContext(), strAuthFail,
								Toast.LENGTH_SHORT).show();
						audioRecordDialog.showErrorDialog(strAuthFail);
						new Thread(new dismissDialogThread()).start();

					} else if (!MyApplication.isNaviInitialSuccess) {

						Log.e(Constant.TAG, "Navigation:Initial Fail");
						Toast.makeText(getApplicationContext(), strInitFail,
								Toast.LENGTH_SHORT).show();
						audioRecordDialog.showErrorDialog(strInitFail);
						new Thread(new dismissDialogThread()).start();
					} else {
						routeplanToNavi(CoordinateType.GCJ02,
								nowLatLng.latitude, nowLatLng.longitude,
								getResources()
										.getString(R.string.location_here),
								homeLat, homeLng, homeAddress);
					}
				} else {
					// 未设置，跳转到设置界面
					Toast.makeText(
							getApplicationContext(),
							getResources()
									.getString(R.string.set_location_home),
							Toast.LENGTH_SHORT).show();
					setStarPlace(StarType.TYPE_HOME);
				}
				break;

			case R.id.layoutStarEditHome:
				setStarPlace(StarType.TYPE_HOME);
				break;

			case R.id.layoutOffline:
				Intent intentOffline = new Intent(NavigationActivity.this,
						UpdateMapActivity.class);
				startActivity(intentOffline);
				break;

			case R.id.layoutLocate:
				whereAmI(nowLatLng);
				break;

			case R.id.layoutPagePriv:
				changePage(-1);
				break;

			case R.id.layoutPageNext:
				changePage(1);
				break;

			default:
				break;
			}
		}
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

	enum StarType {
		TYPE_HOME, TYPE_WORK
	}

	private void setStarPlace(StarType type) {
		Intent intent = new Intent(NavigationActivity.this,
				SetStarPlaceActivity.class);
		if (type == StarType.TYPE_HOME) {
			intent.putExtra("starType", 1);
		} else if (type == StarType.TYPE_WORK) {
			intent.putExtra("starType", 0);
		}
		startActivity(intent);
		setStarPannelVisibility(false);
	}

	private void searchNear(String content) {
		isNearLayoutShow = false;
		layoutNearAdvice.setVisibility(View.GONE);

		startSearchPlace(content, nowLatLng, true, false);
	}

	/**
	 * 显示或关闭收藏界面
	 */
	private void setStarPannelVisibility(boolean isShow) {
		if (isShow) {
			setLayoutNearVisibility(false);
			setLayoutHistoryVisibility(false);

			isStarPannelShow = true;
			layoutStarContent.setVisibility(View.VISIBLE);
		} else {
			isStarPannelShow = false;
			layoutStarContent.setVisibility(View.GONE);
		}
	}

	/**
	 * 打开或关闭路况显示
	 */
	private void openOrCloseRoadCondition() {
		if (mBaiduMap.isTrafficEnabled()) {
			mBaiduMap.setTrafficEnabled(false);
			imageRoadCondition.setImageDrawable(getResources().getDrawable(
					R.drawable.main_icon_roadcondition_off));
		} else {
			mBaiduMap.setTrafficEnabled(true);
			imageRoadCondition.setImageDrawable(getResources().getDrawable(
					R.drawable.main_icon_roadcondition_on));
		}
	}

	/**
	 * 显示或隐藏历史记录
	 */
	private void setLayoutHistoryVisibility(boolean isShow) {
		if (isShow) {
			setStarPannelVisibility(false);
			setLayoutNearVisibility(false);

			isHistoryLayoutShow = true;
			layoutHistory.setVisibility(View.VISIBLE);
			// listHistory.setVisibility(View.VISIBLE);
			naviHistoryArray = naviDb.getAllNaviHistory();

			naviHistoryAdapter = new NaviHistoryAdapter(
					getApplicationContext(), naviHistoryArray);
			listHistory.setAdapter(naviHistoryAdapter);

			listHistory
					.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) {
							// 将内容更新到EditText
							String strHistory = naviHistoryArray.get(position)
									.getKey();
							etHistoryWhere.setText(strHistory);

							String strCity = naviHistoryArray.get(position)
									.getCity();
							etHistoryCity.setText(strCity);
							startSearchPlace(strHistory, nowLatLng, false,
									false);
						}
					});

		} else {
			isHistoryLayoutShow = false;
			isResultListShow = false;
			layoutHistory.setVisibility(View.GONE);
			layoutResult.setVisibility(View.GONE);
		}

	}

	/**
	 * 显示或隐藏周边搜索
	 */
	private void setLayoutNearVisibility(boolean isShow) {
		if (isShow) {
			setStarPannelVisibility(false);
			setLayoutHistoryVisibility(false);

			layoutNearAdvice.setVisibility(View.VISIBLE);
			isNearLayoutShow = true;
		} else {
			isNearLayoutShow = false;
			layoutNearAdvice.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onPause() {
		Log.v(Constant.TAG, "NavigationActivity:onPause");
		mMapView.onPause();

		// 销毁定位
		mLocationClient.stop();
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.v(Constant.TAG, "NavigationActivity:onResume");

		mMapView.onResume();

		// LocationMode 跟随：FOLLOWING 普通：NORMAL 罗盘：COMPASS
		com.baidu.mapapi.map.MyLocationConfiguration.LocationMode currentMode = com.baidu.mapapi.map.MyLocationConfiguration.LocationMode.NORMAL;
		mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(
				currentMode, true, null));
		InitLocation(
				com.baidu.location.LocationClientOption.LocationMode.Hight_Accuracy,
				"bd09ll", 3000, true);
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		mPoiSearch.destroy();
		mSuggestionSearch.destroy();

		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void changePage(int set) {
		pageIndex += set;
		if (pageIndex < 0) {
			pageIndex = 0;
		}
		Log.v(Constant.TAG, "Page Index:" + pageIndex);
		startSearchPlace(doingWhere, doingLatLng, doingIsNear, true);
	}

	public void startSearchPlace(String where, LatLng centerLatLng,
			boolean isNear, boolean isPage) {
		doingWhere = where;
		doingLatLng = centerLatLng;
		doingIsNear = isNear;
		Log.v(Constant.TAG, "startSearchPlace:" + where + ",Lat:"
				+ centerLatLng.latitude + ",Lng:" + centerLatLng.longitude
				+ ",isNear:" + isNear);

		if (pageIndex < 1) {
			layoutPagePriv.setVisibility(View.INVISIBLE);
		} else {
			layoutPagePriv.setVisibility(View.VISIBLE);
		}
		if (where != null && where.trim().length() > 0) {
			if (-1 == NetworkUtil.getNetworkType(getApplicationContext())) {
				NetworkUtil.noNetworkHint(getApplicationContext());
			} else {
				String textCity = etHistoryCity.getText().toString();
				boolean isInputCity = textCity != null
						&& textCity.trim().length() > 0;
				if (isNear) {
					// 周边搜索
					if (!isPage) {
						Toast.makeText(
								getApplicationContext(),
								getResources().getString(
										R.string.poi_search_near)
										+ where, Toast.LENGTH_SHORT).show();
					}

					PoiNearbySearchOption poiOption = new PoiNearbySearchOption();
					poiOption.keyword(where);
					poiOption.location(centerLatLng);
					poiOption.radius(15 * 1000 * 1000); // 检索半径，单位:m
					poiOption.sortType(PoiSortType.distance_from_near_to_far); // 按距离排序
					// poiOption.sortType(PoiSortType.comprehensive); // 按综合排序
					poiOption.pageNum(pageIndex); // 分页编号
					poiOption.pageCapacity(pageCapacity);
					try {
						mPoiSearch.searchNearby(poiOption);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					if (!isInputCity) {
						// 周边搜索
						if (!isPage) {
							Toast.makeText(
									getApplicationContext(),
									getResources().getString(
											R.string.poi_search_near)
											+ where, Toast.LENGTH_SHORT).show();
						}

						PoiNearbySearchOption poiOption = new PoiNearbySearchOption();
						poiOption.keyword(where);
						poiOption.location(centerLatLng);
						poiOption.radius(15 * 1000 * 1000); // 检索半径，单位:m
						poiOption
								.sortType(PoiSortType.distance_from_near_to_far); // 按距离排序
						// poiOption.sortType(PoiSortType.comprehensive); //
						// 按综合排序
						poiOption.pageNum(pageIndex); // 分页编号
						poiOption.pageCapacity(pageCapacity);
						try {
							mPoiSearch.searchNearby(poiOption);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// 全国搜索
						if (!isPage) {
							Toast.makeText(
									getApplicationContext(),
									getResources().getString(
											R.string.poi_in_city)
											+ textCity
											+ getResources().getString(
													R.string.poi_search)
											+ where, Toast.LENGTH_SHORT).show();
						}

						PoiCitySearchOption poiOption = new PoiCitySearchOption();
						poiOption.city(textCity);
						poiOption.keyword(where);
						poiOption.pageNum(pageIndex);
						poiOption.pageCapacity(pageCapacity);
						mPoiSearch.searchInCity(poiOption);
					}

					// 存储搜索历史到数据库,周边不需要
					int existId = naviDb.getNaviIdByKey(where);
					if (existId != -1) {
						naviDb.deleteNaviHistoryById(existId);
					}
					NaviHistory naviHistory = new NaviHistory(where, textCity);
					naviDb.addNaviHistory(naviHistory);
					naviHistoryAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	class MyOnGetSuggestionResultListener implements
			OnGetSuggestionResultListener {

		@Override
		public void onGetSuggestionResult(SuggestionResult result) {
			if (result == null || result.getAllSuggestions() == null) {
				return;
			}
			sugAdapter.clear();
			// for (SuggestionResult.SuggestionInfo info :
			// res.getAllSuggestions()) {
			// if (info.key != null){
			// sugAdapter.add(info.key);

			naviArray = new ArrayList<NaviResultInfo>();
			Log.v(Constant.TAG, "result.getAllSuggestions().size():"
					+ result.getAllSuggestions().size());
			for (int i = 1; i < result.getAllSuggestions().size(); i++) {
				// PoiInfo poiInfo = result.getAllPoi().get(i);
				//
				SuggestionInfo info = result.getAllSuggestions().get(i);
				double distance = DistanceUtil.getDistance(nowLatLng, info.pt);

				NaviResultInfo naviResultInfo = new NaviResultInfo(i, info.key,
						info.city + info.district, info.pt.longitude,
						info.pt.latitude, distance);
				naviArray.add(naviResultInfo);
			}

			naviResultAdapter = new NaviResultAdapter(getApplicationContext(),
					naviArray);

			layoutResult.setVisibility(View.VISIBLE);
			isResultListShow = true;
			listResult.setAdapter(naviResultAdapter);
			// }
			// sugAdapter.notifyDataSetChanged();
		}

	}

	/**
	 * 地址转经纬度监听
	 */
	class MyOnGetGeoCoderResultListener implements OnGetGeoCoderResultListener {

		@Override
		public void onGetGeoCodeResult(GeoCodeResult result) {
			LatLng thisLatLng = result.getLocation();
			if (thisLatLng != null) {
				startSearchPlace(etHistoryWhere.getText().toString(),
						thisLatLng, false, false);
			}
		}

		@Override
		public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
		}
	}

	public void onGetPoiResult(PoiResult result) {
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			Toast.makeText(NavigationActivity.this,
					getResources().getString(R.string.poi_no_result),
					Toast.LENGTH_LONG).show();
			pageIndex--;
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			mBaiduMap.clear();
			PoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
			mBaiduMap.setOnMarkerClickListener(overlay);
			overlay.setData(result);
			overlay.addToMap();
			overlay.zoomToSpan();
			// 添加结果
			LatLng llStart = nowLatLng; // 当前位置
			naviArray = new ArrayList<NaviResultInfo>();
			int arraySize = result.getAllPoi().size();
			if (arraySize < pageCapacity) {
				layoutPageNext.setVisibility(View.INVISIBLE);
			} else {
				layoutPageNext.setVisibility(View.VISIBLE);
			}
			Log.v(Constant.TAG, "result.getAllPoi().size():" + arraySize);
			for (int i = 1; i < arraySize; i++) {
				PoiInfo poiInfo = result.getAllPoi().get(i);

				double distance = DistanceUtil.getDistance(llStart,
						poiInfo.location);

				NaviResultInfo naviResultInfo = new NaviResultInfo(i,
						poiInfo.name, poiInfo.address,
						poiInfo.location.longitude, poiInfo.location.latitude,
						distance);
				naviArray.add(naviResultInfo);
			}

			naviResultAdapter = new NaviResultAdapter(getApplicationContext(),
					naviArray);
			layoutResult.setVisibility(View.VISIBLE);
			// setLayoutHistoryVisibility(false);
			// layoutHistory.setVisibility(View.GONE);
			// isHistoryLayoutShow = false;
			isResultListShow = true;
			listResult.setAdapter(naviResultAdapter);

			listResult
					.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						public void onItemClick(
								android.widget.AdapterView<?> parent,
								android.view.View view, int position, long id) {
							// 开始导航
							layoutResult.setVisibility(View.GONE);
							pageIndex = 0; // 重设搜索结果页码为0
							isResultListShow = false;
							setLayoutHistoryVisibility(false);

							if (!MyApplication.isNaviAuthSuccess) {
								Log.e(Constant.TAG, "Navigation:Auth Fail");
								Toast.makeText(getApplicationContext(),
										strAuthFail, Toast.LENGTH_SHORT).show();
							} else if (!MyApplication.isNaviInitialSuccess) {
								Log.e(Constant.TAG, "Navigation:Initial Fail");
								Toast.makeText(getApplicationContext(),
										strInitFail, Toast.LENGTH_SHORT).show();
							} else {
								routeplanToNavi(
										CoordinateType.GCJ02,
										nowLatLng.latitude,
										nowLatLng.longitude,
										getResources().getString(
												R.string.location_here),
										naviArray.get(position).getLatitude(),
										naviArray.get(position).getLongitude(),
										naviArray.get(position).getName());

								audioRecordDialog.showLoadDialog();
							}
						}
					});

			return;
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

			// // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
			// String strInfo = "在";
			// for (CityInfo cityInfo : result.getSuggestCityList()) {
			// strInfo += cityInfo.city;
			// strInfo += ",";
			// }
			// strInfo += "找到结果";
			// Toast.makeText(NavigationActivity.this, strInfo,
			// Toast.LENGTH_LONG)
			// .show();
			//

		}
	}

	private void routeplanToNavi(CoordinateType coType, double startLatitude,
			double startLongitude, String startName, double endLatitude,
			double endLongitude, String endName) {
		BNRoutePlanNode sNode = null;
		BNRoutePlanNode eNode = null;
		// 需要将bd09ll转成BD09_MC,GCJ02,WGS84
		BDLocation bdLocStartBefore = new BDLocation();
		bdLocStartBefore.setLatitude(startLatitude);
		bdLocStartBefore.setLongitude(startLongitude);
		BDLocation bdLocStartAfter = LocationClient.getBDLocationInCoorType(
				bdLocStartBefore, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);

		BDLocation bdLocEndBefore = new BDLocation();
		bdLocEndBefore.setLatitude(endLatitude);
		bdLocEndBefore.setLongitude(endLongitude);
		BDLocation bdLocEndAfter = LocationClient.getBDLocationInCoorType(
				bdLocEndBefore, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);

		sNode = new BNRoutePlanNode(bdLocStartAfter.getLongitude(),
				bdLocStartAfter.getLatitude(), startName, null,
				CoordinateType.GCJ02);
		eNode = new BNRoutePlanNode(bdLocEndAfter.getLongitude(),
				bdLocEndAfter.getLatitude(), endName, null,
				CoordinateType.GCJ02);

		if (sNode != null && eNode != null) {
			List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
			list.add(sNode);
			list.add(eNode);
			BaiduNaviManager.getInstance().launchNavigator(this, list, 1, true,
					new DemoRoutePlanListener(sNode));
		}
	}

	public class DemoRoutePlanListener implements RoutePlanListener {

		private BNRoutePlanNode mBNRoutePlanNode = null;

		public DemoRoutePlanListener(BNRoutePlanNode node) {
			mBNRoutePlanNode = node;
		}

		@Override
		public void onJumpToNavigator() {
			audioRecordDialog.dismissDialog();
			Intent intent = new Intent(NavigationActivity.this,
					BNavigatorActivity.class);
			Bundle bundle = new Bundle();
			bundle.putSerializable(MainActivity.ROUTE_PLAN_NODE,
					(BNRoutePlanNode) mBNRoutePlanNode);
			intent.putExtras(bundle);
			startActivity(intent);
			Log.v(Constant.TAG, "RoutePlanListener:onJumpToNavigator");
		}

		@Override
		public void onRoutePlanFailed() {
			audioRecordDialog.dismissDialog();
			Toast.makeText(getApplicationContext(), "导航路线规划失败",
					Toast.LENGTH_SHORT).show();
			Log.e(Constant.TAG, "Baidu Navi:Route Plan Failed!");
		}
	}

	private BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {

		@Override
		public void stopTTS() {
			// TODO Auto-generated method stub

		}

		@Override
		public void resumeTTS() {
			// TODO Auto-generated method stub

		}

		@Override
		public void releaseTTSPlayer() {

		}

		@Override
		public int playTTSText(String speech, int bPreempt) {
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
			return 0;
		}
	};

	public void onGetPoiDetailResult(PoiDetailResult result) {
		if (result.error != SearchResult.ERRORNO.NO_ERROR) {
			Toast.makeText(NavigationActivity.this,
					getResources().getString(R.string.poi_no_result),
					Toast.LENGTH_SHORT).show();
		} else {
			// 点击地图上搜索结果气球进入导航

			if (!MyApplication.isNaviAuthSuccess) {
				Log.e(Constant.TAG, "Navigation:Auth Fail");
				Toast.makeText(getApplicationContext(), strAuthFail,
						Toast.LENGTH_SHORT).show();
			} else if (!MyApplication.isNaviInitialSuccess) {
				Log.e(Constant.TAG, "Navigation:Initial Fail");
				Toast.makeText(getApplicationContext(), strInitFail,
						Toast.LENGTH_SHORT).show();
			} else {
				routeplanToNavi(CoordinateType.GCJ02, nowLatLng.latitude,
						nowLatLng.longitude,
						getResources().getString(R.string.poi_no_result),
						result.getLocation().latitude,
						result.getLocation().longitude, result.getAddress());
			}
		}
	}

	@Override
	public void onGetSuggestionResult(SuggestionResult res) {
		if (res == null || res.getAllSuggestions() == null) {
			return;
		}
		sugAdapter.clear();
		for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
			if (info.key != null)
				sugAdapter.add(info.key);
		}
		sugAdapter.notifyDataSetChanged();
	}

	private class MyPoiOverlay extends PoiOverlay {

		public MyPoiOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}

		@Override
		public boolean onPoiClick(int index) {
			super.onPoiClick(index);
			PoiInfo poi = getPoiResult().getAllPoi().get(index);
			// if (poi.hasCaterDetails) {
			mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
					.poiUid(poi.uid));
			// }
			return true;
		}
	}

	// @Override
	// protected void onResumeFragments() {
	// super.onResumeFragments();
	// View decorView = getWindow().getDecorView();
	// decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
	// }

	// ==========================
	int ret = 0;// 函数调用返回值

	public void startVoiceUnderstand() {
		if (-1 == NetworkUtil.getNetworkType(getApplicationContext())) {
			NetworkUtil.noNetworkHint(getApplicationContext());
		} else {
			// 初始化对象
			mSpeechUnderstander = SpeechUnderstander.createUnderstander(
					NavigationActivity.this, speechUnderstanderListener);
			setParam();

			if (mSpeechUnderstander.isUnderstanding()) { // 开始前检查状态
				mSpeechUnderstander.stopUnderstanding(); // 停止录音
			} else {
				ret = mSpeechUnderstander
						.startUnderstanding(mRecognizerListener);
				if (ret != 0) {
					// 语义理解失败,错误码:ret
				} else {
					// showTip(getString(R.string.text_begin));
				}
			}
		}
	}

	/**
	 * 初始化监听器（语音到语义）。
	 */
	private InitListener speechUnderstanderListener = new InitListener() {
		@Override
		public void onInit(int code) {
			if (code != ErrorCode.SUCCESS) {
				// 初始化失败,错误码：code
			}
		}
	};

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setParam() {
		String lag = preference.getString("voiceAccent", "mandarin");
		if (lag.equals("en_us")) {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mSpeechUnderstander.setParameter(SpeechConstant.ACCENT, lag);
		}
		// 设置语音前端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS,
				preference.getString("voiceBos", "4000"));
		// 设置语音后端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS,
				preference.getString("voiceEos", "1000"));
		// 设置是否有标点符号
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT,
				preference.getString("understander_punc_preference", "0"));
		// 语音输入超时时间,单位：ms，默认60000
		// mSpeechUnderstander.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT,"");

		// 识别句子级多候选结果，如asr_nbest=3,注：设置多候选会影响性能，响应时间延迟200ms左右
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_NBEST, "1");

		// 网络连接超时时间,单位：ms，默认20000
		mSpeechUnderstander.setParameter(SpeechConstant.NET_TIMEOUT, "5000");
		// 设置音频保存路径
		mSpeechUnderstander.setParameter(
				SpeechConstant.ASR_AUDIO_PATH,
				preference.getString("voicePath",
						Environment.getExternalStorageDirectory()
								+ "/iflytek/wavaudio.pcm"));
	}

	/**
	 * 设置目的地EditText的内容
	 */
	private void setDestinationText(String text) {
		if (!TextUtils.isEmpty(text)) {
			progressVoice.setVisibility(View.GONE);
			imgVoiceSearch.setVisibility(View.VISIBLE);
			String[] naviIntent = getResources().getStringArray(
					R.array.navi_intent);
			if (text.startsWith(naviIntent[0])
					|| text.startsWith(naviIntent[1])
					|| text.startsWith(naviIntent[2])) {
				text = text.substring(3, text.length());
			} else if (text.startsWith(naviIntent[3])) {
				text = text.substring(2, text.length());
			} else if (text.startsWith(naviIntent[4])
					|| text.startsWith(naviIntent[5])) {
				text = text.substring(1, text.length());
			}

			etHistoryWhere.setText(text);
			startSearchPlace(text, nowLatLng, false, false);
		}
	}

	/**
	 * 识别回调。
	 */
	private SpeechUnderstanderListener mRecognizerListener = new SpeechUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (null != result) {
						// 显示
						String text = result.getResultString();

						try {
							JSONObject jsonObject;
							jsonObject = new JSONObject(text);
							String strContent = jsonObject.getString("text");
							setDestinationText(strContent);
						} catch (JSONException e) {

						}
					} else {
						// 识别结果不正确
						progressVoice.setVisibility(View.GONE);
						imgVoiceSearch.setVisibility(View.VISIBLE);
					}
				}
			});
		}

		@Override
		public void onVolumeChanged(int v) {
			Log.e(Constant.TAG, "VOLUME:" + v);
			audioRecordDialog.updateVolumeLevel(v);

		}

		@Override
		public void onEndOfSpeech() {
			// showTip("onEndOfSpeech");
			audioRecordDialog.dismissDialog();

			// 开始识别
			imgVoiceSearch.setVisibility(View.GONE);
			progressVoice.setVisibility(View.VISIBLE);
		}

		@Override
		public void onBeginOfSpeech() {
			// showTip("onBeginOfSpeech");
			audioRecordDialog.showVoiceDialog();
		}

		@Override
		public void onError(SpeechError error) {
			// showTip("onError Code：" + error.getErrorCode());
			startSpeak(error.getErrorDescription());

			imgVoiceSearch.setVisibility(View.VISIBLE);
			progressVoice.setVisibility(View.GONE);
			Log.e(Constant.TAG, "SpeechError:" + error.getErrorCode());
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

		}
	};

	private void startSpeak(String content) {
		Intent intent = new Intent(NavigationActivity.this, SpeakService.class);
		intent.putExtra("content", content);
		startService(intent);
	}

}
