package com.tchip.carlauncher.ui.manager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.model.MusicAlbumInfo;
import com.tchip.carlauncher.model.MusicSPStorage;
import com.tchip.carlauncher.util.MusicUtils;

/**
 * 专辑列表
 */
public class MusicAlbumBrowserManager extends MusicMainUIManager implements
		Constant, OnClickListener, OnItemClickListener {

	private Activity mActivity;
	private MusicUIManager mUIManager;
	private LayoutInflater mInflater;

	private ListView mListView;
	private ImageButton mBackBtn;
	private List<MusicAlbumInfo> mAlbumList = new ArrayList<MusicAlbumInfo>();
	private MyAdapter mAdapter;

	private LinearLayout mAlbumLayout;

	public MusicAlbumBrowserManager(Activity activity, MusicUIManager manager) {
		this.mActivity = activity;
		this.mUIManager = manager;
		this.mInflater = LayoutInflater.from(activity);
	}

	public View getView() {
		View view = mInflater.inflate(R.layout.music_albumbrower, null);
		initBg(view);
		initView(view);
		return view;
	}

	private void initView(View view) {
		mListView = (ListView) view.findViewById(R.id.album_listview);
		mListView.setOnItemClickListener(this);
		mBackBtn = (ImageButton) view.findViewById(R.id.backBtn);
		mBackBtn.setOnClickListener(this);

		mAlbumList = MusicUtils.queryAlbums(mActivity);
		mAdapter = new MyAdapter();
		mListView.setAdapter(mAdapter);

	}

	private void initBg(View view) {
		mAlbumLayout = (LinearLayout) view.findViewById(R.id.main_album_layout);
		MusicSPStorage mSp = new MusicSPStorage(mActivity);
		String mDefaultBgPath = mSp.getPath();
		Bitmap bitmap = mUIManager.getBitmapByPath(mDefaultBgPath);
		if (bitmap != null) {
			mAlbumLayout.setBackgroundDrawable(new BitmapDrawable(mActivity
					.getResources(), bitmap));
		}
	}

	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAlbumList.size();
		}

		@Override
		public MusicAlbumInfo getItem(int position) {
			return mAlbumList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			MusicAlbumInfo album = getItem(position);

			ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = mActivity.getLayoutInflater().inflate(
						R.layout.music_albumbrower_listitem, null);
				viewHolder.albumNameTv = (TextView) convertView
						.findViewById(R.id.album_name_tv);
				viewHolder.numberTv = (TextView) convertView
						.findViewById(R.id.number_of_songs_tv);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			viewHolder.albumNameTv.setText(album.album_name);
			viewHolder.numberTv.setText(album.number_of_songs + "首歌");

			return convertView;
		}

		private class ViewHolder {
			TextView albumNameTv, numberTv;
		}

	}

	@Override
	public void onClick(View v) {
		if (v == mBackBtn) {
			mUIManager.setCurrentItem();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		mUIManager.setContentType(Constant.Music.ALBUM_TO_MYMUSIC,
				mAdapter.getItem(position));
	}

	@Override
	protected void setBgByPath(String path) {
		Bitmap bitmap = mUIManager.getBitmapByPath(path);
		if (bitmap != null) {
			mAlbumLayout.setBackgroundDrawable(new BitmapDrawable(mActivity
					.getResources(), bitmap));
		}
	}

	@Override
	public View getView(int from) {
		return null;
	}

	@Override
	public View getView(int from, Object obj) {
		return null;
	}

}
