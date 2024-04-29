package chatting.server;

import java.io.BufferedReader;
import java.io.PrintWriter;

/*
 * 방에 입장한 유저의 정보를 저장할 클래스 입니다.
 *
 */
public class RoomClientInfo {
    private String nickName;
    private BufferedReader in;
    private PrintWriter out;

    private boolean isRoomManager;

    private boolean isKicked;

    public RoomClientInfo(String nickName, BufferedReader in, PrintWriter out) {
        this.nickName = nickName;
        this.in = in;
        this.out = out;
        this.isRoomManager = false;
        this.isKicked = false;
    }

    public String getNickName() {
        return nickName;
    }

    public BufferedReader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public boolean isRoomManager() {
        return isRoomManager;
    }

    public boolean isKicked() {
        return isKicked;
    }

    public void setRoomManager(boolean roomManager) {
        isRoomManager = roomManager;
    }

    public void setKicked(boolean kicked) {
        isKicked = kicked;
    }
}
