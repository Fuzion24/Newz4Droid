package net.usenet.yEnc;

/**
 * Thrown to indicate that a header had a missing parameter.
 */
public class MissingParameterException extends YEncException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6810977200204058081L;
	private String parameter;
	private String header;

	/**
	 * Constructs a <code>MissingParameterException</code> with the
	 * specified detail message. The others arguments are the name of
	 * the missing parameter and the name of the header to which the
	 * parameter belongs to.
	 */
	public MissingParameterException(String parameter, String header,
	String s) {
		super(s);
		this.parameter = parameter;
		this.header = header;
	}

	/**
	 * Constructs a <code>MissingParameterException</code> with a default
	 * message. The exception message is constructed using the parameter
	 * and header's name.
	 *
	 * @see #MissingParameterException(String, String, String)
	 */
	public MissingParameterException(String parameter, String header) {
		this(parameter, header, parameter+" not specified in "+header);
	}

	/** Returns the missing parameter name. */
	public String getParameterName() {
		return parameter;
	}

	/** Returns the header to which the parameter belongs to. */
	public String getHeaderName() {
		return header;
	}
}
