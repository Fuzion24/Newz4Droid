package net.usenet.yEnc;

/**
 * Thrown to indicate an error in the encoded data.
 *
 * @author Luis Parravicini <luis@ktulu.com.ar>
 */
public class YEncException extends Exception {

	private static final long serialVersionUID = -6213460422192692535L;

	public YEncException(String s) {
		super(s);
	}
}
