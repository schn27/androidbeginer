package luft27.usbserial;

/**
 * Created by amalikov on 02.08.2016.
 */
public interface ConnectStateHandler {
	void onConnected(String deviceName);
	void onDisconnected();
}
