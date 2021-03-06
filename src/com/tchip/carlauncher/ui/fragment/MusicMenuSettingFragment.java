package com.tchip.carlauncher.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.model.MusicSPStorage;
import com.tchip.carlauncher.ui.activity.MusicMenuSettingActivity;

public class MusicMenuSettingFragment extends Fragment implements
		OnClickListener, Constant {

	private LinearLayout mAdviceLayout, mAboutLayout;
	private CheckedTextView mChangeSongTv, mAutoLyricTv, mFilterSizeTv,
			mFilterTimeTv;
	private MusicSPStorage mSp;

	private ImageButton mBackBtn;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.music_menu_setting_fragment,
				container, false);

		mSp = new MusicSPStorage(getActivity());

		initView(view);

		return view;
	}

	private void initView(View view) {
		mAboutLayout = (LinearLayout) view
				.findViewById(R.id.setting_about_layout);
		mAdviceLayout = (LinearLayout) view
				.findViewById(R.id.setting_advice_layout);
		mAboutLayout.setOnClickListener(this);
		mAdviceLayout.setOnClickListener(this);

		mBackBtn = (ImageButton) view.findViewById(R.id.backBtn);
		mBackBtn.setOnClickListener(this);

		mChangeSongTv = (CheckedTextView) view
				.findViewById(R.id.shake_change_song);
		mAutoLyricTv = (CheckedTextView) view
				.findViewById(R.id.auto_download_lyric);
		mFilterSizeTv = (CheckedTextView) view.findViewById(R.id.filter_size);
		mFilterTimeTv = (CheckedTextView) view.findViewById(R.id.filter_time);

		mChangeSongTv.setChecked(mSp.getShake());
		mAutoLyricTv.setChecked(mSp.getAutoLyric());
		mFilterSizeTv.setChecked(mSp.getFilterSize());
		mFilterTimeTv.setChecked(mSp.getFilterTime());

		mChangeSongTv.setOnClickListener(this);
		mAutoLyricTv.setOnClickListener(this);
		mFilterSizeTv.setOnClickListener(this);
		mFilterTimeTv.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.setting_about_layout:
			break;
		case R.id.setting_advice_layout:
			break;
		case R.id.shake_change_song:
			mChangeSongTv.toggle();
			mSp.saveShake(mChangeSongTv.isChecked());
			Intent intent = new Intent(Constant.Music.BROADCAST_SHAKE);
			intent.putExtra(Constant.Music.SHAKE_ON_OFF,
					mChangeSongTv.isChecked());
			getActivity().sendBroadcast(intent);
			break;
		case R.id.auto_download_lyric:
			mAutoLyricTv.toggle();
			mSp.saveAutoLyric(mAutoLyricTv.isChecked());
			break;
		case R.id.filter_size:
			mFilterSizeTv.toggle();
			mSp.saveFilterSize(mFilterSizeTv.isChecked());
			break;
		case R.id.filter_time:
			mFilterTimeTv.toggle();
			mSp.saveFilterTime(mFilterTimeTv.isChecked());
			break;
		case R.id.backBtn:
			((MusicMenuSettingActivity) getActivity()).mViewPager
					.setCurrentItem(0, true);
			break;
		}
	}
}
