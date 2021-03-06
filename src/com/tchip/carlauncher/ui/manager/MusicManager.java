package com.tchip.carlauncher.ui.manager;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.MyApplication;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.adapter.MusicMyAdapter;
import com.tchip.carlauncher.model.MusicAlbumInfo;
import com.tchip.carlauncher.model.MusicArtistInfo;
import com.tchip.carlauncher.model.MusicFolderInfo;
import com.tchip.carlauncher.model.MusicInfo;
import com.tchip.carlauncher.model.MusicSPStorage;
import com.tchip.carlauncher.service.MusicServiceManager;
import com.tchip.carlauncher.util.MusicTimer;
import com.tchip.carlauncher.util.MusicUtils;

/**
 * 我的音乐
 */
public class MusicManager extends MusicMainUIManager implements Constant,
		OnTouchListener {

	private LayoutInflater mInflater;
	private Activity mActivity;

	private String TAG = MusicManager.class.getSimpleName();
	private MusicMyAdapter mAdapter;
	private ListView mListView;
	private MusicServiceManager mServiceManager = null;
	private MusicSlidingDrawerManager mSdm;
	private MusicMyUIManager mUIm;
	private MusicTimer mMusicTimer;
	private MusicPlayBroadcast mPlayBroadcast;

	private int mFrom;
	private Object mObj;

	private RelativeLayout mBottomLayout, mMainLayout;
	private Bitmap defaultArtwork;

	private MusicUIManager mUIManager;

	public MusicManager(Activity activity, MusicUIManager manager) {
		this.mActivity = activity;
		mInflater = LayoutInflater.from(activity);
		this.mUIManager = manager;
	}

	public View getView(int from) {
		return getView(from, null);
	}

	public View getView(int from, Object object) {
		View contentView = mInflater.inflate(R.layout.music_mymusic, null);
		mFrom = from;
		mObj = object;
		initBg(contentView);
		initView(contentView);

		return contentView;
	}

	private void initView(View view) {
		defaultArtwork = BitmapFactory.decodeResource(mActivity.getResources(),
				R.drawable.img_album_background);
		mServiceManager = MyApplication.mServiceManager;

		mBottomLayout = (RelativeLayout) view.findViewById(R.id.bottomLayout);

		mListView = (ListView) view.findViewById(R.id.music_listview);

		mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mPlayBroadcast = new MusicPlayBroadcast();
		IntentFilter filter = new IntentFilter(Constant.Music.BROADCAST_NAME);
		filter.addAction(Constant.Music.BROADCAST_NAME);
		filter.addAction(Constant.Music.BROADCAST_QUERY_COMPLETE_NAME);
		mActivity.registerReceiver(mPlayBroadcast, filter);

		mUIm = new MusicMyUIManager(mActivity, mServiceManager, view,
				mUIManager);
		mSdm = new MusicSlidingDrawerManager(mActivity, mServiceManager, view);
		mMusicTimer = new MusicTimer(mSdm.mHandler, mUIm.mHandler);
		mSdm.setMusicTimer(mMusicTimer);

		initListView();

		initListViewStatus();
	}

	private void initBg(View view) {
		mMainLayout = (RelativeLayout) view
				.findViewById(R.id.main_mymusic_layout);
		mMainLayout.setOnTouchListener(this);
		MusicSPStorage mSp = new MusicSPStorage(mActivity);
		String mDefaultBgPath = mSp.getPath();
		Bitmap bitmap = mUIManager.getBitmapByPath(mDefaultBgPath);
		if (bitmap != null) {
			mMainLayout.setBackgroundDrawable(new BitmapDrawable(mActivity
					.getResources(), bitmap));
		} else {
			// mMainLayout.setBackgroundResource(R.drawable.bg);
		}
	}

	private void initListViewStatus() {
		try {
			mSdm.setListViewAdapter(mAdapter);
			int playState = mServiceManager.getPlayState();
			if (playState == Constant.Music.MPS_NOFILE
					|| playState == Constant.Music.MPS_INVALID) {
				return;
			}
			if (playState == Constant.Music.MPS_PLAYING) {
				mMusicTimer.startTimer();
			}
			List<MusicInfo> musicList = mAdapter.getData();
			int playingSongPosition = MusicUtils.seekPosInListById(musicList,
					mServiceManager.getCurMusicId());
			mAdapter.setPlayState(playState, playingSongPosition);
			MusicInfo music = mServiceManager.getCurMusic();
			mSdm.refreshUI(mServiceManager.position(), music.duration, music);
			mSdm.showPlay(false);
			mUIm.refreshUI(mServiceManager.position(), music.duration, music);
			mUIm.showPlay(false);

		} catch (Exception e) {
			Log.d(TAG, "", e);
		}
	}

	private void initListView() {
		mAdapter = new MusicMyAdapter(mActivity, mServiceManager, mSdm);
		mListView.setAdapter(mAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				mAdapter.refreshPlayingList();
				mServiceManager
						.playById(mAdapter.getData().get(position).songId);
			}
		});
		StringBuffer select = new StringBuffer();
		switch (mFrom) {
		case Constant.Music.START_FROM_ARTIST:
			MusicArtistInfo artistInfo = (MusicArtistInfo) mObj;
			// select.append(" and " + Media.ARTIST + " = '"
			// + artistInfo.artist_name + "'");
			mAdapter.setData(MusicUtils.queryMusic(mActivity,
					select.toString(), artistInfo.artist_name,
					Constant.Music.START_FROM_ARTIST));
			break;

		case Constant.Music.START_FROM_ALBUM:
			MusicAlbumInfo albumInfo = (MusicAlbumInfo) mObj;
			// select.append(" and " + Media.ALBUM_ID + " = "
			// + albumInfo.album_id);
			mAdapter.setData(MusicUtils.queryMusic(mActivity,
					select.toString(), albumInfo.album_id + "",
					Constant.Music.START_FROM_ALBUM));
			break;

		case Constant.Music.START_FROM_FOLDER:
			MusicFolderInfo folderInfo = (MusicFolderInfo) mObj;
			// select.append(" and " + Media.DATA + " like '"
			// + folderInfo.folder_path + File.separator + "%'");
			mAdapter.setData(MusicUtils.queryMusic(mActivity,
					select.toString(), folderInfo.folder_path,
					Constant.Music.START_FROM_FOLDER));
			break;
		case Constant.Music.START_FROM_FAVORITE:
			mAdapter.setData(MusicUtils.queryFavorite(mActivity),
					Constant.Music.START_FROM_FAVORITE);
			break;
		default:
			mAdapter.setData(MusicUtils.queryMusic(mActivity,
					Constant.Music.START_FROM_LOCAL));
			break;
		}
	}

	private class MusicPlayBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constant.Music.BROADCAST_NAME)) {
				MusicInfo music = new MusicInfo();
				int playState = intent.getIntExtra(
						Constant.Music.PLAY_STATE_NAME,
						Constant.Music.MPS_NOFILE);
				int curPlayIndex = intent.getIntExtra(
						Constant.Music.PLAY_MUSIC_INDEX, -1);
				Bundle bundle = intent.getBundleExtra(MusicInfo.KEY_MUSIC);
				if (bundle != null) {
					music = bundle.getParcelable(MusicInfo.KEY_MUSIC);
				}
				mAdapter.setPlayState(playState, curPlayIndex);
				switch (playState) {
				case Constant.Music.MPS_INVALID:// 考虑后面加上如果文件不可播放直接跳到下一首
					mMusicTimer.stopTimer();
					mSdm.refreshUI(0, music.duration, music);
					mSdm.showPlay(true);

					mUIm.refreshUI(0, music.duration, music);
					mUIm.showPlay(true);
					mServiceManager.next();
					break;
				case Constant.Music.MPS_PAUSE:
					mMusicTimer.stopTimer();
					mSdm.refreshUI(mServiceManager.position(), music.duration,
							music);
					mSdm.showPlay(true);

					mUIm.refreshUI(mServiceManager.position(), music.duration,
							music);
					mUIm.showPlay(true);

					mServiceManager.cancelNotification();
					break;
				case Constant.Music.MPS_PLAYING:
					mMusicTimer.startTimer();
					mSdm.refreshUI(mServiceManager.position(), music.duration,
							music);
					mSdm.showPlay(false);

					mUIm.refreshUI(mServiceManager.position(), music.duration,
							music);
					mUIm.showPlay(false);

					Bitmap bitmap = MusicUtils.getCachedArtwork(mActivity,
							music.albumId, defaultArtwork);
					// Bitmap bitmap = MusicUtils.getArtwork(getActivity(),
					// music._id, music.albumId);
					// 更新顶部notification
					mServiceManager.updateNotification(bitmap, music.musicName,
							music.artist);

					break;
				case Constant.Music.MPS_PREPARE:
					mMusicTimer.stopTimer();
					mSdm.refreshUI(0, music.duration, music);
					mSdm.showPlay(true);

					mUIm.refreshUI(0, music.duration, music);
					mUIm.showPlay(true);

					// 读取歌词文件
					mSdm.loadLyric(music);
					break;
				}
			}
		}
	}

	int oldY = 0;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int bottomTop = mBottomLayout.getTop();
		System.out.println(bottomTop);
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			oldY = (int) event.getY();
			if (oldY > bottomTop) {
				mSdm.open();
			}
		}
		return true;
	}

	@Override
	protected void setBgByPath(String path) {
		Bitmap bitmap = mUIManager.getBitmapByPath(path);
		if (bitmap != null) {
			mMainLayout.setBackgroundDrawable(new BitmapDrawable(mActivity
					.getResources(), bitmap));
		}
	}

	@Override
	public View getView() {
		return null;
	}

}
