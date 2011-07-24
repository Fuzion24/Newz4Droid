package net.usenet.yEnc;

/**
 * Thrown to indicate that a parameter's value is invalid. This can be thrown,
 * for example, if the value needed was a number, but a string was found.
 *
 */
public class InvalidParameterException extends YEncException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2636834477348158504L;

	public InvalidParameterException(String s) {
		super(s);
	}
}
