package luft27.usbserial;

/**
 * Created by amalikov on 07.07.2016.
 */
public final class Info {
	public Info(int vid, int pid) {
		this.vid = vid;
		this.pid = pid;
	}

	public final int vid;
	public final int pid;
}
