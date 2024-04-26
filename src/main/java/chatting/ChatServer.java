package chatting;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServer {
    public final static int PORT = 12345;

    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PORT);) {
            System.out.println("서버가 준비되었습니다.");

            Map<String, PrintWriter> chatClients = new HashMap<>();
            Map<String, List<RoomClientInfo>> roomList = new HashMap<>();

            while (true) {
                Socket socket = serverSocket.accept();

                new ServerMainThread(socket, chatClients, roomList).start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
