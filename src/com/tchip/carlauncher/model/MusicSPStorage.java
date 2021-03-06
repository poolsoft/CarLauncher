package com.tchip.carlauncher.model;

import com.tchip.carlauncher.Constant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

@SuppressLint({ "WorldWriteableFiles", "CommitPrefEdits" })
public class MusicSPStorage implements Constant {

	private SharedPreferences mSp;
	private Editor mEditor;

	public MusicSPStorage(Context context) {
		mSp = context.getSharedPreferences(Constant.Music.SP_NAME,
				Context.MODE_WORLD_WRITEABLE);
		mEditor = mSp.edit();
	}

	/**
	 * 保存背景图片的地址
	 */
	public void savePath(String path) {
		mEditor.putString(Constant.Music.SP_BG_PATH, path);
		mEditor.commit();
	}

	/**
	 * 获取背景图片的地址
	 * 
	 * @return
	 */
	public String getPath() {
		return mSp.getString(Constant.Music.SP_BG_PATH, null);
	}

	public void saveShake(boolean shake) {
		mEditor.putBoolean(Constant.Music.SP_SHAKE_CHANGE_SONG, shake);
		mEditor.commit();
	}

	public boolean getShake() {
		return mSp.getBoolean(Constant.Music.SP_SHAKE_CHANGE_SONG, false);
	}

	public void saveAutoLyric(boolean auto) {
		mEditor.putBoolean(Constant.Music.SP_AUTO_DOWNLOAD_LYRIC, auto);
		mEditor.commit();
	}

	public boolean getAutoLyric() {
		return mSp.getBoolean(Constant.Music.SP_AUTO_DOWNLOAD_LYRIC, false);
	}

	public void saveFilterSize(boolean size) {
		mEditor.putBoolean(Constant.Music.SP_FILTER_SIZE, size);
		mEditor.commit();
	}

	public boolean getFilterSize() {
		return mSp.getBoolean(Constant.Music.SP_FILTER_SIZE, false);
	}

	public void saveFilterTime(boolean time) {
		mEditor.putBoolean(Constant.Music.SP_FILTER_TIME, time);
		mEditor.commit();
	}

	public boolean getFilterTime() {
		return mSp.getBoolean(Constant.Music.SP_FILTER_TIME, false);
	}

}
