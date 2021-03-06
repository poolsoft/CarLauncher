package com.tchip.carlauncher.lib.filemanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tchip.carlauncher.R;

public class FileCardAdapter extends FileAdapter
{
	private static final int TEXT_FILE_PREVIEW_LENGTH = 1000;
	private static final int
		VIEW_TYPE_NONE = 0,
		VIEW_TYPE_IMAGE = 1,
		VIEW_TYPE_TEXT = 2,
        VIEW_TYPE_MUSIC = 3,
		VIEW_TYPE_INVALID = -1,
		VIEW_TYPE_COUNT = 4;
	private static final int MAX_PREFETCH_JOBS = 2;
	private final FilePreviewCache thumbCache;
	Map<ImageView, CardPreviewer> runningTasks = new HashMap<ImageView, CardPreviewer>();
	Map<File, CardPreviewer> prefetchTasks = new HashMap<File, CardPreviewer>();
	int[] viewTypes;
	
	@SuppressLint("UseSparseArrays")
	public FileCardAdapter(Context context, File[] files, FilePreviewCache previewCache, FileIconResolver fileIconResolver)
	{
		super(context, R.layout.file_list_item_file_card_image, files, fileIconResolver);
		this.thumbCache = previewCache;
		
		viewTypes = new int[files.length];
		for (int i=0; i < viewTypes.length; i++)
			viewTypes[i] = VIEW_TYPE_INVALID;
	}
	
	@SuppressLint("UseSparseArrays")
	public FileCardAdapter(Context context, List<File> files, FilePreviewCache previewCache, FileIconResolver fileIconResolver)
	{
		super(context, R.layout.file_list_item_file_card_image, files, fileIconResolver);
		this.thumbCache = previewCache;
		
		viewTypes = new int[files.size()];
		for (int i=0; i < viewTypes.length; i++)
			viewTypes[i] = VIEW_TYPE_INVALID;
	}
	
	@Override
	public int getItemViewType(int position)
	{
		int type = viewTypes[position];
		if (type == VIEW_TYPE_INVALID)
		{
			File file = getItem(position);
			if (file.isDirectory())
				type = VIEW_TYPE_IMAGE;
			else
			{
				String mime = FileUtils.getFileMimeType(file);
				if (mime.startsWith("image/") || mime.startsWith("video/"))
					type = VIEW_TYPE_IMAGE;
                else if (mime.startsWith("audio/"))
                    type = VIEW_TYPE_MUSIC;
				else if (mime.startsWith("text/"))
					type = VIEW_TYPE_TEXT;
				else
					type = VIEW_TYPE_NONE;
					
			}
			viewTypes[position] = type;
		}
		return type;
	}
	
	@Override
	public int getViewTypeCount()
	{
		return VIEW_TYPE_COUNT;
	}
	
	@Override
	protected int getItemLayoutId(int position)
	{
		switch (getItemViewType(position))
		{
			case VIEW_TYPE_IMAGE:
            case VIEW_TYPE_MUSIC:
				return super.getItemLayoutId(position);
			case VIEW_TYPE_TEXT:
				return R.layout.file_list_item_file_card_text;				
			default:
				return R.layout.file_list_item_file_card;
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final View view = super.getView(position, convertView, parent);
		final ViewHolder viewHolder = (ViewHolder) view.getTag();
		
		File file = getItem(position);
		switch (getItemViewType(position))
		{
			case VIEW_TYPE_IMAGE:
            case VIEW_TYPE_MUSIC:
				ImageView imgFileContent = viewHolder.getViewById(R.id.imgFileContent);
				if (runningTasks.containsKey(imgFileContent))
				{
					runningTasks.get(imgFileContent).setImageView(null);
					runningTasks.remove(imgFileContent);
				}
				
				if (thumbCache.get(file) != null)
					imgFileContent.setImageBitmap(thumbCache.get(file));
				else if (prefetchTasks.containsKey(file))
				{
					CardPreviewer cardPreviewer = prefetchTasks.get(file);
					cardPreviewer.setImageView(imgFileContent);
					runningTasks.put(imgFileContent, cardPreviewer);
					prefetchTasks.remove(cardPreviewer);
				}
				else
				{
					CardPreviewer previewer = (CardPreviewer) new CardPreviewer(imgFileContent, thumbCache).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
					runningTasks.put(imgFileContent, previewer);
				}
				break;
				
			case VIEW_TYPE_TEXT:				
				try
				{
					FileReader fileReader = new FileReader(file);
					char[] buffer = new char[TEXT_FILE_PREVIEW_LENGTH];
					int len = fileReader.read(buffer);
					fileReader.close();
					
					TextView textView = viewHolder.getViewById(R.id.tvFileContent);
					
					textView.setText(Html.fromHtml(new String(buffer, 0, len)));
				} catch (FileNotFoundException e)
				{
					e.printStackTrace();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				
				break;
			
		}
		
		View cardBg = viewHolder.getViewById(R.id.layoutCard); 
		if (isSelected(file)) cardBg.setBackgroundResource(R.drawable.file_selector_card_selected);
		else cardBg.setBackgroundResource(R.drawable.file_selector_card);
        //noinspection ResourceType
        view.setBackgroundResource(R.color.color_window_background);
		return view;
	}
	
	public void prefetchImage(File file)
	{
		if (prefetchTasks.containsKey(file) == false && thumbCache.get(file) == null && prefetchTasks.size() < MAX_PREFETCH_JOBS)
		{
			prefetchTasks.put(file, (CardPreviewer) new CardPreviewer(null, thumbCache).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file));
		}
	}

	public void prefetchImages(int startPrefetch, int numItems)
	{
		if (startPrefetch >= getCount()) return;
		if (startPrefetch+numItems >= getCount()) numItems = getCount() - startPrefetch - 1;
		for (int i=startPrefetch; i < startPrefetch+numItems; i++)
		{
			prefetchImage(getItem(i));
		}
	}
}
