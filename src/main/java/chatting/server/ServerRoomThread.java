package chatting.server;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * 채팅방 스레드 클래스 입니다.
 * 채팅방이 처음 만들어지거나 유저들이 참석할 때 생성됩니다.
 *
 */
public class ServerRoomThread extends Thread {

    private final String roomName;
    RoomClientInfo roomClientInfo;
    private final Map<String, List<RoomClientInfo>> roomClients;

    private PrintWriter fileWriter;

    public ServerRoomThread(String roomName, RoomClientInfo roomClientInfo, Map<String, List<RoomClientInfo>> roomClients) {
        this.roomName = roomName;
        this.roomClientInfo = roomClientInfo;
        this.roomClients = roomClients;

        try {
            fileWriter = new PrintWriter(new FileWriter("room [" + roomName + "].txt", true), true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (this.roomClients) {
            this.roomClients.get(roomName).add(roomClientInfo);
        }
    }

    @Override
    public void run() {
        BufferedReader in = roomClientInfo.getIn();
        PrintWriter out = roomClientInfo.getOut();

        out.println("=======================================");
        broadcast(roomClientInfo.getNickName() + "님이 방에 입장했습니다.");

        String msg;
        try {
            while ((msg = in.readLine()) != null) {
                // 채팅방에서 나가기
                if ("/exit".equalsIgnoreCase(msg.trim())) {
                    break;

                }
                // 채팅방 전용 귓속말
                else if (msg.contains("/whisper") &&
                        "/whisper".equalsIgnoreCase(msg.trim().substring(0, 8))
                        && msg.trim().substring(8, 9).equals(" ")) {
                    msg = msg.trim();
                    int idx = msg.indexOf(" ", 9);
                    String receiver = msg.substring(9, idx);
                    String whisperMsg = msg.substring(idx+1);

                    RoomClientInfo receiverInfo = roomClients.get(roomName)
                            .stream()
                            .filter(msgReceiver -> msgReceiver.getNickName().equals(receiver))
                            .findAny()
                            .orElse(null);

                    if (receiverInfo != null) {
                        receiverInfo.getOut().println("[귓속말]"+ roomClientInfo.getNickName() + " : " + whisperMsg);
                        out.println("[귓속말]"+ roomClientInfo.getNickName() + " : " + whisperMsg);
                    } else {
                        out.println("현재 채팅방 안에 존재하지 않는 유저 입니다. 다시 확인해 주세요.");
                    }

                }
                // 채팅방에 /bye 입력하면 접속이 종료되는 문제 방지
                else if ("/bye".equals(msg.trim())) {
                    broadcast(roomClientInfo.getNickName() + " : " + msg);
                    fileWriter.println(roomClientInfo.getNickName() + " : " + msg);
                }
                else {
                    broadcast(roomClientInfo.getNickName() + " : " + msg);
                    fileWriter.println(roomClientInfo.getNickName() + " : " + msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            broadcast(roomClientInfo.getNickName() + "님이 방을 나갔습니다.");
            out.println("=======================================");

            synchronized (roomClients) {
                roomClients.get(roomName).remove(roomClientInfo);

                if (roomClients.get(roomName).size() == 0) {
                    roomClients.remove(roomName);

                    System.out.println("방 [" + roomName + "]가 삭제되었습니다.");
                }
            }

            fileWriter.close();

        }
    }

    public void broadcast(String msg) {

        synchronized (roomClients) {
            Iterator<RoomClientInfo> it = roomClients.get(roomName).iterator();

            while (it.hasNext()) {
                PrintWriter out = it.next().getOut();
                try {
                    out.println(msg);
                } catch (Exception e) {
                    it.remove(); // 브로드케스트 할 수 없는 사용자를 제거한다.
                    e.printStackTrace();
                }
            }
        }
    }
}
