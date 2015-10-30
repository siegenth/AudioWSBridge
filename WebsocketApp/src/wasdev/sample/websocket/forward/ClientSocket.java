package wasdev.sample.websocket.forward;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

class ClientSocket extends WebSocketClient {

    ClientSocket(URI uri) {
        super(uri);
        this.connect();
        System.out.println("connected@ClientSocket-forward");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("onMessage@ClientSocket:-forward" + message);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("onOpen@ClientSocket-forward");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("onClose@ClientSocket-forward");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}