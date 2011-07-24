package net.usenet.yEnc;

import java.io.*;
import java.util.StringTokenizer;
import java.util.zip.CRC32;

/**
 * Decodes data encoded with yEnc
 * (<a href=http://www.yencode.org>http://www.yencode.org</a>).
 * It supports both single and multipart archives. The method to decode the
 * files is the following:
 * <p><strong>For single part archives</strong>:<br>
 * <ol>
 * <li>Set the input stream using <code>setInputStream(InputStream)</code>.</li>
 * <li>Set the output stream using
 * <code>setOutputStream(OutputStream)</code>.</li>
 * <li>Call <code>decode()</code>.</li>
 * </ol>
 * (Note: It doesn't matter if step 1 or 2 is done first, but both must be done
 * before step 3).</p>
 * <p><strong>For multi part archives</strong>:<br>
 * <ol>
 * <li>Set the output stream using <code>setOutputStream(OutputStream)</code>.
 * </li>
 * <li>Set the input stream for the next part
 * <code>setInputStream(InputStream, false)</code>.</li>
 * <li>Call <code>decode()</code>.</li>
 * <li>If there are more parts left go to step 2.</li>
 * </ol>
 * What's missing in the last steps is to take all the files that make the
 * multipart archive and put them in the correct order. Code to do this and
 * also to make some integrity tests on each part can be found in
 * <code>ar.com.ktulu.yenc.ydecode.sortFiles(YEncDecoder, String[])</code>.</p>
 * <p><strong>Note that this class is not synchronized.</strong></p>
 * <br>
 * <br>
 * <strong>BUGS</strong>:
 * <ul>
 * <li>If there are spaces between the parameter name and it's value, the
 * parameter is not accepted. I think this case is not very common but should
 * be fixed anyway.</li>
 * </ul>
 *
 * @see <a href=http://www.yencode.org>yEncode</a>
 * @see #setInputStream(InputStream)
 * @see #setInputStream(InputStream, boolean)
 * @see #setOutputStream(OutputStream)
 * @see #decode()
 * @see ydecode#sortFiles(YEncDecoder, String[])
 */
public class YEncDecoder {

	private InputStream in;
	private OutputStream out;
	private char[] line;
	private char idx, max;
	private int linenum;
	private boolean decode_data;
	private long written, total_written;
	private boolean debug;
	private String name, value;
	private boolean header_found, part_found, trailer_found;
	// header data
	private Integer line_length;
	private Long header_size, trailer_size;
	private String filename;
	private Integer part, total_parts;
	// trailer data
	private Long crc_value, pcrc_value;
	private CRC32 crc, pcrc;
	// part data
	private Long pbegin, pend;
	/** Maximum line length allowed */
	public int MAX_LENGTH;

	/**
	 * Creates a new instance which can read lines up to 1024 characters of
	 * length.
	 */
	public YEncDecoder() {
		this(1024);
	}

	/**
	 * Creates a new instance which can read lines up to
	 * <code>maxlength</code> characters of length.
	 */
	public YEncDecoder(int maxlength) {
		MAX_LENGTH = maxlength;
		line = new char[MAX_LENGTH];
		crc = new CRC32();
		pcrc = new CRC32();
	}

	/**
	 * Resets the instance internal state. It calls
	 * <code>reset(true)</code>.
	 *
	 * @see #reset(boolean)
	 */
	public void reset() {
		reset(true);
	}
	
	/**
	 * Resets the instance internal state. This is used to be able to decode
	 * several files withouth the need of creating more instances. If
	 * <code>resetcrc</code> is <code>true</code>, then the CRC computed so
	 * far is also reset.
	 */
	public void reset(boolean resetcrc) {
		header_found = part_found = trailer_found = false;
		idx = max = 0;
		written = total_written = 0;
		linenum = 0;
		decode_data = false;
		// data from header
		line_length = null;
		header_size = null;
		part = total_parts = null;
		filename = null;
		// data from trailer
		trailer_size = null;
		crc_value = pcrc_value = null;
		if (resetcrc) crc.reset();
		pcrc.reset();
		// data from part
		pbegin = pend = null;
	}

	protected boolean getProperty(String s) {
		int pos = s.indexOf("=");

		if (pos == -1) return false;
		value = s.substring(pos+1);
		name = s.substring(0, pos);
		return true;
	}

