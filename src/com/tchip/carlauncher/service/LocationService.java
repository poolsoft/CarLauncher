package com.tchip.carlauncher.service;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.tchip.carlauncher.Constant;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;

public class LocationService extends Service {
	private LocationClient mLocationClient;

	private int scanSpan = 1000; // 采集轨迹点间隔(ms)
	private String cityName = "未定位";
	// private long cityCode = 123456789;
	private SharedPreferences preferences;
	private Editor editor;

	// private final String WEATHER_PREFIX = "{\"weatherinfo\"";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		InitLocation(LocationMode.Hight_Accuracy, "bd09ll", scanSpan, true);

		preferences = getSharedPreferences(Constant.SHARED_PREFERENCES_NAME,
				Context.MODE_PRIVATE);
		editor = preferences.edit();
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
	private void InitLocation(LocationMode tempMode, String tempCoor,
			int frequence, boolean isNeedAddress) {

		mLocationClient = new LocationClient(this.getApplicationContext());
		mLocationClient.registerLocationListener(new MyLocationListener());
		// mGeofenceClient = new GeofenceClient(getApplicationContext());

		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(tempMode);
		option.setCoorType(tempCoor);
		option.setScanSpan(frequence);
		option.setIsNeedAddress(isNeedAddress);
		mLocationClient.setLocOption(option);

		mLocationClient.start();
	}

	class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {

			cityName = location.getCity();

			if ((cityName != null) && (!cityName.equals("未定位"))) {

				editor.putString("cityName", cityName);
				editor.putString("cityNameRealButOld", cityName);
				editor.putString("latitude", "" + location.getLatitude());
				editor.putString("longitude", "" + location.getLongitude());
				editor.putString("district", location.getDistrict());
				editor.putString("floor", location.getFloor());
				editor.putString("addrStr", location.getAddrStr());
				editor.putString("street", location.getStreet());
				editor.putString("streetNum", location.getStreetNumber());
				editor.putFloat("speed", location.getSpeed());
				editor.putString("altitude", "" + location.getAltitude());
				editor.putString("lbsTime", location.getTime());
				editor.commit();

			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationClient.stop();
	}

}