package luft27.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

/**
 * Created by amalikov on 07.07.2016.
 */
public class Port {
	public Port(UsbManager usbManager, UsbDevice device) {
		this.usbManager = usbManager;
		this.device = device;
	}

	public void open() {
		iface = device.getInterface(1);
		connection = usbManager.openDevice(device);
		boolean res = connection.claimInterface(iface, true);

		endpointSend = iface.getEndpoint(0);
		endpointReceive = iface.getEndpoint(1);
	}

	public void close() {
		if (connection != null) {
			connection.releaseInterface(iface);
			connection.close();
			connection = null;
		}

		endpointSend = null;
		endpointReceive = null;
		iface = null;
	}

	public int read(byte[] buffer, int length, int timeout) {
		return connection.bulkTransfer(endpointReceive, buffer, buffer.length, timeout);
	}

	public int write(byte[] buffer, int length, int timeout) {
		return connection.bulkTransfer(endpointSend, buffer, length, timeout);
	}

	private final UsbManager usbManager;
	private UsbDevice device;
	private UsbInterface iface;
	private UsbEndpoint endpointSend;
	private UsbEndpoint endpointReceive;
	private UsbDeviceConnection connection;
}
