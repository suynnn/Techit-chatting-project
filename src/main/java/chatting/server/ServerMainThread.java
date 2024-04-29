package chatting.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * 메인 서버 스레드 입니다.
 *
 */
public class ServerMainThread extends Thread {
    private Socket socket;

    private String nickname;

    private final Map<String, PrintWriter> chatClients;

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

            // 닉네임 중복 체크
            while (true) {
                nickname = in.readLine();
                if (chatClients.containsKey(nickname)) {
                    out.println("이미 사용 중인 닉네임 입니다. 다시 입력해주세요 : ");

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
        // 명령어 안내 사항
        String notice = "\n방 목록 보기 : /list\n" +
                "방 생성 : /create\n" +
                "방 입장 : /join [방번호]\n" +
                "방 나가기 : /exit\n" +
                "접속종료 : /bye\n" +
                "현재 접속중인 유저 목록 확인 : /users\n" +
                "현재 방에 있는 유저 목록 확인 : /roomusers\n" +
                "귓속말 : /whisper [닉네임] [메시지]\n" +
                "닉네임 변경 : /changename";

        String msg;
        FileWriter fileWriter = null;

        try {
            out.println(notice);
            out.println("=======================================");

            while ((msg = in.readLine()) != null) {
                // 접속 종료
                if ("/bye".equalsIgnoreCase(msg.trim())) {
                    System.out.println(nickname + " 닉네임의 사용자가 연결을 끊었습니다.");
                    out.println("접속을 종료합니다. ");
                    break;

                }
                // 채팅방 생성
                else if("/create".equalsIgnoreCase(msg.trim())) {

                    out.println("생성할 방 제목을 입력해 주세요 : ");
                    String roomName = in.readLine();

                    roomList.put(roomName, new ArrayList<>());

                    // 채팅방 스레드 생성
                    Thread room = new ServerRoomThread(roomName, new RoomClientInfo(nickname, in ,out), roomList);
                    // 대화 내용을 저장할 txt 파일 생성
                    fileWriter = new FileWriter("room [" + roomName + "].txt");

                    out.println(roomName + " 방이 생성되었습니다.");
                    System.out.println(roomName + " 방이 생성되었습니다.");

                    // 현재 실행 중인 메인 서버 스레드에서의 작업을 잠시 멈추고 채팅방 스레드의 작업 시작
                    room.start();
                    room.join();

                }
                // 채팅방 목록 확인
                else if ("/list".equalsIgnoreCase(msg.trim())) {
                    out.println("지금까지 생성된 채팅방 리스트 입니다.");

                    for (String roomName : roomList.keySet()) {
                        out.println("["+roomName+"]");
                    }

                }
                // 채팅방 입장
                else if(msg.contains("/join") &&
                        "/join".equalsIgnoreCase(msg.trim().substring(0, 5))
                        && msg.trim().substring(5, 6).equals(" ")) {

                    // 채팅방 존재 유무 판별
                    if (!roomList.containsKey(msg.trim().substring(6))) {
                        out.println("존재하지 않는 방 이름입니다. 다시 입력해주세요.");
                        continue;
                    }

                    // 채팅방 스레드 생성
                    Thread room = new ServerRoomThread(msg.trim().substring(6), new RoomClientInfo(nickname, in, out), roomList);

                    // 현재 실행 중인 메인 서버 스레드에서의 작업을 잠시 멈추고 채팅방 스레드의 작업 시작
                    room.start();
                    room.join();

                }
                // 현재 접속중인 모든 유저 목록 확인 (방에 들어가 있는 유저 포함)
                else if("/users".equalsIgnoreCase(msg.trim())) {
                    out.println("현재 서버에 접속 중인 유저 리스트 입니다.");

                    for (String nickname : chatClients.keySet()) {
                        out.println(nickname);
                    }
                    out.println(" ");

                }
                // 현재 각 채팅방에 접속해 있는 유저 목록 확인 (방에 들어가 있지 않은 유저 제외)
                else if ("/roomusers".equalsIgnoreCase(msg.trim())) {
                    out.println("현재 각 채팅방에 접속한 유저 리스트 입니다.");
                    for (String roomName : roomList.keySet()) {
                        out.println("[" + roomName + "]");

                        for (RoomClientInfo roomClientInfo : roomList.get(roomName)) {
                            out.println(roomClientInfo.getNickName());
                        }
                        out.println(" ");
                    }
                }
                // 귓속말
                else if (msg.contains("/whisper") &&
                        "/whisper".equalsIgnoreCase(msg.trim().substring(0, 8))
                        && msg.trim().substring(8, 9).equals(" ")) {
                    msg = msg.trim();
                    int idx = msg.indexOf(" ", 9);
                    String receiver = msg.substring(9, idx);
                    String whisperMsg = msg.substring(idx+1);

                    if (chatClients.containsKey(receiver)) {
                        chatClients.get(receiver).println("[귓속말]"+ nickname + " : " + whisperMsg);
                        out.println("[귓속말]"+ nickname + " : " + whisperMsg);
                    } else {
                        out.println("존재하지 않는 유저 입니다. 다시 확인해 주세요.");
                    }
                }
                // 닉네임 변경
                else if ("/changename".equalsIgnoreCase(msg.trim())) {
                    out.println("변경하실 닉네임을 작성해 주세요 :");
                    String newNickname = in.readLine();

                    chatClients.put(newNickname, chatClients.get(nickname));
                    chatClients.remove(nickname);

                    System.out.println(nickname + "님의 닉네임이 변경되었습니다 -> " + newNickname);
                    out.println(nickname + "님의 닉네임이 변경되었습니다 -> " + newNickname);

                    setNickname(newNickname);

                }
                // 명령어를 제대로 입력하지 않았을 시
                else {
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

    private void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
