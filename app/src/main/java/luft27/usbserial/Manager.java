package luft27.usbserial;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.List;
import java.util.Map;

public class Manager {
	public Manager(Activity activity, List<Info> filter, ConnectStateHandler connectStateHandler) {
		this.activity = activity;
		this.filter = filter;
		this.connectStateHandler = connectStateHandler;
		usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
		broadcastReceiver = createBroadcastReceiver();
		permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
		open();
		connectToAnyDevice();
	}

	public void close() {
		setDevice(null);
		activity.unregisterReceiver(broadcastReceiver);
	}

	public Port getPort() {
		return port;
	}

	private void open() {
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DEVICE_ATTACHED));
		activity.registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DEVICE_DETACHED));
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
			if (d.getVendorId() == info.vendorId && d.getProductId() == info.productId) {
				return true;
			}
		}

		return false;
	}

	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(ACTION_USB_PERMISSION)) {
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						setDevice((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
					}
				} else if (action.equals(ACTION_USB_DEVICE_ATTACHED)) {
					usbManager.requestPermission((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE), permissionIntent);
				} else if (action.equals(ACTION_USB_DEVICE_DETACHED)) {
					setDevice(null);
				}
			}
		};
	}

	private void setDevice(UsbDevice device) {
		if (port != null) {
			connectStateHandler.onDisconnected();
			port.close();
			port = null;
		}

		usbDevice = device;

		if (usbDevice != null) {
			port = new Port(usbManager, usbDevice);
			connectStateHandler.onConnected(usbDevice.getDeviceName());
		}
	}

	private final Activity activity;
	private final List<Info> filter;
	private final ConnectStateHandler connectStateHandler;
	private final UsbManager usbManager;
	private BroadcastReceiver broadcastReceiver;
	private PendingIntent permissionIntent;
	private UsbDevice usbDevice;
	private Port port;

	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	private static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
}
