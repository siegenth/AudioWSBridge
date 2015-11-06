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
//    String uriString = "ws://169.55.246.102:8086"; // TODO IP address needs be a configuration parameter    
    // OnOpen will get called by WebSockets when the connection has been established successfully using WebSocket handshaking with
    // the HTTP Request - Response processing.
    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
    	// Store the WebSocket session for later use.
        currentSession = session;
        clientWebSocket = new ClientWebSocket("FROM onOpen call", uriString);
        clientWebSocket.pause();
    }

    // using the OnMessage annotation for this method will cause this method to get called by WebSockets when this connection has received 
    // a WebSocket message from the other side of the connection.  
    // The message is derived from the WebSocket frame payloads of one, and only one, WebSocket message.
    @OnMessage
    public void receiveMessage(String message) {

        try {
            count++;
            if ((count % SAMPLECOUNT) == 0) {
            	//System.out.println("count:" + count + " message length:" + message.length() + ":" + message.substring(0,Math.min(30, message.length())) + "@AnnotatedEndPoint.reciveMessage");
            }
            
        		String[] messageParts = message.split(SEPARATOR);            	
            	if (uriString == null) {
        			// looking for IP to connect to.
            		if (START.equals(messageParts[COMMAND])) {
            			uriString = messageParts[DATA];  
            			currentId = messageParts[ID];
            	    	System.out.println("ID:" + currentId +", forwarder connection is being established." );    	            			
            		} else {
            			return;   // NOTE - we are not message being sent 
            		}
            	}
                // send the message back to the other side with the iteration count.  Notice we can send multiple message without having
                // to receive messages in between.            	
            	if (clientWebSocket == null) {
            		if (uriString == null) {
            			return; // we do not have a connection. 
            		}
                	System.out.println("ID:" + currentId + ", forwarder connecting: " + uriString );    	            		
            		clientWebSocket = new ClientWebSocket(currentId, uriString);
            		if (!clientWebSocket.ready()) {
                    	System.out.println("ID:" + currentId + " Forwarder failed to connect: " + uriString );    	            		            			
            			clientWebSocket = null;
            			uriString = null;
            		}
            	}
            	clientWebSocket.send(message);
                currentSession.getBasicRemote().sendText("From: " + this.getClass().getSimpleName() + "  Iteration count: " + count);
                currentSession.getBasicRemote().sendText(message);  

            	if ((STOP.equals(messageParts[COMMAND]))) {
                    currentSession.getBasicRemote().sendText("OK. AnnotatedEndpoint stop.");
                    clientWebSocket.shut();  // do not return until done.
                    uriString = null;
                    clientWebSocket = null;
            	}            	
        } catch (IOException ex) {
        	System.err.println(ex.getMessage());
            // no error processing will be done for this sample
        }

    }

    // Using the OnClose annotation will cause this method to be called when the WebSocket Session is being closed.
    @OnClose
    public void onClose(Session session, CloseReason reason) {
    	// The connection has been close, usually occur when the page is left. 
    	System.out.println("ID:" + currentId +", forwarder connection closed." );   
    	clientWebSocket.shut();   // close down the connection.
    	uriString = null;	      
    	clientWebSocket = null;
    }

    // Using the OnError annotation will cause this method to be called when the WebSocket Session has an error to report. For the Alpha version
    // of the WebSocket implentation on Liberty, this will not be called on error conditions.
    @OnError
    public void onError(Throwable t) {
    	System.err.println("ID:" + currentId + ", forwarder onError message: " + t.getMessage() ); 	    	
    }

}
