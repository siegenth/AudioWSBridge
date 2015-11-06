package wasdev.sample.websocket.backward;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

class ClientSocket extends WebSocketClient {
	BackwardResponse backwardResponse;
    ClientSocket(URI uri, BackwardResponse backwardResponse) {
		
        super(uri);
        this.backwardResponse = backwardResponse;        
        this.connect();
        System.out.println("connected@ClientSocket--backward:");
    }

    @Override
    public void onMessage(String message) {
    	backwardResponse.responseFromStreams(message);
    //    System.out.println("onMessage@ClientSocket-backward:" + message);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("onOpen@ClientSocket-backward:");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("onClose@ClientSocket-backward:");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}