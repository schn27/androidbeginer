package luft27.usbterminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import luft27.usbserial.ConnectStateHandler;

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

		broadcastReceiver = createBroadcastReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(broadcastReceiver, new IntentFilter(ACTION_USB_NEW_DATA));

		List<luft27.usbserial.Info> filter = new ArrayList<>();
		filter.add(new luft27.usbserial.Info(pixhawkVendorId, pixhawkProductId));
		manager = new luft27.usbserial.Manager(this, filter, new ConnectStateHandler() {
			@Override
			public void onConnected(String deviceName) {
				status.setText(deviceName);
				startReadThread();
			}

			@Override
			public void onDisconnected() {
				stopReadThread();
				status.setText("");
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		manager.close();
		unregisterReceiver(broadcastReceiver);
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
		}
    }

	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(ACTION_USB_NEW_DATA)) {
					try {
						consoleOutput.append(new String(intent.getByteArrayExtra("data"), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
					}
				}
			}
		};
	}


	private void startReadThread() {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				running = true;
				byte[] buffer = new byte[64];

				while (running) {
					luft27.usbserial.Port port = manager.getPort();

					if (port != null) {
						int n = port.read(buffer, buffer.length, 100);
						if (n > 0) {
							notifyUi(Arrays.copyOfRange(buffer, 0, n));
						}
					} else {
						break;
					}
				}
			}
		});

		thread.start();
	}

	private void notifyUi(byte[] data) {
		Intent intent = new Intent(ACTION_USB_NEW_DATA);
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

	private Thread thread;
	private volatile boolean running;

	private BroadcastReceiver broadcastReceiver;

	private static final String ACTION_USB_NEW_DATA = "luft27.usbterminal.USB_NEW_DATA";
	private static final int pixhawkVendorId = 9900;
	private static final int pixhawkProductId = 17;
}


