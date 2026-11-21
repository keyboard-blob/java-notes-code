package socket.GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

//登录、注册按钮的监听器：连服务器、发命令、看回复
public class UIListener implements ActionListener {

    private int port = 8888;
    private JTextField hostField;
    private JTextField nameField;
    private JPasswordField passwordField;
    private JFrame jFrame;

    public UIListener(JTextField hostField, JTextField nameField, JPasswordField passwordField, JFrame jFrame) {
        this.hostField = hostField;
        this.nameField = nameField;
        this.passwordField = passwordField;
        this.jFrame = jFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        //服务器地址留空就按本机处理
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }

        String name = nameField.getText();
        String password = new String(passwordField.getPassword());

        //命令（command）就是按钮文字：登录 / 注册
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
                    new ChatFrame(name, host, socket, in, out);
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
