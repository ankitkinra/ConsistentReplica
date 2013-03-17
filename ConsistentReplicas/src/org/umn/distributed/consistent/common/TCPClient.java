package org.umn.distributed.consistent.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPClient {

	public static byte[] sendData(Socket clientSocket, byte[] data) {
		String modifiedSentence;
		try {
			clientSocket.getOutputStream().write(data);
//			DataOutputStream outToServer = new DataOutputStream(
//					clientSocket.getOutputStream());
//			
//			BufferedInputStream inFromServer = new BufferedInputStream(
//					new InputStreamReader(clientSocket.getInputStream()));
//			clientSocket.getInputStream().read(arg0)
//			modifiedSentence = inFromServer.read();
			return modifiedSentence;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
