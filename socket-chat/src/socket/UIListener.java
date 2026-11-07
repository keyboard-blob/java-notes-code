package socket;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class UIListener implements ActionListener {

    private int port = 8888;
    private String host = "127.0.0.1";
    private JTextField nameField;
    private JPasswordField passwordField;
    private JFrame jFrame;

    public UIListener(JTextField nameField, JPasswordField passwordField, JFrame jFrame) {
        this.nameField = nameField;
        this.passwordField = passwordField;
        this.jFrame = jFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String name = nameField.getText();
        String password = new String(passwordField.getPassword());

        String command = e.getActionCommand();

        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(jFrame, "昵称或密码不能为空");
            return;
        }

        try {

            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            if ("登录".equals(command)) {

                out.println("LOGIN " + name + " " + password);

                String reply = in.readLine();
                if ("登录成功".equals(reply)) {
                    JOptionPane.showMessageDialog(jFrame, reply);
                    jFrame.dispose();
                    new ChatFrame(name,socket,in,out);
                } else {
                    JOptionPane.showMessageDialog(jFrame, reply);
                    socket.close();
                }


            } else if ("注册".equals(command)) {

                out.println("REG " + name + " " + password);

                String reply = in.readLine();
                if ("注册成功".equals(reply)) {
                    JOptionPane.showMessageDialog(jFrame, reply);
                    socket.close();
                } else {
                    JOptionPane.showMessageDialog(jFrame, reply);
                    socket.close();
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(jFrame, "无法连接到服务器");
        }
    }
}
