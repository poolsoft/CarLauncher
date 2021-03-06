package com.tchip.carlauncher.lib.filemanager;

import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;

import com.tchip.carlauncher.lib.filemanager.NavDrawerAdapter.NavDrawerItem;

import java.io.File;

public abstract class NavDrawerShortcut implements NavDrawerItem {
	public abstract File getFile();

	@Override
	public boolean onClicked(FolderActivity activity) {
		if (getFile().equals(activity.getLastFolder()))
			return true;
		Bundle args = new Bundle();
		args.putString(FolderFragment.EXTRA_DIR, getFile().getAbsolutePath());
		FolderFragment folderFragment = new FolderFragment();
		folderFragment.setArguments(args);
		activity.showFragment(folderFragment);
		return true;
	}

	@Override
	public CharSequence getSubTitle(Context context) {
		return FileUtils.getUserFriendlySdcardPath(getFile());
	}

	@Override
	public void setImageToView(ImageView imageView) {
		imageView.setImageResource(FileUtils.getFileIconResource(getFile()));
	}

	@Override
	public int getViewType() {
		return TYPE_SHORTCUT;
	}

}
