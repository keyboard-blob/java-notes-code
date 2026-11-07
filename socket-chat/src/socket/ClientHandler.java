package socket;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private Socket socket;
    private UserManager userManager;

    public ClientHandler(Socket socket, UserManager userManager) {
        this.socket = socket;
        this.userManager = userManager;
    }

    private static Map<String, PrintWriter> onlineClients = new ConcurrentHashMap<>();

    @Override
    public void run() {
        PrintWriter out = null;
        String clientName = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String firstLine = in.readLine();
            if (firstLine == null) return;
            if (firstLine.startsWith("LOGIN ")) {
                String[] parts = firstLine.split(" ");
                if (parts.length != 3) {
                    out.println("格式错误，登录失败");
                    return;
                }
                String name = parts[1];
                String password = parts[2];
                UserData user = userManager.login(name, password);
                if (user == null) {
                    out.println("登录失败，密码或昵称错误");
                    return;
                }
                clientName = user.getName();
                PrintWriter oldOut=onlineClients.putIfAbsent(clientName,out);
                if (oldOut!=null){
                    out.println("登录失败，该账号已在别处登录");
                    return;
                }
                out.println("登录成功");
            } else if (firstLine.startsWith("REG ")) {
                String[] parts = firstLine.split(" ");
                if (parts.length != 3) {
                    out.println("格式错误，注册失败");
                    return;
                }
                String name = parts[1];
                String password = parts[2];
                UserData newUser = new UserData(name, password, 0, "未知");
                boolean success = userManager.register(newUser);
                if (success) out.println("注册成功,请重新登陆");
                else out.println("注册失败，昵称已存在");
                return;

            } else {
                out.println("请先登录");
                return;
            }
            System.out.println("新客户端 " + clientName + " 上线，当前在线人数：" + onlineClients.size());

            String clientMsg;
            while (true) {
                clientMsg = in.readLine();
                if (clientMsg == null) break;

                System.out.println("收到消息：" + clientMsg);

                if (clientMsg.equals("bye")) {
                    System.out.println("客户端结束了对话");
                        break;
                }

                int colonIndex = clientMsg.indexOf(":");
                if (colonIndex == -1) {
                    continue;
                }
                String realMsg = clientMsg.substring(colonIndex + 1);

                if (realMsg.isEmpty()) continue;
                if (realMsg.charAt(0) == ('@')) {
                    int spaceIndex = realMsg.indexOf(" ");

                    if (spaceIndex != -1) {
                        String targetName = realMsg.substring(1, spaceIndex);
                        String privateMsg = realMsg.substring(spaceIndex + 1);
                        String name = clientMsg.substring(0, colonIndex + 1);

                        PrintWriter pint = onlineClients.get(targetName);
                        if (pint != null) pint.println("(私聊消息)" + name + privateMsg);

                    }
                } else {
                    for (PrintWriter client : onlineClients.values()) {
                        client.println("(群聊消息)" + clientMsg);
                    }
                }
            }

            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("客户端断开");
        } finally {
            if (clientName != null) {
                onlineClients.remove(clientName,out);
                System.out.println("客户端 " + clientName + " 下线，当前在线人数：" + onlineClients.size());
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}