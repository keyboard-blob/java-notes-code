package socket.GUI;

import javax.swing.*;
import java.awt.*;
import java.io.File;

//登录窗口：服务器地址、昵称、密码，加登录和注册两个按钮
public class LoginFrame extends JFrame {

    private JTextField hostField;
    private JTextField nameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("聊天室 - 登录");
        if (new File("icon.png").exists()) {
            setIconImage(new ImageIcon("icon.png").getImage());
        }
        setSize(300, 150);
        setDefaultCloseOperation(3);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel jPanel = new JPanel();
        JLabel hostLabel = new JLabel("IP址", SwingConstants.CENTER);
        JLabel nameLabel = new JLabel("昵称", SwingConstants.CENTER);
        JLabel passwordLabel = new JLabel("密码", SwingConstants.CENTER);

        hostField = new JTextField("127.0.0.1");
        hostField.setPreferredSize(new Dimension(230, 20));
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(230, 20));
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(230, 20));

        JButton loginBtn = new JButton("登录");
        JButton regBtn = new JButton("注册");

        jPanel.add(hostLabel);
        jPanel.add(hostField);
        jPanel.add(nameLabel);
        jPanel.add(nameField);
        jPanel.add(passwordLabel);
        jPanel.add(passwordField);
        jPanel.add(loginBtn);
        jPanel.add(regBtn);

        //一个监听器（listener 实例）管两个按钮，靠按钮上的文字区分
        UIListener uiListener = new UIListener(hostField, nameField, passwordField, this);

        loginBtn.addActionListener(uiListener);
        regBtn.addActionListener(uiListener);


        add(jPanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        new LoginFrame();
    }
}