	/**
	 * Converts the string <code>s</code> to an <code>Integer</code>. In
	 * case an error occurs, a <code>YEncException</code> will be thrown
	 * using <code>name</code> as part of the exception message.
	 */
	protected Integer strToInt(String s, String name)
	throws InvalidParameterException {
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException("value supplied " +
				"for " + name + " is not a number");
		}
	}

	/**
	 * Calls <code>strToLong()</code> with radix equal to 10.
	 *
	 * @see #strToLong(String, int, String)
	 */
	protected Long strToLong(String s, String name)
	throws InvalidParameterException {
		return strToLong(s, 10, name);
	}

	/**
	 * Converts the string <code>s</code> to a <code>Long</code> in the
	 * specified radix. In case an error occurs, a
	 * <code>YEncException</code> will be thrown using <code>name</code>
	 * as part of the exception message.
	 */
	protected Long strToLong(String s, int radix, String name)
	throws InvalidParameterException {
		try {
			return Long.valueOf(s, radix);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException("value supplied " +
				"for " + name + " is not a number");
		}
	}

	/**
	 * Process the next parameter from the header. A call to
	 * <code>getProperty(String)</code> must be made before calling this
	 * method. The tokenizer is needed as the filename parameter eats up
	 * everything from the parameter definition till the end of the line.
	 */
	protected void processHeaderArg(StringTokenizer tokenizer)
	throws InvalidParameterException {
		if (name.equals("total"))
			total_parts = strToInt(value, "total parts");
		else
		if (name.equals("part"))
			part = strToInt(value, "part number");
		else
		if (name.equals("line"))
			line_length = strToInt(value, "line length");
		else
		if (name.equals("size"))
			header_size = strToLong(value, "file size");
		else
		if (name.equals("name")) {
			String s = new String(line, 0, max);
			// get the filename and strip the = character
			filename = s.substring(s.indexOf("name")+4).trim().
				substring(1).trim();
			while (tokenizer.hasMoreElements())
				tokenizer.nextToken();
			return;
		}
	}

	protected void processHeader(StringTokenizer tokenizer)
	throws InvalidParameterException, MissingParameterException {
		if (header_found) {
			if (debug)
				System.out.println(linenum+": duplicate " +
					"header ignored");
			return;
		}
		while (tokenizer.hasMoreElements() &&
		getProperty(tokenizer.nextToken()))
			processHeaderArg(tokenizer);
		if (header_size == null)
			throw new MissingParameterException("size", "header");
		if (line_length == null)
			throw new MissingParameterException("line length",
				"header");
		if (filename == null)
			throw new MissingParameterException("filename",
				"header");
		header_found = true;
		if (debug)
			System.out.println(linenum+": header found: line=" +
				line_length + " size=" + header_size +
				" name=[" + filename + "] total=" +
				total_parts + " part=" + part+ " calculated crc: " + pcrc.getValue());
	}

	/**
	 * Process the next parameter from the trailer. A call to
	 * <code>getProperty(String)</code> must be made before calling this
	 * method.
	 */
	protected void processTrailerArg() throws InvalidParameterException {
		if (name.equals("size"))
			trailer_size = strToLong(value, "file size in trailer");
		else
		if (name.equals("crc32"))
			crc_value = strToLong(value, 16, "crc32");
		else
		if (name.equals("pcrc32"))
			pcrc_value = strToLong(value, 16, "pcrc32");
	}

	protected void processTrailer(StringTokenizer tokenizer)
	throws InvalidParameterException, MissingParameterException {
		if (trailer_found) {
			if (debug)
				System.out.println(linenum +
					": duplicate trailer ignored");
			return;
		}
		while (tokenizer.hasMoreElements() &&
		getProperty(tokenizer.nextToken()))
			processTrailerArg();
		if (trailer_size == null)
			throw new MissingParameterException("size", "trailer");
		trailer_found = true;
		if (debug) System.out.println(linenum+": trailer found: size="+
			trailer_size+" crc32="+
			(crc_value != null ? Long.toHexString(
				crc_value.longValue()).toUpperCase() : "") +
			" pcrc32=" +
			(pcrc_value != null ? Long.toHexString(
				pcrc_value.longValue()).toUpperCase() : ""));
	}

	/**
	 * Process the next parameter from the part header. A call to
	 * <code>getProperty(String)</code> must be made before calling this
	 * method.
	 */
	protected void processPartArg() throws InvalidParameterException {
		if (name.equals("begin"))
			pbegin = strToLong(value, "part beginning offset");
		else
		if (name.equals("end"))
			pend = strToLong(value, "part ending offset");
	}

	protected void processPart(StringTokenizer tokenizer)
	throws IOException, InvalidParameterException,
	MissingParameterException {
		if (part_found) {
			if (debug)
				System.out.println(linenum+": duplicate part " +
					"ignored");
			return;
		}
		while (tokenizer.hasMoreElements() &&
		getProperty(tokenizer.nextToken()))
			processPartArg();
		if (pbegin == null)
			throw new MissingParameterException("beginning offset",
				"part");
		if (pend == null)
			throw new MissingParameterException("ending offset",
				"part");
		part_found = true;
		if (debug)
			System.out.println(linenum + ": part found: pbegin=" +
				pbegin + " pend=" + pend);
	}

	/**
	 * Reads the next line from the input stream. Maximum line length
	 * acepted is <code>MAX_LENGTH</code> bytes. The line terminator is
	 * "CR*LF" (actually, the CRs are ignored and LF is the line
	 * terminator).<br>
	 * If <code>skip</code> is <code>true</code>, it returns a line with
	 * encoded data or with a header, everything else is skipped. The
	 * skipped lines are not subject to the maximum line length mentioned
	 * above; i.e., all headers and encoded data lines' length must be
	 * less than <code>MAX_LENGTH</code> but the rest of the lines can be
	 * of any length.<br>
	 * <strong>Note</strong> that if before the encoded data there is text
	 * that starts like a yEnc header ("=y"), then that line will have to
	 * conform to the requeriment mentionend before.
	 *
	 * @see #MAX_LENGTH
	 */
	protected boolean readNextLine(boolean skip)
	throws IOException, YEncException {
		int c;
		boolean skipLine = false;

		idx = max = 0;
		while ((c=in.read()) != -1) {
			if (c == '\r') continue;
			if (c == '\n') {
				linenum++;
				if (!skipLine)
					break;
				else {
					skipLine = false;
					continue;
				}
			}
			if (skipLine) continue;
			if (max == MAX_LENGTH)
				throw new YEncException("line too long");
			line[max++] = (char)c;
			// if we are not yet decoding, skip everything
			// but the headers.
			if (!decode_data && skip)
				if (max == 2 && !(line[0] == '=' &&
				line[1] == 'y')) {
					max = 0;
					skipLine = true;
					continue;
				}
		}
		if (c == -1) return false;
		return true;
	}

	/**
	 * Decodes the next character. Returns <code>false</code> when EOF is
	 * found.
	 */
	protected boolean decodeNext() throws IOException, YEncException {

		if (idx == max)
			if (!readNextLine(true)) return false;

		if (max > 1 && line[0] == '=' && line[1] == 'y') {
			StringTokenizer st = new StringTokenizer(
				new String(line, 0, max), " \t");
			if (st.countTokens() < 2) {
				idx = max = 0;
				return true;
			}
			String header = st.nextToken();
			if (header.equals("=ybegin"))
				processHeader(st);
			else
			if (header.equals("=yend"))
				processTrailer(st);
			else
			if (header.equals("=ypart"))
				processPart(st);
			idx = max = 0;
			decode_data =
				total_parts != null &&
				total_parts.intValue() > 1 &&
				header_found && part_found
				||
				(total_parts == null ||
					total_parts.intValue() <= 1) &&
				header_found;
			return true;
		}
		if (decode_data) {
			if (line[idx] == '=') {
				if (idx == max-1)
					throw new YEncException("escape " +
						"character found but as last " +
						"character in a line");
				line[++idx] = (char)((line[idx] - 64) % 255);
			}
			char c = (char)((line[idx] - 42) % 255);
			out.write(c);
			crc.update(c);
			pcrc.update(c);
			written++;
			total_written++;
		}
		idx++;
		return true;
	}

	/**
	 * Read encoded data from the stream set with
	 * <code>setInputStream</code> and writes decoded data to the stream
	 * set with <code>setOutputStream</code>. If you haven't used those
	 * two methods mentioned to set the input/output streams before
	 * calling this method, it will fail.
	 *
	 * @see #setInputStream(InputStream)
	 * @see #setOutputStream(OutputStream)
	 */
	public void decode() throws IOException, YEncException {
		while (decodeNext());
		if (header_found) {
			if (!trailer_found)
				throw new YEncException("missing trailer");
			if (part_found) {
				if (trailer_size.longValue() != written)
					throw new SizeException("size " +
						"mismatch (trailer=" +
						trailer_size + "/read=" +
						written + ")");
				//TODO:  Something is wrong with the logic below.  It doesn't work with multipart files on the trailer... 
				/*
				if (part != null && total_parts != null &&
				part.intValue() == total_parts.intValue() &&
				header_size.longValue() != total_written)
					throw new SizeException("size " +
						"mismatch (header=" +
						trailer_size + "/total read=" +
						written + ")");*/
			} else
			if (header_size.longValue() != written ||
			header_size.longValue() != trailer_size.longValue())
				throw new SizeException("size mismatch " +
					"(header=" + header_size +
					"/trailer=" + trailer_size + "/read=" +
					written + ")");
			if (crc_value != null && crc.getValue() !=
			crc_value.longValue())
				throw new CRCException("crc mismatch (" +
					Long.toHexString(
					crc_value.longValue()).toUpperCase() +
					"/" +
					Long.toHexString(
					crc.getValue()).toUpperCase() + ")");
			if (pcrc_value != null && pcrc.getValue() !=
			pcrc_value.longValue())
				throw new CRCException("pcrc mismatch (" +
					Long.toHexString(
					pcrc_value.longValue()).toUpperCase() +
					"/" +
					Long.toHexString(
					pcrc.getValue()).toUpperCase() + ")");
		}
		out.flush();
	}

	/**
	 * Enables or disables debugging messages to <code>System.out</code>.
	 */
	public void debugMessages(boolean enable) {
		debug = enable;
	}

	/**
	 * Returns if debug messages are enabled or not.
	 */
	public boolean debugMessages() {
		return debug;
	}

	/**
	 * Returns the filename set in the header. If the filename is not yet
	 * known, it scans the stream until it finds the header. If no headers
	 * are found, returns <code>null</code>.
	 */
	public String getFileName() throws IOException, YEncException {
		while (!header_found && decodeNext());
		return filename;
	}

	/**
	 * Returns the file size set in the header. If the file size is not
	 * yet known, it scans the stream until it finds the header. If no
	 * headers are found returns -1.
	 */
	public long getSize() throws IOException, YEncException {
		while (!header_found && decodeNext());
		return (header_size == null ? -1 : header_size.longValue());
	}

	/**
	 * Returns the current part number set in the header. If the part
	 * number is not yet known, it scans the stream until it finds the
	 * header. If no headers are found or no part number is set,
	 * returns -1.
	 */
	public int getPartNumber() throws IOException, YEncException {
		while (!header_found && decodeNext());
		return (part == null ? -1 : part.intValue());
	}

	/**
	 * Returns the total parts number set in the header. If the total
	 * parts are not yet known, it scans the stream until it finds the
	 * header. If no headers are found or no total parts is set,
	 * returns -1.
	 */
	public int getTotalParts() throws IOException, YEncException {
		while (!header_found && decodeNext());
		return (total_parts == null ? -1 : total_parts.intValue());
	}

	/**
	 * Returns <code>true</code> if the current input stream is a
	 * multipart archive.
	 */
	public boolean isMultiPart() throws IOException, YEncException {
		return (getPartNumber() > 0);
	}

	/**
	 * Returns the beginning offset of the current part. If the offset is
	 * not yet known, it scans the stream until it finds the part
	 * header. If no part header is found returns -1.
	 */
	public int getPartBegin() throws IOException, YEncException {
		while (!part_found && decodeNext());
		return (pbegin == null ? -1 : pbegin.intValue());
	}

	/**
	 * Returns the ending offset of the current part. If the offset is
	 * not yet known, it scans the stream until it finds the part
	 * header. If no part header is found returns -1.
	 */
	public int getPartEnd() throws IOException, YEncException {
		while (!part_found && decodeNext());
		return (pend == null ? -1 : pend.intValue());
	}

	/**
	 * Returns the actual line number. It's also the number of lines read
	 * from the input stream.
	 */
	public int getLineNumber() {
		return linenum;
	}

	/**
	 * Sets the input stream from which encoded data is read. Setting the
	 * input stream resets the instance internal state. It calls
	 * <code>setInputStream(InputStream, false)</code>.
	 *
	 * @see #setInputStream(InputStream, boolean)
	 */
	public void setInputStream(InputStream in) {
		setInputStream(in, false);
	}

	/**
	 * Sets the input stream from which encoded data is read.
	 * <code>isNextPart</code> determines whether the new input stream is
	 * a new archive or the next part for the previous stream.<br>
	 * You may want to use some kind of buffered input for better
	 * performance.
	 */
	public void setInputStream(InputStream in, boolean isNextPart) {
		if (isNextPart) {
			long aux = total_written;
			reset(false);
			total_written = aux;
		} else
			reset();
		this.in = in;
	}

	/**
	 * Sets the input stream to which decoded data is written.
	 */
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

}
