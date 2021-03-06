package com.tchip.carlauncher.ui.activity;


import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.model.Typefaces;
import com.tchip.carlauncher.util.SettingUtil;
import com.tchip.carlauncher.view.SwitchButton;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import android.provider.Settings;

public class FmTransmitActivity extends Activity {

	private RelativeLayout layoutBack;
	private TextView textHint;
	private SeekBar fmSeekBar;

	private Button fmFreqDecrease, fmFreqIncrease;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fm_transmit);

		initialLayout();
	}

	private void initialLayout() {
		// 开关
		SwitchButton switchFm = (SwitchButton) findViewById(R.id.switchFm);

		layoutBack = (RelativeLayout) findViewById(R.id.layoutBack);
		layoutBack.setOnClickListener(new MyOnClickListener());

		textHint = (TextView) findViewById(R.id.textHint);
		textHint.setTypeface(Typefaces.get(this, Constant.Path.FONT
				+ "Font-Helvetica-Neue-LT-Pro.otf"));

		switchFm.setChecked(isFmTransmitOn());
		switchFm.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Settings.System.putString(getContentResolver(),
						Constant.FMTransmit.SETTING_ENABLE, isChecked ? "1"
								: "0");
				SettingUtil.SaveFileToNode(SettingUtil.nodeFmEnable, (isChecked ? "1" : "0"));

			}
		});

		fmSeekBar = (SeekBar) findViewById(R.id.fmSeekBar);
		// 875-1080
		// 0- 205
		fmSeekBar.setMax(205);
		int nowFrequency = SettingUtil.getFmFrequceny(this); // 当前频率
		fmSeekBar.setProgress(nowFrequency / 10 - 875);
		textHint.setText("  " + nowFrequency / 100.0f + "MHz");
		fmSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				SettingUtil.setFmFrequency(getApplicationContext(),
						(seekBar.getProgress() + 875) * 10);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				float frequency = (progress + 875.0f) / 10;
				textHint.setText("  " + frequency + "MHz");
			}
		});

		// fm频率0.1增加减少
		fmFreqDecrease = (Button) findViewById(R.id.fmFreqDecrease);
		fmFreqIncrease = (Button) findViewById(R.id.fmFreqIncrease);
		fmFreqDecrease.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setFmFrequencySmallDeIncrease(false);
			}
		});
		fmFreqIncrease.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setFmFrequencySmallDeIncrease(true);
			}
		});
	}

	/**
	 * 小幅度的调整fm频率
	 * 
	 * @param deincrease
	 */
	private void setFmFrequencySmallDeIncrease(boolean deincrease) {
		int nowFrequency = SettingUtil.getFmFrequceny(this)
				+ (deincrease ? 10 : -10); // 当前频率
		fmSeekBar.setProgress(nowFrequency / 10 - 875);
		textHint.setText("  " + nowFrequency / 100.0f + "MHz");

		SettingUtil.setFmFrequency(this, nowFrequency);
	}

	/**
	 * FM发射是否打开
	 * 
	 * @return
	 */
	private boolean isFmTransmitOn() {
		boolean isFmTransmitOpen = false;
		String fmEnable = Settings.System.getString(getContentResolver(),
				Constant.FMTransmit.SETTING_ENABLE);
		if (fmEnable.trim().length() > 0) {
			if ("1".equals(fmEnable)) {
				isFmTransmitOpen = true;
			} else {
				isFmTransmitOpen = false;
			}
		}
		return isFmTransmitOpen;
	}

	class MyOnClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.layoutBack:
				finish();
				break;
			}
		}
	}

}
