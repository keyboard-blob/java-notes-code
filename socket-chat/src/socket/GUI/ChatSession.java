package socket.GUI;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

//一个会话，自己维护消息列表
public class ChatSession {
    private final List<ChatMessage> messages = new ArrayList<>();

    //添加一条文字
    public void addText(String text) {
        messages.add(new ChatMessage(text));
    }

    //添加一张图片
    public void addImage(ImageIcon image) {
        messages.add(new ChatMessage(image));
    }

    //给界面渲染用
    public List<ChatMessage> getMessages() {
        return messages;
    }

    public int size() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
    }
}

//一条消息，要么是文字，要么是图片
class ChatMessage {
    String text;
    ImageIcon image;
    boolean isImage;

    public ChatMessage(String text) {
        this.text = text;
        this.isImage = false;
    }

    public ChatMessage(ImageIcon image) {
        this.image = image;
        this.isImage = true;
    }
}
