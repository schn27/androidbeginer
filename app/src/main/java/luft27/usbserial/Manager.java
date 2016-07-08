package luft27.usbserial;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.view.View;
import android.widget.ScrollView;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import luft27.androidbeginner.R;

public class Manager {
	public Manager(Activity activity, List<Info> filter) {
		this.activity = activity;
		this.filter = filter;
		usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		broadcastReceiver = createBroadcastReceiver();
		permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		open();
		connectToAnyDevice();
	}

	public void close() {
		activity.unregisterReceiver(broadcastReceiver);
	}

	private void open() {
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DEVICE_ATTACHED));
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DEVICE_DETACHED));
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DATA_RECEIVED));
	}

	private void connectToAnyDevice() {
		Map<String, UsbDevice> deviceList = usbManager.getDeviceList();
		if (deviceList != null && !deviceList.isEmpty()) {
			for (UsbDevice d : deviceList.values()) {
				if (isAcceptable(d)) {
					usbManager.requestPermission(d, permissionIntent);
				}
			}
		}
	}

	private boolean isAcceptable(UsbDevice d) {
		if (filter == null || filter.isEmpty()) {
			return true;
		}

		for (Info info : filter) {
			if (d.getVendorId() == info.vid && d.getProductId() == info.pid) {
				return true;
			}
		}

		return false;
	}

	private void connect(UsbDevice device) {
		if (this.device != null) {
			stopReadThread();

			if (connection != null) {
				connection.releaseInterface(iface);
				connection.close();
				connection = null;
			}

			endpointSend = null;
			endpointReceive = null;
			iface = null;
		}

		this.device = device;

		if (device != null) {
			status.setText(device.getDeviceName());

			iface = device.getInterface(1);
			connection = usbManager.openDevice(device);
			boolean res = connection.claimInterface(iface, true);

			endpointSend = iface.getEndpoint(0);
			endpointReceive = iface.getEndpoint(1);

			startReadThread();
		} else {
			status.setText("");
		}
	}

	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(ACTION_USB_PERMISSION)) {
					UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						setDevice(device);
					}
				} else if (action.equals(ACTION_USB_DEVICE_ATTACHED)) {
					UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					usbManager.requestPermission(device, permissionIntent);
				} else if (action.equals(ACTION_USB_DEVICE_DETACHED)) {
					setDevice(null);
				} else if (action.equals(ACTION_USB_DATA_RECEIVED)) {
					try {
						consoleOutput.append(new String(intent.getByteArrayExtra("data"), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
					}
					((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
				}
			}
		};
	}


	private final Activity activity;
	private final List<Info> filter;
	private final UsbManager usbManager;
	private BroadcastReceiver broadcastReceiver;
	private PendingIntent permissionIntent;


	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	private static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
	private static final String ACTION_USB_DATA_RECEIVED = "luft27.androidbeginner.action.USB_DATA_RECEIVED";
}
