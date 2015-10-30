package wasdev.sample.websocket.backward;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Manage the WS connection, to Streams.
 */
public class ClientWebSocket {

	private static final int TIMEOUT = 10;
	private static final int ONESEC = 1000;
	ClientSocket clientSocket;

	public ClientWebSocket(String uriString, BackwardResponse resp) {

		URI uri = null;
		try {
			uri = new URI(uriString);
			System.out.println(uri.toString() + "connecting:@ClientWebSockets");
			clientSocket = new ClientSocket(uri, resp);
			this.pause(); // wait for things to settle out.
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Send the message out via WebSockets. Last chance to look/modify the data
	 * before it hits WebSockets outbound.
	 * 
	 */
	public void send(String text) {
		clientSocket.send(text);
	}

	public void close() {
		System.out.println("close@ClientWebSockets");
		clientSocket.close();
	}

	// Wait for the user to catchup.....
	public void pause() {
		try {
			Thread.sleep(ONESEC);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	/**
	 * Shut down the connection, wait until it's done.
	 * 
	 * @return false if failed to close, otherwise
	 */
	public boolean shut() {
		clientSocket.close();
		for (int idx = 0; (idx++ < TIMEOUT) && !clientSocket.isClosed(); pause())
			;
		if (!clientSocket.isClosed()) {
			System.err.println("Failed to close the WS connection");
			return false;
		}
		return true;
	}

	/**
	 * Wait for ws to be ready.
	 * 
	 * @return false if it never came ready.
	 */
	public boolean ready() {
		for (int idx = 0; (idx++ < TIMEOUT) && !clientSocket.isOpen(); pause())
			;
		return clientSocket.isOpen();
	}

	// this is for testing....


}
