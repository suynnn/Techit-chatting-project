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

    public RoomClientInfo(String nickName, BufferedReader in, PrintWriter out) {
        this.nickName = nickName;
        this.in = in;
        this.out = out;
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
}
