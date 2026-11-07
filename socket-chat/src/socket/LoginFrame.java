package socket;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField nameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("聊天室 - 登录");
        setSize(300, 140);
        setDefaultCloseOperation(3);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel jPanel = new JPanel(new FlowLayout());
        JLabel nameLabel = new JLabel("昵称", SwingConstants.CENTER);
        JLabel passwordLabel = new JLabel("密码", SwingConstants.CENTER);

        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(230, 20));
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(230, 20));

        JButton loginBtn = new JButton("登录");
        JButton regBtn = new JButton("注册");
        jPanel.add(nameLabel);
        jPanel.add(nameField);
        jPanel.add(passwordLabel);
        jPanel.add(passwordField);
        jPanel.add(loginBtn);
        jPanel.add(regBtn);

        UIListener uiListener = new UIListener(nameField, passwordField, this);

        loginBtn.addActionListener(uiListener);
        regBtn.addActionListener(uiListener);


        add(jPanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        new LoginFrame();
    }
}
