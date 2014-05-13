package com.geminiapps.upnpbrowser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import com.geminiapps.upnpbrowser.R;

/**
 * @author Christian Bauer
 */
public class MediaBrowserActivity extends Activity {

	// private static final Logger log =
	// Logger.getLogger(BrowseActivity.class.getName());

	private List<ListDisplay> ListAdapter = new ArrayList<ListDisplay>();
	private AndroidUpnpService upnpService;
	private Item selected_item;
	private Stack<String> historyq;
	private Stack<String> pathq;

	private Stack<String> historyq_backup;
	private Stack<String> pathq_backup;
	private Device chosenDMSdevice;
	ListView lv;
	private Context context;
	TextView path;
	BrowserListAdapter list_adapter;
	private static ArrayAdapter<DeviceDisplay> listAdapter;
	boolean blockingflag = false;
	private BrowseRegistryListener registryListener = new BrowseRegistryListener();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mediabrowser);

		// Fix the logging integration between java.util.logging and Android
		// internal logging

		org.seamless.util.logging.LoggingUtil
				.resetRootHandler(new FixedAndroidLogHandler());
		// Now you can enable logging as needed for various categories of Cling:
		// Logger.getLogger("org.fourthline.cling").setLevel(Level.FINEST);

		listAdapter = new ArrayAdapter<DeviceDisplay>(this,
				android.R.layout.simple_list_item_1);

		lv = (ListView) findViewById(R.id.contentlists);
		path = (TextView) findViewById(R.id.sourcepath);
		// This will start the UPnP service if it wasn't already started
		getApplicationContext().bindService(
				new Intent(this, AndroidUpnpServiceImpl.class),
				serviceConnection, Context.BIND_AUTO_CREATE);
		context = getApplicationContext();
		historyq = new Stack<String>();
		pathq = new Stack<String>();

		// setContentView(R.layout.fragment_home);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		list_adapter = new BrowserListAdapter(inflater, ListAdapter, context);
		// final ListView listview = (ListView)
		// view.findViewById(R.id.songlist);
		// listview.setAdapter(list_adapter);

		lv.setAdapter(list_adapter);
		if (chosenDMSdevice != null) {
			browse(chosenDMSdevice, "0");
			historyq.push("0");
		} else {
			showToast("select a media server to browse", true);

		}

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (ListAdapter.get(position).getType() == 1) {
					// block user behavior when network has not been ready
					if (blockingflag)
						return;
					else
						blockingflag = true;
					if (historyq.empty())
						historyq.push("0");

					// add backup in case failure
					if (historyq != null)
						historyq_backup = (Stack<String>) historyq.clone();
					if (pathq != null)
						pathq_backup = (Stack<String>) pathq.clone();

					historyq.push(ListAdapter.get(position).getContainer()
							.getId());
					pathq.push(ListAdapter.get(position).getContainer()
							.getTitle());
					String paths = "/"
							+ chosenDMSdevice.getDetails().getFriendlyName()
							+ "/";
					for (int i = 0; i < pathq.size(); i++) {
						paths = paths + pathq.get(i) + "/";
					}
					path.setText(paths);
					browse(chosenDMSdevice, ListAdapter.get(position)
							.getContainer().getId());

				} else if (ListAdapter.get(position).getType() == 2) {
					try {
						selected_item = ListAdapter.get(position).getItem();
						if (selected_item.getFirstResource().getProtocolInfo()
								.getContentFormatMimeType().toString()
								.contains("audio")) {
							System.out.println("this is an audio");
						}
						if (selected_item.getFirstResource().getProtocolInfo()
								.getContentFormatMimeType().toString()
								.contains("image")) {
							System.out.println("this is an image");
						}
						if (selected_item.getFirstResource().getProtocolInfo()
								.getContentFormatMimeType().toString()
								.contains("video")) {
							System.out.println("this is an video");
						}
					} catch (IllegalArgumentException e) {
						System.out.println("IllegalArgumentException");
					}

				} else if (ListAdapter.get(position).getType() == 3) {
					chosenDMSdevice = ListAdapter.get(position).getDevice();
					browse(chosenDMSdevice, "0");
					historyq.push("0");
					path.setText("/" + chosenDMSdevice + "/");
				}

			}

		});

		// initButtons();

	}

	// protected void StreamVideo() {
	// System.out.println("Stream video");
	// if (StreambelsCore.local_speaker) {
	// if (!StreambelsCore.dmsSharer.checkLocalVideoSupport(selected_item)) {
	// Toast.makeText(this,
	// "This media format is not supported by this phone",
	// Toast.LENGTH_LONG).show();
	// return;
	// }
	// Intent intent = new Intent(this, VideoLocalActivity.class);
	// intent.putExtra("path", selected_item.getFirstResource().getValue());
	// intent.putExtra("duration", selected_item.getFirstResource()
	// .getDuration());
	// startActivity(intent);
	// return;
	// }
	// if (StreambelsCore.selectedDevice != null) {
	// if (!StreambelsCore.dmsSharer
	// .checkAppleTvVideosupport(selected_item)) {
	// Toast.makeText(this,
	// "This media format is not supported over AirPlay",
	// Toast.LENGTH_LONG).show();
	// return;
	// }
	// StreambelsCore.dmsdb.setSelectedVideoItem(selected_item);
	// Intent intent = new Intent(this, VideoLocalPlayActivity.class);
	// intent.putExtra("position", -1);
	// intent.putExtra("videourl", "DMSvideo");
	// startActivity(intent);
	//
	// } else if (StreambelsCore.chosenDLNAdevice != null) {
	// if (!StreambelsCore.dmsSharer.checkVideosupport(selected_item)) {
	// Toast.makeText(
	// this,
	// "This media format is not supported by selected device",
	// Toast.LENGTH_LONG).show();
	// return;
	// }
	// StreambelsCore.dmsdb.setSelectedVideoItem(selected_item);
	// Intent intent = new Intent(this, VideoLocalPlayActivity.class);
	// intent.putExtra("position", -1);
	// intent.putExtra("videourl", "DMSvideo");
	// startActivity(intent);
	//
	// } else {
	// Toast.makeText(this, "select the device to stream",
	// Toast.LENGTH_LONG).show();
	// // showAlertDialog();
	// }
	// }
	//
	// protected void StreamImage() {
	// System.out.println("Stream image");
	// int selected_position = 0;
	// List<Item> list = new ArrayList<Item>();
	// for (int i = 0; i < ListAdapter.size(); i++) {
	// if (ListAdapter.get(i).getType() == 2) {
	// Item item1 = ListAdapter.get(i).getItem();
	// if (item1.equals(selected_item)) {
	// selected_position = list.size();
	// }
	// list.add(item1);
	// }
	// }
	// StreambelsCore.dmsdb.setFolderPlaylist(list);
	// // open photo viewer
	// Intent intent = new Intent(MediaBrowserActivity.this, photoviewer.class);
	// // updatesel(position);
	// Bundle b = new Bundle();
	// // b.putStringArray("arrPath", arrPath);
	// b.putInt("position", selected_position);
	// intent.putExtras(b);
	// startActivity(intent);
	//
	// // try {
	// // if (StreambelsCore.chosenDLNAdevice != null)
	// // shareMusic(selected_item, StreambelsCore.chosenDLNAdevice);
	// // else
	// // showToast("DLNA device is not selected", true);
	// // } catch (IOException e) {
	// // // TODO Auto-generated catch block
	// // e.printStackTrace();
	// // }
	// }
	//
	// protected void StreamMusic() {
	// System.out.println("Stream music");
	// int selected_position = 0;
	// tracks.clear();
	// // add items in this folder in playlist
	// for (int i = 0; i < ListAdapter.size(); i++) {
	// if (ListAdapter.get(i).getType() == 2) {
	// Item item1 = ListAdapter.get(i).getItem();
	// if (item1.equals(selected_item))
	// selected_position = tracks.size();
	// TrackInfo track = new TrackInfo();
	// track.setTitle(item1.getTitle());
	// track.setAlbum(item1
	// .getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM.class));
	// track.setArtist(item1.getCreator());
	// try {
	// track.setMimetype(item1.getFirstResource()
	// .getProtocolInfo().getContentFormatMimeType()
	// .toString());
	// } catch (IllegalArgumentException e) {
	// System.out.println("IllegalArgumentException");
	// }
	// track.setPath(item1.getFirstResource().getValue());
	// // track.setId(item1.getId());
	// track.setDuration(CalcTime(item1.getFirstResource()
	// .getDuration()) * 1000);
	// track.setItem(item1);
	// tracks.add(track);
	//
	// }
	// }
	// StreambelsCore.songs_playing = tracks;
	// streamAudio(selected_position);
	// // try {
	// // if (StreambelsCore.chosenDLNAdevice != null)
	// // shareMusic(selected_item, StreambelsCore.chosenDLNAdevice);
	// // else
	// // showToast("DLNA device is not selected", true);
	// // } catch (IOException e) {
	// // // TODO Auto-generated catch block
	// // e.printStackTrace();
	// // }
	// }
	//
	// public void streamAudio(int position) {
	// StreambelsCore.is_playlist = false;
	//
	// StreambelsCore.songs_playing = tracks;
	// StreambelsCore.track_selected = position;
	// StreambelsCore.current_track = tracks.get(position);
	// if (StreambelsCore.selectedDevice != null) {
	// // if (!(StreambelsCore.current_track.getPath().contains("mp3") ||
	// // StreambelsCore.current_track
	// // .getPath().contains("wav"))) {
	// showToast(
	// "Stream audio from Media Server to AirPlay is not supported",
	// true);
	//
	// return;
	// // }
	// }
	// if (StreambelsCore.chosenDLNAdevice != null) {
	// if (!StreambelsCore.ds.checksupport(StreambelsCore.current_track)) {
	// showToast(
	// "This audio format is not supported by Streambels for this DLNA device.",
	// true);
	// return;
	// }
	// }
	// if (StreambelsCore.local_speaker) {
	// if (!StreambelsCore.ds
	// .checkLocalMusicSupport(StreambelsCore.current_track)) {
	// showToast(
	// "This audio format is not supported by Streambels for this phone",
	// true);
	// return;
	// }
	// }
	// if (StreambelsCore.is_playing_video) {
	// if (StreambelsCore.vs != null)
	// StreambelsCore.vs.tearDown();
	// if (StreambelsCore.ds != null) {
	// StreambelsCore.ds.stop();
	// StreambelsCore.ds.stopProxy();
	// }
	// StreambelsCore.is_playing_video = false;
	// }
	// Intent intent = new Intent(context, TuxeraAirPlayActivity.class);
	// intent.putExtra("reloadui", 0);
	// // intent.putExtra("musicInfo",tracks.get(position));
	// startActivityForResult(intent, 0);
	// }
	//
	// private void initButtons() {
	// Button btnback = (Button) findViewById(R.id.btnback);
	// btnback.setOnClickListener(new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// // block user behavior when network has not been ready
	// if (blockingflag)
	// return;
	// else
	// blockingflag = true;
	//
	// // add backup in case failure
	// if (historyq != null)
	// historyq_backup = (Stack<String>) historyq.clone();
	// if (pathq != null)
	// pathq_backup = (Stack<String>) pathq.clone();
	//
	// if (!historyq.empty())
	// historyq.pop();
	// if (!pathq.empty())
	// pathq.pop();
	// if (historyq.empty()) {
	// ;
	// } else {
	// String ctn = (String) historyq.peek();
	// String paths = "/";
	// for (int i = 0; i < pathq.size(); i++) {
	// paths = paths + pathq.get(i) + "/";
	// }
	// path.setText(paths);
	// browse(StreambelsCore.chosenDMSdevice, ctn);
	// }
	// }
	//
	// });
	// Button btnbrowse = (Button) findViewById(R.id.btnbrowse);
	// btnbrowse.setOnClickListener(new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// finish();
	// }
	//
	// });
	//
	// Button btnhome = (Button) findViewById(R.id.musicHomebtn);
	// btnhome.setOnClickListener(new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// System.out.println("Home button clicked");
	// Intent intent = new Intent();
	// setResult(Activity.RESULT_OK, intent);
	// finish();
	// }
	//
	// });
	//
	// RelativeLayout playerlayout = (RelativeLayout)
	// findViewById(R.id.music_player);
	// playerlayout.setOnClickListener(new OnClickListener() {
	// @Override
	// public void onClick(View v) {
	// Intent intent = new Intent(context, TuxeraAirPlayActivity.class);
	// intent.putExtra("reloadui", 1);
	// startActivityForResult(intent, 0);
	// }
	//
	// });
	//
	// inAppdialog = new InappPurchaseDialog(context);
	// inAppdialog.setOnKeyListener(new InappPurchaseDialog.OnKeyListener() {
	//
	// @Override
	// public boolean onKey(DialogInterface arg0, int keyCode,
	// KeyEvent event) {
	// // TODO Auto-generated method stub
	// if (keyCode == KeyEvent.KEYCODE_BACK) {
	// ToggleButton btnpause = (ToggleButton)
	// findViewById(R.id.music_pause_btn);
	// if (BackendService.player_status == StreambelsCore.ST_PLAYER_PLAYING)
	// btnpause.setChecked(true);
	// else
	// btnpause.setChecked(false);
	// inAppdialog.dismiss();
	//
	// }
	// return true;
	// }
	// });

	// }

	// for dms selection
	// public void setMediaSourceNotification() {
	// LinearLayout sourcelistlayout = (LinearLayout)
	// findViewById(R.id.sourcelist);
	// TextView sourcetxt = (TextView) findViewById(R.id.sourcetxt);
	// ImageView sourceimg = (ImageView) findViewById(R.id.sourcepic);
	// if (StreambelsCore.chosenDMSdevice == null) {
	// // sourcelistlayout.setBackgroundResource(R.color.orange);
	// sourcetxt.setText("Phone Storage");
	// } else {
	//
	// String value = null;
	// try {
	// // transcode to show Chinese character
	// byte ptext[] = StreambelsCore.chosenDMSdevice.getDetails()
	// .getFriendlyName().getBytes("ISO-8859-1");
	// value = new String(ptext, "UTF-8");
	// } catch (UnsupportedEncodingException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// sourcetxt.setText(value);
	// }
	//
	// }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	protected void showToast(final String msg, final boolean longLength) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MediaBrowserActivity.this, msg,
						longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// block user behavior when network has not been ready
			if (blockingflag)
				return true;
			else
				blockingflag = true;

			// add backup in case failure
			if (historyq != null)
				historyq_backup = (Stack<String>) historyq.clone();
			if (pathq != null)
				pathq_backup = (Stack<String>) pathq.clone();

			if (!historyq.empty())
				historyq.pop();
			if (!pathq.empty())
				pathq.pop();
			if (historyq.empty()) {
				finish();
				chosenDMSdevice=null;
				
			} else {
				String ctn = (String) historyq.peek();
				String paths = "/"
						+ chosenDMSdevice.getDetails().getFriendlyName() + "/";
				for (int i = 0; i < pathq.size(); i++) {
					paths = paths + pathq.get(i) + "/";
				}
				path.setText(paths);
				browse(chosenDMSdevice, ctn);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void browse(final Device device, String id) {
		ControlPoint cp = upnpService.getControlPoint();
		ServiceId serviceId2 = new UDAServiceId("ContentDirectory");
		Service m_cds = device.findService(serviceId2);
		if (m_cds == null) {
			System.out.println("can not find service");
			return;
		}
		cp.execute(new Browse(m_cds, id, BrowseFlag.DIRECT_CHILDREN) {

			@Override
			public void received(ActionInvocation actionInvocation,
					DIDLContent didl) {
				blockingflag = false;
				updateList(didl);

			}

			@Override
			public void updateStatus(Status status) {
				// Called before and after loading the DIDL content
			}

			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse operation, String defaultMsg) {
				// Something wasn't right...

				blockingflag = false;

				showToast(defaultMsg, false);
				if (pathq_backup != null)
					pathq = (Stack<String>) pathq_backup.clone();
				if (historyq_backup != null)
					historyq = (Stack<String>) historyq_backup.clone();

				runOnUiThread(new Runnable() {
					public void run() {
						String paths = "/"
								+ chosenDMSdevice.getDetails()
										.getFriendlyName() + "/";
						for (int i = 0; i < pathq.size(); i++) {
							paths = paths + pathq.get(i) + "/";
						}
						path.setText(paths);
					}

				});

				System.out.println(defaultMsg);
			}

		});
	}

	protected void updateList(final DIDLContent didl) {
		runOnUiThread(new Runnable() {
			public void run() {
				ListAdapter.clear();
				for (int i = 0; i < didl.getContainers().size(); i++) {
					Container container = didl.getContainers().get(i);
					ListAdapter.add(new ListDisplay(container));
				}
				for (int i = 0; i < didl.getItems().size(); i++) {
					Item item = didl.getItems().get(i);
					ListAdapter.add(new ListDisplay(item));
				}
				list_adapter.notifyDataSetChanged();
			}
		});
	}

	private int CalcTime(String Time) {
		if (Time == null)
			return 0;
		String[] tokens = Time.split(":");
		if (tokens.length != 3)
			return 1;
		double hours = Double.parseDouble(tokens[0]);
		double minutes = Double.parseDouble(tokens[1]);
		double seconds = Double.parseDouble(tokens[2]);
		int duration_t = (int) (3600 * hours + 60 * minutes + seconds);
		return duration_t;
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;
			System.out.println("service is running");
			// Clear the list
			listAdapter.clear();

			// Get ready for future device advertisements
			upnpService.getRegistry().addListener(registryListener);

			// Now add all devices to the list we already know about
			for (Device<?, ?, ?> device : upnpService.getRegistry()
					.getDevices()) {
				registryListener.deviceAdded(device);
			}

			// Search asynchronously for all devices, they will respond soon
			upnpService.getControlPoint().search();
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}
	};

	protected class BrowseRegistryListener extends DefaultRegistryListener {

		/* Discovery performance optimization for very slow Android devices! */
		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry,
				RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
				final RemoteDevice device, final Exception ex) {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(
							MediaBrowserActivity.this,
							"Discovery failed of '"
									+ device.getDisplayString()
									+ "': "
									+ (ex != null ? ex.toString()
											: "Couldn't retrieve device/service descriptors"),
							Toast.LENGTH_LONG).show();
				}
			});
			deviceRemoved(device);
		}

		/*
		 * End of optimization, you can remove the whole block if your Android
		 * handset is fast (>= 600 Mhz)
		 */

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			deviceRemoved(device);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			deviceAdded(device);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			deviceRemoved(device);
		}

		public void deviceAdded(final Device<?, ?, ?> device) {
			runOnUiThread(new Runnable() {
				public void run() {
					System.out.println("device added");
					DeviceDisplay d = new DeviceDisplay(device);
					int position = listAdapter.getPosition(d);

					if (!(device
							.getType()
							.toString()
							.contains("urn:schemas-upnp-org:device:MediaServer") && device
							.isFullyHydrated())) {
						return;
					}

					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						listAdapter.remove(d);
						listAdapter.insert(d, position);

						if (chosenDMSdevice == null) {
							ListAdapter.remove(position);
							ListAdapter.set(position, new ListDisplay(device));
							list_adapter.notifyDataSetChanged();
						}
					} else {
						listAdapter.add(d);

						if (chosenDMSdevice == null) {
							ListAdapter.add(new ListDisplay(device));
							list_adapter.notifyDataSetChanged();
							System.out.println("add server,,,,,");
						}
					}
				}
			});
		}

		public void deviceRemoved(final Device<?, ?, ?> device) {
			runOnUiThread(new Runnable() {
				public void run() {
					listAdapter.remove(new DeviceDisplay(device));
				}
			});
		}
	}

	protected class DeviceDisplay {

		Device<?, ?, ?> device;

		public DeviceDisplay(Device<?, ?, ?> device) {
			this.device = device;
		}

		public Device<?, ?, ?> getDevice() {
			return device;
		}

		// DOC:DETAILS
		public String getDetailsMessage() {
			StringBuilder sb = new StringBuilder();
			if (getDevice().isFullyHydrated()) {
				sb.append(getDevice().getDisplayString());
				sb.append("\n\n");
				for (Service<?, ?> service : getDevice().getServices()) {
					sb.append(service.getServiceType()).append("\n");
				}
			} else {
				sb.append(getString(R.string.deviceDetailsNotYetAvailable));
			}
			return sb.toString();
		}

		// DOC:DETAILS

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			DeviceDisplay that = (DeviceDisplay) o;
			return device.equals(that.device);
		}

		@Override
		public int hashCode() {
			return device.hashCode();
		}

		@Override
		public String toString() {
			String name = getDevice().getDetails() != null
					&& getDevice().getDetails().getFriendlyName() != null ? getDevice()
					.getDetails().getFriendlyName() : getDevice()
					.getDisplayString();
			// Display a little star while the device is being loaded (see
			// performance optimization earlier)
			return device.isFullyHydrated() ? name : name + " *";
		}
	}
}
