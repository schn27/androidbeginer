package luft27.usbterminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
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

		input = (EditText) findViewById(R.id.consoleInput);
		output = (TextView) findViewById(R.id.consoleOutput);
		output.setMovementMethod(new ScrollingMovementMethod());

		terminalEmulator = new TerminalEmulator(output);

		input.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
					onInputDone();
				}
				return false;
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
				setTitle(getText(R.string.app_name) + " (" + deviceName + ")");
				startReadThread();
			}

			@Override
			public void onDisconnected() {
				stopReadThread();
				setTitle(R.string.app_name);
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
		state.putCharSequence("consoleOutputText", output.getText());
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		terminalEmulator.put(state.getCharSequence("consoleOutputText", "").toString());
	}

    private void onInputDone() {
		luft27.usbserial.Port port = manager.getPort();

		if (port != null) {
			String cmd = input.getText().toString() + "\n";
			input.setText("");
			port.write(cmd.getBytes(), cmd.length(), 0);
		} else {
			terminalEmulator.put("1\n2\n3\n4\n5\n6\n7\n8\n9\na\nb\nc\nd\ne\nf\ng\nh\ni\nj\n");
		}
    }

	private BroadcastReceiver createBroadcastReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(ACTION_USB_NEW_DATA)) {
					try {
						terminalEmulator.put(new String(intent.getByteArrayExtra("data"), "UTF-8"));
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

	private EditText input;
	private TextView output;
	private TerminalEmulator terminalEmulator;

	private static final String TAG = "MainActivity";

	private luft27.usbserial.Manager manager;

	private Thread thread;
	private volatile boolean running;

	private BroadcastReceiver broadcastReceiver;

	private static final String ACTION_USB_NEW_DATA = "luft27.usbterminal.USB_NEW_DATA";
	private static final int pixhawkVendorId = 9900;
	private static final int pixhawkProductId = 17;
}


