package com.tchip.carlauncher.ui.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BaiduMap.SnapshotReadyCallback;
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.adapter.RouteAdapter;
import com.tchip.carlauncher.model.RouteDistance;
import com.tchip.carlauncher.model.RouteDistanceDbHelper;
import com.tchip.carlauncher.model.RoutePoint;
import com.tchip.carlauncher.model.Typefaces;

public class RouteShowActivity extends Activity {
	private MapView mMapView;
	private BaiduMap mBaiduMap;
	private InfoWindow mInfoWindow;
	private Marker mMarkerStart;
	private Marker mMarkerEnd;
	private Button btnShare, btnToRouteListFromShow;
	private TextView textDistance;

	public double mRouteLatitude = 0.0;
	public double mRouteLongitude = 0.0;

	private final String ROUTE_PATH = Constant.Path.ROUTE_TRACK;
	private String filePath = "";
	private RouteAdapter routeAdapter = new RouteAdapter();

	private RouteDistanceDbHelper _db;
	private SharedPreferences sharedPreferences;

	// 初始化全局 bitmap 信息，不用时及时 recycle
	BitmapDescriptor iconStart = BitmapDescriptorFactory
			.fromResource(R.drawable.icon_st);
	BitmapDescriptor iconEnd = BitmapDescriptorFactory
			.fromResource(R.drawable.icon_en);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_route_show);

		_db = new RouteDistanceDbHelper(getApplicationContext());
		sharedPreferences = getSharedPreferences(
				Constant.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

		btnShare = (Button) findViewById(R.id.btnShare);
		btnShare.setOnClickListener(new MyOnClickListener());

		btnToRouteListFromShow = (Button) findViewById(R.id.btnToRouteListFromShow);
		btnToRouteListFromShow.setOnClickListener(new MyOnClickListener());

		textDistance = (TextView) findViewById(R.id.textDistance);
		// TextColor
		// btnDistance.setBackgroundColor(Color.parseColor("#ffffff"));

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			filePath = extras.getString("filePath");
			// setTitle(filePath.substring(0, filePath.length() - 4));
		} else {
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.no_route_file),
					Toast.LENGTH_SHORT).show();
			finish();
		}

		mMapView = (MapView) findViewById(R.id.routeMap);

		// 去掉缩放控件和百度Logo
		int count = mMapView.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = mMapView.getChildAt(i);
			if (child instanceof ImageView || child instanceof ZoomControls) {
				child.setVisibility(View.INVISIBLE);
			}
		}
		mBaiduMap = mMapView.getMap();
		mBaiduMap.setOnMarkerClickListener(new MyOnMarkerClickListener());
		addRouteToMap(filePath);

	}

	class MyOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnShare:
				mBaiduMap.snapshot(new SnapshotReadyCallback() {
					public void onSnapshotReady(Bitmap snapshot) {
						File file = new File(ROUTE_PATH + filePath + ".png");
						FileOutputStream out;
						try {
							out = new FileOutputStream(file);
							if (snapshot.compress(Bitmap.CompressFormat.PNG,
									100, out)) {
								out.flush();
								out.close();
							}
							Intent intent = new Intent(Intent.ACTION_SEND);
							Uri uri = Uri.fromFile(file);
							intent.setType("image/png");
							intent.putExtra(Intent.EXTRA_STREAM, uri);
							intent.putExtra(
									Intent.EXTRA_TITLE,
									getResources().getString(
											R.string.share_route_screenshot_to));
							Intent chooserIntent = Intent.createChooser(
									intent,
									getResources().getString(
											R.string.share_route));
							if (chooserIntent == null) {
								return;
							}
							try {
								startActivity(chooserIntent);
							} catch (android.content.ActivityNotFoundException ex) {
							}
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				break;
			case R.id.btnToRouteListFromShow:
				backToRouteList();
				break;
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			backToRouteList();
			return true;
		} else
			return super.onKeyDown(keyCode, event);
	}

	private void backToRouteList() {
		finish();
		overridePendingTransition(R.anim.zms_translate_right_out,
				R.anim.zms_translate_right_in);
	}

	/**
	 * 从文件读取经纬度表单
	 * 
	 * @param fileName
	 * @return
	 */
	public List<RoutePoint> readFileSdcard(String fileName) {
		String res = "";
		try {
			FileInputStream fin = new FileInputStream(fileName);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			res = EncodingUtils.getString(buffer, "UTF-8");
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (res.equals("")) {
			res = " ";
		}
		return routeAdapter.getJsonString(res);
	}

	/**
	 * 添加轨迹到地图
	 * 
	 * @param path
	 */
	public void addRouteToMap(String path) {
		// 初始化轨迹点
		List<LatLng> points = getRoutePoints(path);

		if (points.size() < 2) {
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.route_point_less),
					Toast.LENGTH_SHORT).show();
			finish();
		} else {
			// 绘制轨迹
			OverlayOptions ooPolyline = new PolylineOptions().width(5)
					.color(0xAA0000FF).points(points);
			mBaiduMap.addOverlay(ooPolyline);

			// 定位地图到轨迹起点位置
			MapStatusUpdate u1 = MapStatusUpdateFactory
					.newLatLng(points.get(1));
			mBaiduMap.setMapStatus(u1);

			MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(getZoomLevel());
			mBaiduMap.animateMapStatus(msu);

			// 绘制起始点Marker
			LatLng llStart = points.get(0);
			LatLng llEnd = points.get(points.size() - 1);
			OverlayOptions ooStart = new MarkerOptions().position(llStart)
					.icon(iconStart).zIndex(9).draggable(true);
			mMarkerStart = (Marker) (mBaiduMap.addOverlay(ooStart));
			OverlayOptions ooEnd = new MarkerOptions().position(llEnd)
					.icon(iconEnd).zIndex(9).draggable(true);
			mMarkerEnd = (Marker) (mBaiduMap.addOverlay(ooEnd));

			double linearDistance = 0.0; // 直线距离
			double driveDistance = 0.0; // 轨迹距离

			try { // 轨迹距离信息已保存，直接读取数据库
				RouteDistance routeDistance = _db
						.getRouteDistanceByName(filePath);
				linearDistance = routeDistance.getLinear();
				driveDistance = routeDistance.getDrive();

			} catch (Exception e) {
				linearDistance = DistanceUtil.getDistance(llStart, llEnd);
				for (int i = 0; i < points.size() - 1; i++) {
					driveDistance = driveDistance
							+ DistanceUtil.getDistance(points.get(i),
									points.get(i + 1));
				}
				RouteDistance newRouteDistance = new RouteDistance(filePath,
						linearDistance, driveDistance);
				_db.addRouteDistance(newRouteDistance); // 保存轨迹距离信息到数据库
			} finally {
				textDistance.setVisibility(View.VISIBLE);
				textDistance.setText(getResources().getString(
						R.string.linear_diatance)
						+ ":"
						+ (int) linearDistance
						+ getResources().getString(R.string.meter)
						+ getResources().getString(R.string.drive_distance)
						+ (int) driveDistance
						+ getResources().getString(R.string.meter));
				textDistance.setTypeface(Typefaces.get(this, Constant.Path.FONT
						+ "Font-Helvetica-Neue-LT-Pro.otf"));
				_db.close();
			}
		}
	}

	public List<LatLng> getRoutePoints(String fileName) {

		List<RoutePoint> list = readFileSdcard(ROUTE_PATH + fileName);
		if (sharedPreferences.getBoolean("routeSmooth", true)) {
			list = optimizeRoutePoints(list); // 优化轨迹平滑度
		}
		List<LatLng> points = new ArrayList<LatLng>(list.size());
		for (int i = 0; i < list.size(); i++) {
			i = i + getOffset() - 1;
			if (i < list.size()) {
				points.add(new LatLng(list.get(i).getLat(), list.get(i)
						.getLng()));
			} else {
				// 越界添加最后一个轨迹点
				points.add(new LatLng(list.get(list.size() - 1).getLat(), list
						.get(list.size() - 1).getLng()));
			}
		}
		return points;
	}

	/**
	 * 轨迹点经纬度优化，使连线平滑
	 * 
	 * @param inPoint
	 * @return
	 */
	public List<RoutePoint> optimizeRoutePoints(List<RoutePoint> inPoint) {
		int size = inPoint.size();
		int i;
		if (size < 5) {
			return inPoint;
		} else {
			// 经度优化
			inPoint.get(0)
					.setLat((3.0 * inPoint.get(0).getLat() + 2.0
							* inPoint.get(1).getLat() + inPoint.get(2).getLat() - inPoint
							.get(4).getLat()) / 5.0);
			inPoint.get(1)
					.setLat((4.0 * inPoint.get(0).getLat() + 3.0
							* inPoint.get(1).getLat() + 2
							* inPoint.get(2).getLat() + inPoint.get(3).getLat()) / 10.0);

			for (i = 2; i <= size - 3; i++) {
				inPoint.get(i).setLat(
						(inPoint.get(i - 2).getLat()
								+ inPoint.get(i - 1).getLat()
								+ inPoint.get(i).getLat()
								+ inPoint.get(i + 1).getLat() + inPoint.get(
								i + 2).getLat()) / 5.0);
			}
			inPoint.get(size - 2).setLat(
					(4.0 * inPoint.get(size - 1).getLat() + 3.0
							* inPoint.get(size - 2).getLat() + 2
							* inPoint.get(size - 3).getLat() + inPoint.get(
							size - 4).getLat()) / 10.0);
			inPoint.get(size - 1).setLat(
					(3.0 * inPoint.get(size - 1).getLat() + 2.0
							* inPoint.get(size - 2).getLat()
							+ inPoint.get(size - 3).getLat() - inPoint.get(
							size - 5).getLat()) / 5.0);

			// 纬度优化
			inPoint.get(0)
					.setLng((3.0 * inPoint.get(0).getLng() + 2.0
							* inPoint.get(1).getLng() + inPoint.get(2).getLng() - inPoint
							.get(4).getLng()) / 5.0);
			inPoint.get(1)
					.setLng((4.0 * inPoint.get(0).getLng() + 3.0
							* inPoint.get(1).getLng() + 2
							* inPoint.get(2).getLng() + inPoint.get(3).getLng()) / 10.0);

			for (i = 2; i <= size - 3; i++) {
				inPoint.get(i).setLng(
						(inPoint.get(i - 2).getLng()
								+ inPoint.get(i - 1).getLng()
								+ inPoint.get(i).getLng()
								+ inPoint.get(i + 1).getLng() + inPoint.get(
								i + 2).getLng()) / 5.0);
			}
			inPoint.get(size - 2).setLng(
					(4.0 * inPoint.get(size - 1).getLng() + 3.0
							* inPoint.get(size - 2).getLng() + 2
							* inPoint.get(size - 3).getLng() + inPoint.get(
							size - 4).getLng()) / 10.0);
			inPoint.get(size - 1).setLng(
					(3.0 * inPoint.get(size - 1).getLng() + 2.0
							* inPoint.get(size - 2).getLng()
							+ inPoint.get(size - 3).getLng() - inPoint.get(
							size - 5).getLng()) / 5.0);
		}
		return inPoint;
	}

	class MyOnMarkerClickListener implements OnMarkerClickListener {
		public boolean onMarkerClick(final Marker marker) {
			Button button = new Button(getApplicationContext());
			button.setBackgroundResource(R.drawable.popup);
			OnInfoWindowClickListener listener = null;
			if (marker == mMarkerStart) {
				button.setText(getResources()
						.getString(R.string.location_start));
				listener = new OnInfoWindowClickListener() {
					public void onInfoWindowClick() {
						// LatLng ll = marker.getPosition();
						// LatLng llNew = new LatLng(ll.latitude + 0.005,
						// ll.longitude + 0.005);
						// marker.setPosition(llNew);
						// mBaiduMap.hideInfoWindow();
					}
				};
				LatLng ll = marker.getPosition();
				mInfoWindow = new InfoWindow(
						BitmapDescriptorFactory.fromView(button), ll, -47,
						listener);
				mBaiduMap.showInfoWindow(mInfoWindow);
			} else if (marker == mMarkerEnd) {
				button.setText(getResources()
						.getString(R.string.drive_distance));
			}
			return true;
		}
	}

	/**
	 * 读取设置的缩放倍数
	 * 
	 * @return
	 */
	public float getZoomLevel() {

		return sharedPreferences.getFloat("zoomLevel", 19f);
	}

	/**
	 * 读取轨迹点采样偏移量
	 * 
	 * @return
	 */
	public int getOffset() {
		String offsetStr = sharedPreferences.getString("routeSpan", "HIGH");
		if ("LOW".equals(offsetStr))
			return Constant.BaiduMap.ROUTE_POINT_OFFSET_LOW;
		else if ("MIDDLE".equals(offsetStr))
			return Constant.BaiduMap.ROUTE_POINT_OFFSET_MIDDLE;
		else
			return Constant.BaiduMap.ROUTE_POINT_OFFSET_HIGH;
	}

	@Override
	protected void onPause() {
		// MapView的生命周期与Activity同步，当activity挂起时需调用MapView.onPause()
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		// MapView的生命周期与Activity同步，当activity恢复时需调用MapView.onResume()
		mMapView.onResume();
		super.onResume();
		// Hide Status Bar
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	@Override
	protected void onDestroy() {
		// MapView的生命周期与Activity同步，当activity销毁时需调用MapView.destroy()
		mMapView.onDestroy();
		super.onDestroy();
		// 回收 bitmap 资源
		iconStart.recycle();
		iconEnd.recycle();
	}

}
