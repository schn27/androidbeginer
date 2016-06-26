package luft27.androidbeginner;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		status = (TextView) findViewById(R.id.status);
		consoleInput = (EditText) findViewById(R.id.consoleInput);
		consoleOutput = (TextView) findViewById(R.id.consoleOutput);

		((Button) findViewById(R.id.buttonSend)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onButtonSend(v);
			}
		});

		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		createBroadcastReceiver();
	}

	@Override
	protected void onPause() {
		super.onPause();
		setDevice(null);
	}

    private void onButtonSend(View view) {
		connectDevice();

		if (connection != null) {
			String cmd = consoleInput.getText().toString() + "\n";
			consoleOutput.append(cmd);
			connection.bulkTransfer(endpointSend, cmd.getBytes(), cmd.length(), 0);
		}
    }

	private void createBroadcastReceiver() {
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(ACTION_USB_PERMISSION)) {
					UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						setDevice(device);
					}
				} else if (action.equals(ACTION_USB_DEVICE_DETACHED)) {
					setDevice(null);
				} else if (action.equals(ACTION_USB_DATA_RECEIVED)) {
					consoleOutput.append(intent.getStringExtra("text"));
					((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
				}
			}
		};

		permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

		registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));
		registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DEVICE_DETACHED));
		registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_DATA_RECEIVED));
	}

	private void connectDevice() {
		if (connection != null) {
			return;
		}

		int vendorId = 9900;
		int productId = 17;

		Map<String, UsbDevice> deviceList = usbManager.getDeviceList();
		if (deviceList != null && !deviceList.isEmpty()) {
			for (UsbDevice d : deviceList.values()) {
				if (d.getVendorId() == vendorId && d.getProductId() == productId) {
					usbManager.requestPermission(d, permissionIntent);
				}
			}
		}
	}

	private void setDevice(UsbDevice device) {
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
			status.setText(String.format("VID=%04x PID=%04x\n",
					device.getVendorId(), device.getProductId()));

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

	private void startReadThread() {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				byte[] buffer = new byte[64];
				StringBuilder line = new StringBuilder();

				while (running && connection != null) {
					int n = connection.bulkTransfer(endpointReceive, buffer, 64, 100);

					for (int i = 0; i < n; ++i) {
						line.append((char)buffer[i]);
						if (buffer[i] == '\n') {
							notifyUi(line.toString());
							line = new StringBuilder();
						}
					}

					if (n <= 0 && line.length() > 0) {
						notifyUi(line.toString());
						line = new StringBuilder();
					}
				}
			}
		});

		thread.start();
	}

	private void notifyUi(String text) {
		Intent intent = new Intent(ACTION_USB_DATA_RECEIVED);
		intent.putExtra("text", text);
		sendBroadcast(intent);
	}

	private void stopReadThread() {
		running = false;
		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}

			thread = null;
		}
	}

    private TextView status;
	private EditText consoleInput;
	private TextView consoleOutput;

	private static final String TAG = "MainActivity";
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	private static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
	private static final String ACTION_USB_DATA_RECEIVED = "luf27.androidbeginner.action.USB_DATA_RECEIVED";

	private UsbManager usbManager;
	private UsbDevice device;
	private UsbInterface iface;
	private UsbEndpoint endpointSend;
	private UsbEndpoint endpointReceive;
	private volatile UsbDeviceConnection connection;
	private BroadcastReceiver broadcastReceiver;
	private PendingIntent permissionIntent;

	private Thread thread;
	private volatile boolean running;
}


