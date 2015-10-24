package wasdev.sample.websocket.forward;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.ibm.json.java.JSONObject;

public class ClientWebSocket {

	ClientSocket cs;

	public ClientWebSocket(String uriString) {

		URI uri = null;
		try {
			uri = new URI(uriString);
			System.out.println(uri.toString() + ":@ClientWebSockets");
			cs = new ClientSocket(uri);
			this.pause();   // wait for things to settle out.
			// cs.send("<First Message");
			// System.out.println("should have sent a message...");

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * Send the message out via WebSockets.
	 * Last chance to look/modify the data before
	 * it hits WebSockets outbound. 
	 * 
	 */
	public void send(String text) {
		cs.send(text);
		//cs.send("<" + text); // TODO this will allow you to look at all the messages in console.
		
		//System.out.println("send@ClientWebSocket:" + text); 
	}
	// Wait for the user to catchup.....
	public void pause() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ie) {
			// Handle exception
		}
	}
	// this is for testing....
	public static void main(String[] args) {
		ClientWebSocket cws = new ClientWebSocket("ws://172.16.49.153:8086");
		cws.send("This from main!");
	}

}
