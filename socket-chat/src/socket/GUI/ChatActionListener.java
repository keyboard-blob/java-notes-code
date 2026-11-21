package socket.GUI;

import socket.file.FileSendTask;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

//按钮监听器：发送、发文件、表情三个按钮共用，靠动作命令（actionCommand）区分
public class ChatActionListener implements ActionListener {

    public static final String SEND = "发送";
    public static final String FILE = "文件";
    public static final String EMOJI = "表情";

    private final ChatFrame frame;

    public ChatActionListener(ChatFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command == null) {
            return;
        }
        switch (command) {
            case SEND:
                sendMessage();
                break;
            case FILE:
                sendFile();
                break;
            case EMOJI:
                sendEmoji();
                break;
            default:
                break;
        }
    }

    private void sendMessage() {
        String msg = frame.getInputField().getText();
        if (msg.isEmpty()) {
            return;
        }
        if ("群聊".equals(frame.getCurrentChat())) {
            frame.getOut().println(frame.getUserName() + ":" + msg);
            frame.appendLocalText("(我) " + msg + "\n");
        } else {
            frame.getOut().println(frame.getUserName() + ":@" + frame.getCurrentChat() + " " + msg);
            frame.appendLocalText("(我)" + frame.getUserName() + ":" + msg + "\n");
        }
        frame.getInputField().setText("");
    }

    //文件传输放后台线程（避免卡住界面）
    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selectedFile = chooser.getSelectedFile();
        System.out.println("发送文件: " + selectedFile.getAbsolutePath());
        ProgressDialog progressDialog=new ProgressDialog(frame,"正在上传文件...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileSendTask.sendFile(frame.getHost(), 9999, selectedFile, frame.getUserName(), frame.getCurrentChat(), new FileSendTask.ProgressCallback() {
                    @Override
                    public void onProgress(int percent) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.updateProgress(percent);
                            }
                        });
                    }
                });
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dispose();
                        frame.appendLocalText("(系统)成功发送了文件：" + selectedFile.getName() + "\n");
                    }
                });
            }
        }).start();
        progressDialog.setVisible(true);
    }

    private void sendEmoji() {
        frame.showImagePicker();
    }
}
