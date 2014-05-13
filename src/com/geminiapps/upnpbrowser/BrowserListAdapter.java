package com.geminiapps.upnpbrowser;

import java.util.List;

import org.fourthline.cling.support.model.item.Item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BrowserListAdapter extends BaseAdapter {
	class ViewHolder {
		public TextView title;
		public ImageView image;
	}

	private LayoutInflater Inflater;
	List<ListDisplay> tracks;
	Context context;

	public BrowserListAdapter(LayoutInflater Inflater,
			List<ListDisplay> tracks, Context context) {
		this.tracks = tracks;
		this.Inflater = Inflater;
		this.context = context;
	}

	@Override
	public int getCount() {
		return tracks.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = Inflater.inflate(R.layout.browser_item, null);
			holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(R.id.itemtitle);
//			holder.title.setTypeface(StreambelsCore.helvetaTypeface);
			holder.image = (ImageView) convertView.findViewById(R.id.itemimage);
			// holder.album_art.setImageResource(R.drawable.default_cover);
			// convertView.setId(tracks.get(position).getId());
			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();
		}
//		holder.title.setTypeface(StreambelsCore.helvetaTypeface);
		holder.title.setText(tracks.get(position).toString());

		if (tracks.get(position).getType() == 1)
			holder.image.setBackgroundResource(R.drawable.folder);
		else if (tracks.get(position).getType() == 2)
			setItemBackground(tracks.get(position).getItem(), holder);

		return convertView;
	}

	private void setItemBackground(Item item, ViewHolder holder) {
		String SongPath = item.getFirstResource().getValue();
		String extension = null;
		if (SongPath.contains(".")) {
			// Split it.
			String arrs[] = SongPath.split("\\.");
			extension = arrs[arrs.length - 1].toLowerCase();
			// extension = SongPath.substring(SongPath.indexOf('.') + 1);
		}
		System.out.println("item extension:"+extension);
		if (extension == null)
			holder.image.setBackgroundResource(R.drawable.unknown);
		else if(extension.contains("mp3"))
			holder.image.setBackgroundResource(R.drawable.mp3);
		else if(extension.contains("aac"))
			holder.image.setBackgroundResource(R.drawable.aac);
		else if(extension.contains("avi"))
			holder.image.setBackgroundResource(R.drawable.avi);
		else if(extension.contains("flac"))
			holder.image.setBackgroundResource(R.drawable.flac);
		else if(extension.contains("flv"))
			holder.image.setBackgroundResource(R.drawable.flv);
		else if(extension.contains("gif"))
			holder.image.setBackgroundResource(R.drawable.gif);
		else if(extension.contains("gp"))
			holder.image.setBackgroundResource(R.drawable.gp);
		else if(extension.contains("jpg"))
			holder.image.setBackgroundResource(R.drawable.jpg);
		else if(extension.contains("m3u"))
			holder.image.setBackgroundResource(R.drawable.m3u);
		else if(extension.contains("m4a"))
			holder.image.setBackgroundResource(R.drawable.m4a);
		else if(extension.contains("m4v"))
			holder.image.setBackgroundResource(R.drawable.m4v);
		else if(extension.contains("mkv"))
			holder.image.setBackgroundResource(R.drawable.mkv);
		else if(extension.contains("mov"))
			holder.image.setBackgroundResource(R.drawable.mov);
		else if(extension.contains("mp4"))
			holder.image.setBackgroundResource(R.drawable.mp4);
		else if(extension.contains("mpg"))
			holder.image.setBackgroundResource(R.drawable.mpg);
		else if(extension.contains("oga"))
			holder.image.setBackgroundResource(R.drawable.oga);
		else if(extension.contains("ogg"))
			holder.image.setBackgroundResource(R.drawable.ogg);
		else if(extension.contains("ogv"))
			holder.image.setBackgroundResource(R.drawable.ogv);
		else if(extension.contains("png"))
			holder.image.setBackgroundResource(R.drawable.png);
		else if(extension.contains("swf"))
			holder.image.setBackgroundResource(R.drawable.swf);
		else if(extension.contains("ts"))
			holder.image.setBackgroundResource(R.drawable.ts);
		else if(extension.contains("wav"))
			holder.image.setBackgroundResource(R.drawable.wav);
		else if(extension.contains("webm"))
			holder.image.setBackgroundResource(R.drawable.webm);
		else if(extension.contains("wma"))
			holder.image.setBackgroundResource(R.drawable.wma);
		else if(extension.contains("wmv"))
			holder.image.setBackgroundResource(R.drawable.wmv);
		else if(extension.contains("pcm"))
			holder.image.setBackgroundResource(R.drawable.pcm);
		else 
			holder.image.setBackgroundResource(R.drawable.unknown);
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

}
