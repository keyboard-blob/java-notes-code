package socket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatFrame extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendBtn;
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ChatFrame(String name, Socket socket, BufferedReader in, PrintWriter out) {
        this.name = name;
        this.socket = socket;
        this.in = in;
        this.out = out;

        setTitle("聊天室 - " + name);
        setSize(new Dimension(500, 400));
        setDefaultCloseOperation(3);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(chatArea);
        add(jScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendBtn = new JButton("发送");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        Thread receiveThread=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg;
                    while ((msg = in.readLine())!=null){
                        String finalMsg = msg;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                chatArea.append(finalMsg+"\n");
                                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                            }
                        });
                    }
                }catch (Exception e){
                    System.out.println("与服务器断开连接");
                }
            }
        });
        receiveThread.start();

        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg=inputField.getText();
                if (!msg.isEmpty()){
                    out.println(name + ":" + msg);
                    inputField.setText("");
                }
            }
        });
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendBtn.doClick();
            }
        });

        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                out.print("bye");
                try {
                    socket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                System.exit(0);
            }
        });

        setVisible(true);
    }
}
