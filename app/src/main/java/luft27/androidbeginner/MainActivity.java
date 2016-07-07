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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<luft27.usbserial.Info> filter = new ArrayList<>();
		filter.add(new luft27.usbserial.Info(9900, 17));
		manager = new luft27.usbserial.Manager(this, filter);
		port = manager.openPort();
	}

	@Override
	protected void onPause() {
		super.onPause();
		setDevice(null);
		manager.close();
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putCharSequence("consoleOutputText", consoleOutput.getText());
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		consoleOutput.setText(state.getCharSequence("consoleOutputText", ""));
	}

    private void onButtonSend(View view) {
		luft27.usbserial.Port port = manager.getPort();

		if (port != null) {
			String cmd = consoleInput.getText().toString() + "\n";
			consoleOutput.append(cmd);
			port.write(cmd.getBytes(), cmd.length(), 0);
			//connection.bulkTransfer(endpointSend, cmd.getBytes(), cmd.length(), 0);
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

	private void startReadThread() {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				byte[] buffer = new byte[64];

				while (running && connection != null) {
					int n = connection.bulkTransfer(endpointReceive, buffer, buffer.length, 100);
					if (n > 0) {
						notifyUi(Arrays.copyOfRange(buffer, 0, n));
					}
				}
			}
		});

		thread.start();
	}

	private void notifyUi(byte[] data) {
		Intent intent = new Intent(ACTION_USB_DATA_RECEIVED);
		intent.putExtra("data", data);
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

	private luft27.usbserial.Manager manager;
	private luft27.usbserial.Port port;

	private Thread thread;
	private volatile boolean running;
}


