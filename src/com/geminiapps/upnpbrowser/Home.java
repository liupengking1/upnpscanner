package com.geminiapps.upnpbrowser;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Home extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	private static int current_section = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		// Fix the logging integration between java.util.logging and Android
		// internal logging
		org.seamless.util.logging.LoggingUtil
				.resetRootHandler(new FixedAndroidLogHandler());
		// Now you can enable logging as needed for various categories of Cling:
		// Logger.getLogger("org.fourthline.cling").setLevel(Level.FINEST);

		listAdapter = new ArrayAdapter<DeviceDisplay>(this,
				android.R.layout.simple_list_item_1);

		// This will start the UPnP service if it wasn't already started
		getApplicationContext().bindService(
				new Intent(this, AndroidUpnpServiceImpl.class),
				serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			break;
		case 4:
			mTitle = getString(R.string.title_section4);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.home, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_home, container,
					false);
			// TextView textView = (TextView) rootView
			// .findViewById(R.id.section_label);
			// textView.setText(Integer.toString(getArguments().getInt(
			// ARG_SECTION_NUMBER)));
			current_section = getArguments().getInt(ARG_SECTION_NUMBER);
			scanNetwork();
			listAdapter.clear();

			final ListView listview = (ListView) rootView
					.findViewById(R.id.listView1);

			listview.setAdapter(listAdapter);
			listview.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> parent, View v,
						int position, long id) {
					AlertDialog dialog = new AlertDialog.Builder(getActivity())
							.create();
					dialog.setTitle(R.string.deviceDetails);
					DeviceDisplay deviceDisplay = (DeviceDisplay) listview
							.getItemAtPosition(position);
					dialog.setMessage(deviceDisplay.getDetailsMessage());
					dialog.show();
					TextView textView = (TextView) dialog
							.findViewById(android.R.id.message);
					textView.setTextSize(12);
				}
			});
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((Home) activity).onSectionAttached(getArguments().getInt(
					ARG_SECTION_NUMBER));
		}
	}

	private static ArrayAdapter<DeviceDisplay> listAdapter;

	private BrowseRegistryListener registryListener = new BrowseRegistryListener();

	private static AndroidUpnpService upnpService;

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
			if (device.getType().toString()
					.equals("urn:dial-multiscreen-org:device:dial:1")) {
				System.out.println("remote device:"
						+ device.getDetails().getFriendlyName());
				// System.out.println("remote device:"
				// + device.getDetails().getManufacturerDetails()
				// .getManufacturer());
				// System.out.println("remote device:"
				// + device.getType().toString());
				// System.out.println("remote device:"
				// + device.getVersion().getMajor() + ","
				// + device.getVersion().getMinor());
				System.out.println("remote device:"
						+ device.getDIALApplicationURL().toString());
			}
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
				final RemoteDevice device, final Exception ex) {
			if (device.getType().toString()
					.equals("urn:dial-multiscreen-org:device:dial:1")) {
				deviceAdded(device);
				System.out.println("remote device:"
						+ device.getDetails().getFriendlyName());
				// System.out.println("remote device:"
				// + device.getDetails().getManufacturerDetails()
				// .getManufacturer());
				// System.out.println("remote device:"
				// + device.getType().toString());
				// System.out.println("remote device:"
				// + device.getVersion().getMajor() + ","
				// + device.getVersion().getMinor());
				System.out.println("remote device:"
						+ device.getDIALApplicationURL().toString());
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(
								Home.this,
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
					if (current_section == 2) {
						if (!(device
								.getType()
								.toString()
								.contains(
										"urn:schemas-upnp-org:device:InternetGatewayDevice") && device
								.isFullyHydrated())) {
							return;
						}
					} else if (current_section == 3) {
						if (!(device
								.getType()
								.toString()
								.contains(
										"urn:schemas-upnp-org:device:MediaServer") && device
								.isFullyHydrated())) {
							return;
						}
					} else if (current_section == 4) {
						if (!(device
								.getType()
								.toString()
								.contains(
										"urn:schemas-upnp-org:device:MediaRenderer") && device
								.isFullyHydrated())) {
							return;
						}
					} else if (current_section == 5) {
						if (!(device.getType().toString()
								.contains("urn:dial-multiscreen-org:device:dial:1"))) {
							return;
						}
					}
					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						listAdapter.remove(d);
						listAdapter.insert(d, position);
					} else {
						listAdapter.add(d);
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
			} else if (device.getType().toString()
					.equals("urn:dial-multiscreen-org:device:dial:1")) {
				sb.append(getDevice().getDisplayString());
				sb.append("\n\n");
				sb.append(((RemoteDevice) device).getDIALApplicationURL()
						.toString());
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

	public static void scanNetwork() {
		if (upnpService != null) {
			upnpService.getRegistry().removeAllRemoteDevices();
			upnpService.getControlPoint().search();
		}
	}
}
