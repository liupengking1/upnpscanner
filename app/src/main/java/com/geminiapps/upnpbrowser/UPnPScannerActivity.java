package com.geminiapps.upnpbrowser;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.geminiapps.upnpscanner.BuildConfig;
import com.geminiapps.upnpscanner.R;
import com.vungle.publisher.VunglePub;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.android.FixedAndroidLogHandler;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;

import java.util.logging.Level;
import java.util.logging.Logger;

import timber.log.Timber;

public class UPnPScannerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static int current_section = 0;
    private ListView deviceList;
    // get the VunglePub instance
    final VunglePub vunglePub = VunglePub.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upnp_scanner);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        deviceList = (ListView) findViewById(R.id.listView1);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewDevice();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.filter_all);

        setVersionName();

        // setup timber
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
        startUpnpService();

        // get your App ID from the app's main page on the Vungle Dashboard after setting up your app
        final String app_id = "5834bb492f3759ce01000073";

        // initialize the Publisher SDK
        vunglePub.init(this, app_id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        vunglePub.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vunglePub.onResume();
    }

    void setVersionName() {
        final LayoutInflater factory = getLayoutInflater();

        final View textEntryView = factory.inflate(R.layout.nav_header_upnp_scanner, null);
        final TextView versionName = (TextView) textEntryView.findViewById(R.id.versionName);
        PackageInfo pInfo = null;
        String version = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "null";
            Timber.e("package not found, %s", e);
        }

        final String finalVersion = version;
        UPnPScannerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                versionName.setText(finalVersion);
            }
        });
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

            //FakeCrashLibrary.log(priority, tag, message);

            if (t != null) {
                if (priority == Log.ERROR) {
                    //FakeCrashLibrary.logError(t);
                } else if (priority == Log.WARN) {
                    //FakeCrashLibrary.logWarning(t);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.upnp_scanner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_scan_network) {
            scanNetwork();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.filter_all) {
            current_section = 0;
            scanNetwork();
        } else if (id == R.id.filter_router) {
            current_section = 1;
            scanNetwork();
        } else if (id == R.id.filter_media_server) {
            current_section = 2;
            scanNetwork();
        } else if (id == R.id.filter_media_renderer) {
            current_section = 3;
            scanNetwork();
        } else if (id == R.id.filter_dial) {
            current_section = 4;
            scanNetwork();
        } else if (id == R.id.nav_rate) {
            openRatingPage();
        } else if (id == R.id.nav_send) {
            sendFeedback();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openRatingPage() {
        Uri uri = Uri.parse("market://details?id="
                + getApplicationContext().getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(),
                    "Couldn't launch the market", Toast.LENGTH_LONG).show();
        }
    }

    private void sendFeedback() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"geminiapps14@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "UPnP Scanner Feedback");
        i.putExtra(Intent.EXTRA_TEXT, "");
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(UPnPScannerActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startUpnpService() {
        // Fix the logging integration between java.util.logging and Android
        // internal logging
        org.seamless.util.logging.LoggingUtil
                .resetRootHandler(new FixedAndroidLogHandler());
        // Now you can enable logging as needed for various categories of Cling:
        Logger.getLogger("org.fourthline.cling").setLevel(Level.FINEST);

        listAdapter = new ArrayAdapter<DeviceDisplay>(this,
                android.R.layout.simple_list_item_1);
        deviceList.setAdapter(listAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                AlertDialog dialog = new AlertDialog.Builder(v.getContext(), R.style.MyAlertDialogStyle)
                        .create();
                dialog.setTitle(R.string.deviceDetails);
                DeviceDisplay deviceDisplay = (DeviceDisplay) deviceList
                        .getItemAtPosition(position);

                dialog.setMessage(deviceDisplay.getDetailsMessage());
                dialog.show();
                TextView textView = (TextView) dialog
                        .findViewById(android.R.id.message);
                textView.setTextSize(12);

                vunglePub.playAd();
            }
        });
        // This will start the UPnP service if it wasn't already started
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private static ArrayAdapter<DeviceDisplay> listAdapter;

    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private static AndroidUpnpService upnpService;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            upnpService = (AndroidUpnpService) service;
            Timber.d("service is running");
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
                Timber.d("remote device:"
                        + device.getDetails().getFriendlyName());
                Timber.d("remote device:" + device.getDIALApplicationURL().toString());
            }
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry,
                                                final RemoteDevice device, final Exception ex) {
            if (device.getType().toString()
                    .equals("urn:dial-multiscreen-org:device:dial:1")) {
                deviceAdded(device);
                Timber.d("remote device:"
                        + device.getDetails().getFriendlyName());
                Timber.d("remote device:" + device.getDIALApplicationURL().toString());
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(
                                UPnPScannerActivity.this,
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
                    Timber.d("device added");
                    DeviceDisplay d = new DeviceDisplay(device);
                    int position = listAdapter.getPosition(d);
                    if (current_section == 1) {
                        if (!(device
                                .getType()
                                .toString()
                                .contains(
                                        "urn:schemas-upnp-org:device:InternetGatewayDevice") && device
                                .isFullyHydrated())) {
                            return;
                        }
                    } else if (current_section == 2) {
                        if (!(device
                                .getType()
                                .toString()
                                .contains(
                                        "urn:schemas-upnp-org:device:MediaServer") && device
                                .isFullyHydrated())) {
                            return;
                        }
                    } else if (current_section == 3) {
                        if (!(device
                                .getType()
                                .toString()
                                .contains(
                                        "urn:schemas-upnp-org:device:MediaRenderer") && device
                                .isFullyHydrated())) {
                            return;
                        }
                    } else if (current_section == 4) {
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
                sb.append("\n\nFriendly name: \n");
                sb.append(getDevice().getDetails().getFriendlyName());
                sb.append("\n\nDevice type: \n");
                sb.append(getDevice().getType().toString());
                sb.append("\n\nBaseURL: \n");
                sb.append(getDevice().getDetails().getBaseURL());
                sb.append("\n\nPresentationURL: \n");
                sb.append(getDevice().getDetails().getPresentationURI());
                sb.append("\n\nServices: \n");
                for (Service<?, ?> service : getDevice().getServices()) {
                    sb.append(service.getServiceType()).append("\n");
                }

            } else if (device.getType().toString()
                    .equals("urn:dial-multiscreen-org:device:dial:1")) {
                sb.append(getDevice().getDisplayString());
                sb.append("\n\n");
                sb.append(((RemoteDevice) device).getDIALApplicationURL().toString());
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
            if (!device.getType().toString()
                    .contains("urn:dial-multiscreen-org:device:dial:1"))
                return device.isFullyHydrated() ? name : name + " *";
            else
                return name;
        }
    }

    public static void scanNetwork() {
        if (upnpService != null) {
            listAdapter.clear();
            upnpService.getRegistry().removeAllRemoteDevices();
            upnpService.getControlPoint().search();
        }
    }

    private void createNewDevice(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create a dummy UPnP device");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText deviceName = new EditText(this);
        deviceName.setHint("Device Name");
        layout.addView(deviceName);

        final EditText deviceType = new EditText(this);
        deviceType.setHint("Device Type");
        layout.addView(deviceType);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mDeviceName = deviceName.getText().toString();
                String mDeviceType = deviceType.getText().toString();
                Intent intent = new Intent(UPnPScannerActivity.this, DummyDeviceActivity.class);
                intent.putExtra("deviceName",mDeviceName);
                intent.putExtra("deviceType",mDeviceType);
                UPnPScannerActivity.this.startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

}
