package socket.GUI;


import socket.file.FileSendTask;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatFrame extends JFrame {

    private String host;
    int filePort = 9999;

    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendBtn;
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private Map<String, ChatSession> chatHistoryMap = new HashMap<>();
    private String currentChat = "群聊";

    public ChatFrame(String name, String host, Socket socket, BufferedReader in, PrintWriter out) {
        this.name = name;
        this.host = host;
        this.socket = socket;
        this.in = in;
        this.out = out;

        setTitle("聊天室 - " + name);
        if (new File("icon.png").exists()) {
            setIconImage(new ImageIcon("icon.png").getImage());
        }
        setSize(new Dimension(500, 400));
        setDefaultCloseOperation(3);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        if (listModel.getSize() > 0)userList.setSelectedIndex(0);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScrollPane, chatScrollPane);
        splitPane.setDividerLocation(150);
        add(splitPane, BorderLayout.CENTER);

        JPanel Panel = new JPanel(new BorderLayout());
        inputField = new JTextField();

        sendBtn = new JButton("发送");
        JButton emojiBtn = new JButton("表情");
        JButton fileBtn = new JButton("发文件");

        JPanel bottomPanel = new JPanel(new BorderLayout());

        bottomPanel.add(sendBtn, BorderLayout.EAST);
        bottomPanel.add(emojiBtn, BorderLayout.CENTER);
        bottomPanel.add(fileBtn, BorderLayout.WEST);

        Panel.add(inputField, BorderLayout.CENTER);
        Panel.add(bottomPanel, BorderLayout.EAST);
        add(Panel, BorderLayout.SOUTH);

        //接收线程只读不改（改控件必须回到界面线程）
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        final String msg2 = msg;

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                processIncomingMessage(msg2);
                            }
                        });
                    }
                } catch (Exception e) {
                    System.out.println("与服务器断开连接");
                }
            }
        });
        receiveThread.start();

        userList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selected = userList.getSelectedValue();
                    if (selected == null) return;

                    currentChat = selected;
                    setTitle("聊天室 - " + name + " (当前: " + currentChat + ")");

                    //重新渲染整个聊天界面（先清空，再按顺序重放）
                    chatPane.setText("");
                    ChatSession session = chatHistoryMap.getOrDefault(currentChat, new ChatSession());
                    for (ChatMessage message : session.getMessages()) {
                        if (message.isImage) {
                            appendImageToChatArea(message.image);
                        } else {
                            appendToChatArea(message.text);
                        }
                    }
                }
            }
        });

        ChatActionListener actionListener = new ChatActionListener(this);

        sendBtn.setActionCommand(ChatActionListener.SEND);
        sendBtn.addActionListener(actionListener);

        emojiBtn.setActionCommand(ChatActionListener.EMOJI);
        emojiBtn.addActionListener(actionListener);

        fileBtn.setActionCommand(ChatActionListener.FILE);
        fileBtn.addActionListener(actionListener);

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendBtn.doClick();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                out.println("bye");
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

    private void processIncomingMessage(String msg) {
        if (msg.contains("[EMOJI]")) {
            int idx = msg.indexOf("[EMOJI]");
            String emojiId = msg.substring(idx + 7).trim();
            String prefix = msg.substring(0, idx);

            //先判断这条表情属于哪个会话
            String chatKey = currentChat;
            if (msg.startsWith("(群聊消息)")) {
                chatKey = "群聊";
            } else if (msg.startsWith("(私聊消息)")) {
                String content = msg.substring(6);
                int colon = content.indexOf(":");
                if (colon > -1) chatKey = content.substring(0, colon);
            }

            //文字部分进记录，表情图也进记录（重放时按顺序还原）
            session(chatKey).addText(prefix);
            ImageIcon icon = loadEmoji(emojiId);
            if (icon != null) {
                session(chatKey).addImage(icon);
            } else {
                session(chatKey).addText("[表情]\n");
            }

            if (chatKey.equals(currentChat)){
                appendToChatArea(prefix);
                if (icon != null) {
                    appendImageToChatArea(icon);
                } else {
                    appendToChatArea("(系统)找不到表情:" + emojiId + "\n");
                }
            }
        }else if (msg.startsWith("USERS ")) {
            String namesSrt = msg.substring(6);
            listModel.clear();
            listModel.addElement("群聊");
            String[] nameList = namesSrt.split(",");
            for (String n : nameList) {
                if (!n.equals(name)) listModel.addElement(n);
            }
            if (userList.getSelectedIndex() == -1 || !listModel.contains(userList.getSelectedValue())) {
                userList.setSelectedIndex(0);
            }
        } else if (msg.startsWith("(群聊消息)")) {
            session("群聊").addText(msg + "\n");
            if ("群聊".equals(currentChat)) {
                appendToChatArea(msg + "\n");
            }
        } else if (msg.startsWith("(私聊消息)")) {
            String content = msg.substring(6);
            int idx = content.indexOf(":");
            if (idx > -1) {
                String sender = content.substring(0, idx);
                session(sender).addText(msg + "\n");
                if (sender.equals(currentChat)) {
                    appendToChatArea(msg + "\n");
                }
            }
        } else if (msg.startsWith("[FILE]\u0001")) {
            processIncomingFile(msg);
        }
    }

    //收到的文件消息先确认再选保存位置，最后交给后台线程下载
    private void processIncomingFile(String msg) {
        String[] parts = msg.split("\u0001");

        if (parts.length == 6) {
            String target = parts[1];
            String senderName = parts[2];
            String fileName = parts[3];
            String fileSize = parts[4];
            String saveFileName = parts[5];
            String displayMsg = "(文件) " + senderName + "发送了文件: " + fileName + "(" + fileSize + " 字节)";

            String chatKey;
            if ("群聊".equals(target)) chatKey = "群聊";
            else chatKey = senderName.equals(name)?target:senderName;

            session(chatKey).addText(displayMsg + "\n");

            if (chatKey.equals(currentChat)) {
                appendToChatArea(displayMsg + "\n");
            }
            if (!senderName.equals(name)) {
                int choice = JOptionPane.showConfirmDialog(ChatFrame.this, senderName + " 发送了文件: " + fileName + "\n是否接收? ", "文件传输", JOptionPane.YES_NO_OPTION);


                if (choice == JOptionPane.YES_OPTION) {
                    System.out.println("准备开始下载文件: " + saveFileName);

                    JFileChooser saveChooser = new JFileChooser();
                    saveChooser.setSelectedFile(new File(fileName));
                    int saveResult = saveChooser.showSaveDialog(ChatFrame.this);

                    if (saveResult == JFileChooser.APPROVE_OPTION) {
                        File destFile = saveChooser.getSelectedFile();

                        ProgressDialog downloadDialog = new ProgressDialog(ChatFrame.this, "正在下载文件...");

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                boolean isDownload = FileSendTask.downloadFile(host, filePort, saveFileName, destFile, new FileSendTask.ProgressCallback() {
                                    @Override
                                    public void onProgress(int percent) {
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                downloadDialog.updateProgress(percent);
                                            }
                                        });
                                    }
                                });
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        downloadDialog.dispose();
                                        if (isDownload) {
                                            appendLocalText("(系统) 文件已保存至：" + destFile.getAbsolutePath() + "\n");
                                        } else {
                                            appendLocalText("(系统) 文件保存失败\n");
                                        }
                                    }
                                });
                            }
                        }).start();
                        downloadDialog.setVisible(true);
                    }
                }

            }

        }
    }

    //按会话名取会话，没有就新建一个
    private ChatSession session(String chatKey) {
        return chatHistoryMap.computeIfAbsent(chatKey, key -> new ChatSession());
    }

    //本地产生的文字（自己发的、系统提示）显示出来并记进当前会话
    public void appendLocalText(String text) {
        appendToChatArea(text);
        session(currentChat).addText(text);
    }

    public void appendToChatArea(String text) {
        try {
            javax.swing.text.Document doc = chatPane.getDocument();
            doc.insertString(doc.getLength(), text, null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendImageToChatArea(ImageIcon icon) {
        StyledDocument doc = chatPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        try {
            StyleConstants.setIcon(attrs, icon);
            doc.insertString(doc.getLength(), " ", attrs);
            doc.insertString(doc.getLength(), "\n", null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    //只负责把表情画到界面上（重放历史时也走这里）
    public void showEmojiOnUi(String emojiId) {
        ImageIcon icon = loadEmoji(emojiId);
        if (icon == null) {
            appendToChatArea("(系统)找不到表情:emoji/" + emojiId + ".jpg\n");
            return;
        }
        appendImageToChatArea(icon);
    }

    private ImageIcon loadEmoji(String emojiId) {
        String path = "emoji/" + emojiId + ".jpg";
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() <= 0) {
            return null;
        }
        icon.setImage(icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH));
        return icon;
    }

    public void showImagePicker() {
        JDialog dialog = new JDialog(this, "选择表情", true);
        dialog.setLayout(new FlowLayout());

        //自动扫描 emoji 目录，有几个显示几个（支持 jpg 和 png）
        File[] files = new File("emoji").listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                int dot = fileName.lastIndexOf('.');
                if (dot <= 0) continue;
                String ext = fileName.substring(dot + 1).toLowerCase();
                if (!"jpg".equals(ext) && !"png".equals(ext)) continue;

                final String id = fileName.substring(0, dot);
                ImageIcon icon = new ImageIcon(file.getPath());
                if (icon.getIconWidth() <= 0) continue;

                icon.setImage(icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH));
                JButton button = new JButton(icon);
                button.setPreferredSize(new Dimension(50, 50));
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sendEmojiById(id);
                        dialog.dispose();
                    }
                });
                dialog.add(button);
                count++;
            }
        }

        if (count == 0) {
            JOptionPane.showMessageDialog(this, "没找到表情图（emoji 目录）");
            return;
        }
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    //自己发一个表情：先发给服务器，再显示，在这里只存一次记录
    public void sendEmojiById(String id) {
        String prefix = "(我) " + name + ":";
        if ("群聊".equals(currentChat)) out.println(name + ":[EMOJI]" + id);
        else out.println(name + ":@" + currentChat + " [EMOJI]" + id);

        appendToChatArea(prefix);
        session(currentChat).addText(prefix);

        ImageIcon icon = loadEmoji(id);
        if (icon != null) {
            appendImageToChatArea(icon);
            session(currentChat).addImage(icon);
        } else {
            appendToChatArea("(系统)找不到表情:emoji/" + id + ".jpg\n");
        }
    }

    public String getUserName() {
        return name;
    }

    public String getCurrentChat() {
        return currentChat;
    }

    public String getHost() {
        return host;
    }

    public PrintWriter getOut() {
        return out;
    }

    public JTextField getInputField() {
        return inputField;
    }

    public JTextPane getChatPane() {
        return chatPane;
    }

    public Map<String, ChatSession> getChatHistoryMap() {
        return chatHistoryMap;
    }
}
