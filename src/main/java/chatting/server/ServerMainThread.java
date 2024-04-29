package chatting.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServerMainThread extends Thread {
    private Socket socket;

    private String nickname;

    private final Map<String, PrintWriter> chatClients;

    private static int roomNumber = 1;
    private final Map<String, List<RoomClientInfo>> roomList;

    private BufferedReader in;

    private PrintWriter out;

    public ServerMainThread(Socket socket, Map<String, PrintWriter> chatClients,
                            Map<String, List<RoomClientInfo>> roomList) {
        this.socket = socket;
        this.chatClients = chatClients;
        this.roomList = roomList;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                nickname = in.readLine();
                if (chatClients.containsKey(nickname)) {
                    out.print("이미 사용 중인 닉네임 입니다. 다시 입력해주세요 : ");

                } else {
                    System.out.println(nickname + " 닉네임의 사용자가 연결했습니다. 해당 사용자의 IP 주소 : "
                            + socket.getInetAddress().getHostAddress());
                    break;
                }
            }

            synchronized (this.chatClients) {
                this.chatClients.put(nickname, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String notice = "\n방 목록 보기 : /list\n" +
                "방 생성 : /create\n" +
                "방 입장 : /join [방번호]\n" +
                "방 나가기 : /exit\n" +
                "접속종료 : /bye\n" +
                "현재 접속중인 유저 목록 확인 : /users\n" +
                "현재 방에 있는 유저 목록 확인 : /roomusers\n" +
                "귓속말 : /whisper [닉네임] [메시지]";

        String msg;
        FileWriter fileWriter = null;

        try {
            out.println(notice);
            out.println("=======================================");

            while ((msg = in.readLine()) != null) {
                if ("/bye".equalsIgnoreCase(msg.trim())) {
                    System.out.println(nickname + " 닉네임의 사용자가 연결을 끊었습니다.");
                    break;

                } else if("/create".equalsIgnoreCase(msg.trim())) {
                    System.out.println(roomNumber + "번 방이 생성되었습니다.");
                    roomList.put(String.valueOf(roomNumber), new ArrayList<>());

                    Thread room = new ServerRoomThread(String.valueOf(roomNumber), new RoomClientInfo(nickname, in ,out), roomList);
                    fileWriter = new FileWriter("room [" + roomNumber + "].txt");

                    out.println(roomNumber++ + "번 방이 생성되었습니다.");
                    room.start();
                    room.join();

                } else if ("/list".equalsIgnoreCase(msg.trim())) {
                    out.println("지금까지 생성된 채팅방 리스트 입니다.");

                    for (String roomName : roomList.keySet()) {
                        out.println("["+roomName+"]");
                    }

                } else if(msg.contains("/join") &&
                        "/join".equalsIgnoreCase(msg.trim().substring(0, 5))
                        && msg.trim().substring(5, 6).equals(" ")) {
                    // TODO 1. 입력한 방 이름이 없을 때의 exception 잡기
                    Thread room = new ServerRoomThread(msg.trim().substring(6), new RoomClientInfo(nickname, in, out), roomList);
                    room.start();
                    room.join();

                } else if("/users".equalsIgnoreCase(msg.trim())) {
                    out.println("현재 서버에 접속 중인 유저 리스트 입니다.");

                    for (String nickname : chatClients.keySet()) {
                        out.println(nickname);
                    }
                    out.println(" ");

                } else if ("/roomusers".equalsIgnoreCase(msg.trim())) {
                    out.println("현재 각 채팅방에 접속한 유저 리스트 입니다.");
                    for (String roomName : roomList.keySet()) {
                        out.println("[" + roomName + "]");

                        for (RoomClientInfo roomClientInfo : roomList.get(roomName)) {
                            out.println(roomClientInfo.getNickName());
                        }
                        out.println(" ");
                    }
                } else if (msg.contains("/whisper") &&
                        "/whisper".equalsIgnoreCase(msg.trim().substring(0, 8))
                        && msg.trim().substring(8, 9).equals(" ")) {
                    msg = msg.trim();
                    int idx = msg.indexOf(" ", 9);
                    String receiver = msg.substring(9, idx);
                    String whisperMsg = msg.substring(idx+1);

                    if (chatClients.containsKey(receiver)) {
                        chatClients.get(receiver).println("[귓속말]"+ nickname + " : " + whisperMsg);
                    } else {
                        out.println("존재하지 않는 유저 입니다. 다시 확인해 주세요.");
                    }
                } else {
                    out.println("잘못된 명령어 입니다. 다시 입력해 주세요");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            synchronized (chatClients) {
                chatClients.remove(nickname);
            }

            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }


            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }
}
