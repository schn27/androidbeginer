package luft27.usbserial;

public final class Info {
	public Info(int vendorId, int productId) {
		this.vendorId = vendorId;
		this.productId = productId;
	}

	public final int vendorId;
	public final int productId;
}
