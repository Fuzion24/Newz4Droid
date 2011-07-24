package net.usenet.NetworkInterface;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

public class NNTPclient {

	private Socket nntpClient;
	private SSLSocket sslNNTPclient;
	private SocketAddress sockAdd = null;
	private BufferedReader nntpReader = null;
	private OutputStream nntpWriter = null;
	private ServerSettings serverSettings = null;
	public Boolean postingAllowed;
	public String status;
	public int KBperSec = 0;
	private Boolean connected = false;
	private Boolean debug = true;

	public NNTPclient(ServerSettings settings) {
		serverSettings = settings;
		
	}

	public NNTPclient() {

		serverSettings = new ServerSettings();
		loadSettings(serverSettings);
	
	}

	@Override
	protected void finalize() {
		disconnect();
	}

	private static void printSocketInfo(SSLSocket s) {
		System.out.println("Socket class: " + s.getClass());
		System.out.println("   Remote address = "
				+ s.getInetAddress().toString());
		System.out.println("   Remote port = " + s.getPort());
		System.out.println("   Local socket address = "
				+ s.getLocalSocketAddress().toString());
		System.out.println("   Local address = "
				+ s.getLocalAddress().toString());
		System.out.println("   Local port = " + s.getLocalPort());
		System.out.println("   Need client authentication = "
				+ s.getNeedClientAuth());
		SSLSession ss = s.getSession();
		System.out.println("   Cipher suite = " + ss.getCipherSuite());
		System.out.println("   Protocol = " + ss.getProtocol());
	}

