/*
	Copyright (C) 2002-2003  Luis Parravicini <luis@ktulu.com.ar>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at Your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


package net.usenet.yEnc;

/**
 * Thrown to indicate a CRC mismatch. This mismatch can be either on the CRC of
 * the whole file or the CRC of a part.
 *
 * @author Luis Parravicini <luis@ktulu.com.ar>
 */
public class CRCException extends YEncException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5459728393516182407L;

	public CRCException(String s) {
		super(s);
	}
}
