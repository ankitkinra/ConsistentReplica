package org.umn.distributed.consistent.server;

import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
	
	TcpServerDelegate delegate;
	int numThreads = 0;
	public TCPServer(TcpServerDelegate delegate, int numThreads) {
		this.delegate = delegate;
		this.numThreads = numThreads;
	}

	public static void server() {
		ServerSocket welcomeSocket = new ServerSocket(6789);
		Socket connectionSocket = welcomeSocket.
		BufferedReader inFromClient =
             new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
          clientSentence = inFromClient.readLine();
          System.out.println("Received: " + clientSentence);
          capitalizedSentence = clientSentence.toUpperCase() + '\n';
          outToClient.writeBytes(capitalizedSentence);
	}
}
