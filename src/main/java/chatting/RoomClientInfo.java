package chatting;

import java.io.BufferedReader;
import java.io.PrintWriter;

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
