package net.usenet.yEnc;

/**
 * Thrown to indicate a difference in the expected file size. This can happen
 * for a part of a multipart archive or for the total size of a file when the
 * total number of bytes written is different from the size in the headers.
 *
 * @author Luis Parravicini <luis@ktulu.com.ar>
 */
public class SizeException extends YEncException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6331885680283801380L;

	public SizeException(String s) {
		super(s);
	}
}
