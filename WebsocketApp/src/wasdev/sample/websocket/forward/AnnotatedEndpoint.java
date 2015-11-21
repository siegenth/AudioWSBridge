/*
 * COPYRIGHT LICENSE: This information contains sample code provided in source code form.
 * You may copy, modify, and distribute these sample programs in any form without payment
 * to IBM for the purposes of developing, using, marketing or distributing application
 * programs conforming to the application programming interface for the operating platform
 * for which the sample code is written.
 * 
 * Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON 
 * AN "AS IS" BASIS AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING,
 * BUT NOT LIMITED TO, ANY IMPLIED WARRANTIES OR CONDITIONS OF MERCHANTABILITY,
 * SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, AND ANY WARRANTY OR
 * CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF
 * THE SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.
 * 
 * (C) Copyright IBM Corp. 2013.
 * All Rights Reserved. Licensed Materials - Property of IBM.
 */

package wasdev.sample.websocket.forward;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

// The ServerEndpoint annotation value is the name of the WebSocket Endpoint for this application/endpoint. 
// JavaScript to access from a WebSocket capable browser would be:  ws://<Host Name>:<port>/<Context-Root>/SimpleAnnotated
@ServerEndpoint(value = "/Forward")

public class AnnotatedEndpoint {

	private static final String STOP = "STOP";
	private static final String START = "START";
	private static final String SEPARATOR = "#";
	private static final int ID = 1;
	private static final int COMMAND = 2;
	private static final int DATA = 3;
	private static final int SAMPLECOUNT = 10;

	Session currentSession = null;
	String currentId;
	int count = 0;
	ClientWebSocket clientWebSocket = null;
	String uriString = null;
	
	
	public void msgToClient(String msg) {
		try {
			if (!currentSession.isOpen()) {
				System.err.println("currentSession.isOpen() == FALSE");
				clientWebSocket.close();
			} else {
				currentSession.getBasicRemote().sendText(msg);
			}
		} catch (IOException e) {
			System.err.println("Connection to client error - close Streams connection:" + e.getMessage());
		}
	}	
	/**
	 * TODO duplicated code, move into common path.
	 * 
	 * @param status
	 *            0 == success, all others fail.
	 * @param message
	 * @return
	 */
	String buildStatus(int status, String message) {
		String msg = "[PROXY-forward]" + message;
		if (status == 0) {
			System.out.println("STATUS-OK:" + msg);		
		} else {
			System.err.println("STATUS ERR[" + status + "]:" + msg);					
		}
		return ("{\"status\":" + status + ",\"message\":\"" + msg + "\"}");
	}
	
	// OnOpen will get called by WebSockets when the connection has been
	// established successfully using WebSocket handshaking with
	// the HTTP Request - Response processing.
	@OnOpen
	public void onOpen(Session session, EndpointConfig ec) {
		// Store the WebSocket session for later use.
		currentSession = session;
		clientWebSocket = new ClientWebSocket("FROM onOpen call", uriString);
		clientWebSocket.pause();
	}


	// using the OnMessage annotation for this method will cause this method to
	// get called by WebSockets when this connection has received
	// a WebSocket message from the other side of the connection.
	// The message is derived from the WebSocket frame payloads of one, and only
	// one, WebSocket message.
	@OnMessage
	public void receiveMessage(String message) {
			count++;
			if ((count % SAMPLECOUNT) == 0) {
				// System.out.println("count:" + count + " message length:" +
				// message.length() + ":" + message.substring(0,Math.min(30,
				// message.length())) + "@AnnotatedEndPoint.reciveMessage");
			}

			String[] messageParts = message.split(SEPARATOR);
			if (uriString == null) {
				// looking for IP to connect to.
				if (START.equals(messageParts[COMMAND])) {
					uriString = messageParts[DATA];
					currentId = messageParts[ID];
					msgToClient(buildStatus(0, "START ID:" + currentId + " URI:" + uriString));
				} else {
					return; // no message sent
				}
			}
			// send the message back to the other side with the iteration count.
			// Notice we can send multiple message without having
			// to receive messages in between.
			if (clientWebSocket == null) {
				if (uriString == null) {
					msgToClient(buildStatus(4, "no Streams URL"));					
					return; // we do not have a connection.
				}
				System.out.println("Connecting ID:" + currentId + " to:" + uriString);				
				msgToClient(buildStatus(0, "Connecting ID:" + currentId + " to:" + uriString));													
				clientWebSocket = new ClientWebSocket(currentId, uriString);
				if (!clientWebSocket.ready()) {
					msgToClient(buildStatus(2, "ID:" + currentId + " Failed to connect to: " + uriString));
					clientWebSocket = null;
					uriString = null;
					return;
				}
				msgToClient(buildStatus(0, "connected to : " + uriString));									
			}
			if ((STOP.equals(messageParts[COMMAND]))) {
				msgToClient(buildStatus(0, "STOP Request processing"));
				clientWebSocket.shut(); // do not return until done.
				uriString = null;
				clientWebSocket = null;
				return;
			}
			clientWebSocket.send(message);
			msgToClient(buildStatus(0, "MSG COUNT: " + this.getClass().getSimpleName() + "  Iteration count: " + count));
			


	}

	// Using the OnClose annotation will cause this method to be called when the
	// WebSocket Session is being closed.
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		// The connection has been close, usually occur when the page is left.
		msgToClient(buildStatus(0, "CLOSING ID : " + currentId));
		clientWebSocket.shut(); // close down the connection.
		uriString = null;
		clientWebSocket = null;
	}

	// Using the OnError annotation will cause this method to be called when the
	// WebSocket Session has an error to report. For the Alpha version
	// of the WebSocket implentation on Liberty, this will not be called on
	// error conditions.
	@OnError
	public void onError(Throwable t) {
		System.err.println("ID:" + currentId + ", forwarder onError message: " + t.getMessage());
		msgToClient(buildStatus(6, "ERROR thrown : " + t.getMessage()));
	}



}
