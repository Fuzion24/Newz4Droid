package net.usenet.yEnc;

/**
 * Thrown to indicate that some parts of a multipart archive are missing.
 *
 */
public class MissingPartsException extends YEncException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1790856946817020530L;

	public MissingPartsException(String s) {
		super(s);
	}
}
