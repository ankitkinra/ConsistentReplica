package org.umn.distributed.consistent.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;

public class Utils {
	private static Logger logger = Logger.getLogger(Utils.class);

	private static String myIP = null;
	private static final int FORMAT_INDENT = 2;
	private static final String POST_START = BulletinBoard.FORMAT_START
			+ Article.FORMAT_START;

	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}

	public static boolean isNumber(String num) {
		try {
			Integer.parseInt(num);
		} catch (NumberFormatException ne) {
			return false;
		}
		return true;
	}

	public static int findFreePort(int startNumber) {
		while (!isPortAvailable(startNumber)) {
			if (!isValidPort(startNumber)) {
				return -1;
			}
			startNumber++;
		}
		return startNumber;
	}

	public static boolean isValidPort(int port) {
		if (port < 1 || port > 65535) {
			return false;
		}
		return true;
	}

	public static boolean isPortAvailable(int port) {
		if (isValidPort(port)) {
			ServerSocket sSocket = null;
			DatagramSocket dSocket = null;
			try {
				sSocket = new ServerSocket(port);
				sSocket.setReuseAddress(true);
				dSocket = new DatagramSocket(port);
				dSocket.setReuseAddress(true);
				return true;
			} catch (IOException e) {
			} finally {
				if (dSocket != null) {
					dSocket.close();
				}
				if (sSocket != null) {
					try {
						sSocket.close();
					} catch (IOException e) {
						// TODO: handle exception
					}
				}
			}

			return false;
		}
		return false;
	}

	public static String getLocalServerIp() {
		if (myIP != null) {
			return myIP;
		}
		try {
			Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface intf = en.nextElement();
				Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
				while (enumIpAddr.hasMoreElements()) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()) {
						myIP = inetAddress.getHostAddress().toString();
						logger.debug("localhost ip found:" + myIP);
						return myIP;
					}
				}
			}
		} catch (SocketException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		logger.error("cannot find the localhost ip");
		return null;
	}

	//
	// public static String getDataFromPacket(DatagramPacket packet,
	// String encoding) {
	// try {
	// return new String(packet.getData(), packet.getOffset(),
	// packet.getLength(), encoding);
	// } catch (UnsupportedEncodingException e) {
	// e.printStackTrace();
	// logger.error("invalid encoding type: " + encoding);
	// }
	// return null;
	// }

	public static byte[] stringToByte(String str, String encoding) {
		try {
			return str.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			logger.error("invalid encoding type: " + encoding, e);
		}
		return null;
	}

	public static byte[] stringToByte(String str) {
		String encoding = Props.ENCODING;
		if (Props.ENCODING == null) {
			encoding = ClientProps.ENCODING;
		}
		return stringToByte(str, encoding);
	}

	public static String byteToString(byte[] data, String encoding) {
		try {
			if (data != null) {
				return new String(data, encoding);
			} else {
				return null;
			}
		} catch (UnsupportedEncodingException e) {
			logger.error("invalid encoding type: " + encoding, e);
		}
		return null;
	}

	public static String byteToString(byte[] data) {
		String encoding = Props.ENCODING;
		if (Props.ENCODING == null) {
			encoding = ClientProps.ENCODING;
		}
		return byteToString(data, encoding);
	}

	public static List<String> getIndentedArticleList(String str) {
		List<String> articleList = new ArrayList<String>();
		int indent = (-1) * FORMAT_INDENT;
		while (str.length() > 0) {
			if (str.startsWith(POST_START)) {
				indent += FORMAT_INDENT;
				int index = str.indexOf(Article.FORMAT_END);
				if (index < -1) {
					System.out.println("Article list format error");
					break;
				}
				articleList.add(getArticleIndented(str.substring(1, index + 1),
						indent));
				str = str.substring(index + 1);
			} else if (str.startsWith(BulletinBoard.FORMAT_ENDS)) {
				indent -= FORMAT_INDENT;
				if (indent < 0) {
					System.out.println("Article list format error");
					break;
				}
				str = str.substring(1);
			} else {
				System.out.println("Article list format error");
				break;
			}
		}
		return articleList;
	}

	private static String getArticleIndented(String str, int indent) {
		StringBuilder builder = new StringBuilder();
		for (int i = FORMAT_INDENT; i < indent; i++) {
			builder.append(" ");
		}
		if(str.startsWith(BulletinBoard.NULL_ARTICLE_START)) {
			builder.append(str.substring(BulletinBoard.NULL_ARTICLE_START.length() + 1, str.length() - 1)).append(".");
			builder.append("Article details no available at this replica");
		}
		else {
			Article article = Article.parseArticle(str);
			builder.append(article.getId()).append(". ");
			builder.append(article.getTitle()).append("    ").append(article.getContent());
		}
		builder.append(str);
		return builder.toString();
	}
}