	public void connect() throws NNTPException {
		if (serverSettings == null) {
			throw new NNTPException("No Server Settings have been initialized");
		}
		NNTPresponse currentRes = null;
		if (serverSettings.requiresSSL == true) {
			SSLSocketFactory f;
			try {
				// Create a trust manager that does not validate certificate chains
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs,
							String authType) {
					}

					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs,
							String authType) {
					}
				} };

				// Install the all-trusting trust manager

				SSLContext sc = SSLContext.getInstance("TLS");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				f = sc.getSocketFactory();
				InetAddress addr = InetAddress.getByName(serverSettings.hostName);
				sockAdd = new InetSocketAddress(addr, serverSettings.port);
				sslNNTPclient = (SSLSocket) f.createSocket();
				sslNNTPclient.connect(sockAdd,serverSettings.connectTimeoutMs);
				if(debug)
					printSocketInfo(sslNNTPclient);
				sslNNTPclient.startHandshake();
				sslNNTPclient.setKeepAlive(true);
				nntpWriter = sslNNTPclient.getOutputStream();
				nntpReader = new BufferedReader(new InputStreamReader(
						sslNNTPclient.getInputStream(), "ISO-8859-1"));

				currentRes = getResponse(null);

				if ((currentRes.Code != 200) && (currentRes.Code != 201)) {
					disconnect();
					throw new NNTPException("Error Connecting to Server: "
							+ currentRes.Info);
				}

				if (currentRes.Code == 200) {
					postingAllowed = true;
				} else {
					postingAllowed = false;
				}

				if (serverSettings.requiresLogin) {
					Authenticate(serverSettings.userName,
							serverSettings.passWord);
				}
			} catch (NNTPException e) {
				throw e;
			} catch (Exception e) {
				throw new NNTPException("Error connecting to server: "
						+ e.toString());
			}

		} else {
			try {
				InetAddress addr = InetAddress
						.getByName(serverSettings.hostName);
				sockAdd = new InetSocketAddress(addr, serverSettings.port);
				nntpClient = new Socket();
				nntpClient.setKeepAlive(true);
				nntpClient.connect(sockAdd, serverSettings.connectTimeoutMs);

				nntpWriter = nntpClient.getOutputStream();

				nntpReader = new BufferedReader(new InputStreamReader(
						nntpClient.getInputStream(), "ISO-8859-1"));

				currentRes = getResponse(null);

				if ((currentRes.Code != 200) && (currentRes.Code != 201)) {
					disconnect();
					throw new NNTPException("Error Connecting to Server: "
							+ currentRes.Info);
				}

				if (currentRes.Code == 200) {
					postingAllowed = true;
				} else {
					postingAllowed = false;
				}

				if (serverSettings.requiresLogin) {
					Authenticate(serverSettings.userName,
							serverSettings.passWord);
				}

			} catch (NNTPException e) {
				throw e;
			} catch (Exception e) {
				throw new NNTPException("Error connecting to server: "
						+ e.toString());
			}
		}
		connected = true;
	}

	Boolean isConnected() {
		if(serverSettings.requiresSSL)
		{
			connected = sslNNTPclient.isConnected();
		}
		else
		{
			connected = nntpClient.isConnected();
		}
		return connected;
	}

	private void loadSettings(ServerSettings settings) {
		/*
		settings.requiresLogin = true;		
		settings.hostName = "news.giganews.com";
	    settings.port = 563; //TODO: WHEN SSL is set to false but port is 563, connection just hangs.
		settings.userName = "accounfuzion1";
		settings.passWord = "un0rn1ll";
		settings.connectTimeoutMs = 10000;
		settings.requiresSSL = true;
		  */
	}

	public void selectGroup(String group) throws NNTPException {
		NNTPresponse res = getResponse("GROUP " + group);
		if (res.Code != 211) {
			throw new NNTPException("Error switching to group: " + res.Info);
		}
	}

	public ByteArrayOutputStream DownloadBody(String articleID)
			throws NNTPException {

		long t0 = System.currentTimeMillis();
		try {
			NNTPresponse res = getResponse("BODY <" + articleID + ">");

			Boolean Done = false;
			String response;
			byte[] newLine = { 13, 10 };
			if (res.Code != 222) {
				throw new NNTPException(res);
			}

			ByteArrayOutputStream bsoStream = new ByteArrayOutputStream(
					256 * 1024); // Allocate 256KB it will grow as necessary

			while (!Done) {
				response = nntpReader.readLine();

				if (response.equals(".")) {
					Done = true;
					break;
				}

				if (response.startsWith("..")) {
					response = response.substring(1);
				}

				bsoStream.write(response.getBytes("ISO-8859-1"));
				bsoStream.write(newLine);
			}
			long t1 = System.currentTimeMillis();
			t1 = t1 - t0;
			KBperSec = (int) (bsoStream.size() / t1);
			return bsoStream;

		} catch (IOException e) {
			throw new NNTPException("Error downloading Article: "
					+ e.toString());
		}

	}

	public void sendLine(String message) throws NNTPException {
		// Ensures that a CR/LF is sent at the end of every line
		message += "\r\n";
		if (debug) {
			System.out.print("Sent: " + message);
		}
		byte[] byteMsg = message.getBytes();

		try {
			nntpWriter.write(byteMsg);
			nntpWriter.flush();
		} catch (IOException e) {

			throw new NNTPException("Error Sending to server: " + e.toString());

		}

	}

	public Boolean Authenticate(String Username, String Password)
			throws NNTPException {

		NNTPresponse res = getResponse("AUTHINFO USER " + Username);

		if (res.Code == 381) // More Authentication Information Required
		{
			res = getResponse("AUTHINFO PASS " + Password);
		}
		if (res.Code != 281) // Authentication rejected
		{
			throw new NNTPException("Error authenticating with server: "
					+ res.Info);
		}

		return true;
	}

	public void disconnect() {
		try {
			NNTPresponse res = getResponse("QUIT");
			if (res.Code != 205) {
				throw new NNTPException("Server did not disconnect cleanly: "
						+ res.Info);
			}
			nntpClient.close();
			nntpWriter = null;
			nntpReader = null;
		} catch (Exception e) {
		}
	}

	private NNTPresponse getResponse(String message) throws NNTPException {

		String lineRead = null;

		try {
			if (message != null) {
				sendLine(message);
			}
			lineRead = nntpReader.readLine();
			status = "Received: " + lineRead;

			if (debug) {
				System.out.println(status);
			}
			return new NNTPresponse(lineRead);
		} catch (NNTPException e) {
			throw e;
		} catch (Exception e) {
			throw new NNTPException("Problem receiving response from server: "
					+ e.toString());
		}

	}

	private class NNTPresponse {

		public int Code;
		public String Info;

		public NNTPresponse(String fullResponse) throws NNTPException {

			try {
				Code = Integer.parseInt(fullResponse.substring(0, 3));
			} catch (NumberFormatException e) {
				throw new NNTPException("Server response not expected: " + Info);
			}
			Info = fullResponse;
		}

		@Override
		public String toString() {
			return Info;
		}
	}

	public class NNTPException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2525901341922310074L;
		String error;
		private int code;
		NNTPException(String errorString) {
			error = errorString;
			setCode(-1);
		}
		NNTPException(NNTPresponse res) {
			error = res.Info;
			setCode(res.Code);
		}

		@Override
		public String toString() {
			return error;
		}
		public int setCode(int code) {
			this.code = code;
			return code;
		}
		public int getCode() {
			return code;
		}
	}

}
