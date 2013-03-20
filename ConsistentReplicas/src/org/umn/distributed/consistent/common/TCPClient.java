package org.umn.distributed.consistent.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPClient {

	public static byte[] sendData(Socket clientSocket, byte[] data) {
		try {
			clientSocket.getOutputStream().write(data);
//			DataOutputStream outToServer = new DataOutputStream(
//					clientSocket.getOutputStream());
//			DataInputStream outToServer = new DataInputStream(
//					clientSocket.getInputStream());
			byte readBytes[] = new byte[1024];
			byte buffer[] = new byte[1024];
			int count = 0;
			int start = 0;
			while((count = clientSocket.getInputStream().read(readBytes, start, 1024)) != -1) {
				
			}
//			modifiedSentence = inFromServer.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
