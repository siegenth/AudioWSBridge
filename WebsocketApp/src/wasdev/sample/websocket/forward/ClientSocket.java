package wasdev.sample.websocket.forward;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

class ClientSocket extends WebSocketClient {
	String currentId;
    ClientSocket(String currentId, URI uri) {
        super(uri);
        this.currentId = currentId;
        this.connect();
        System.out.println("ID: " + currentId  + ", forwarder ClientSocket connecting");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("ID: " + currentId  + ", forwarder ClientSocket @onMessage -" + message);    	        
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("ID: " + currentId  + ", forwarder ClientSocket @onOpen.");    	
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("ID: " + currentId  + ", forwarder ClientSocket @onClose.");    	    	
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("ID: " + currentId  + ", forwarder ClientSocket @onError.");    	    	    	
        ex.printStackTrace();
    }
}