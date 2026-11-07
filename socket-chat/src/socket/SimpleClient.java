package socket;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) {
        int port = 8888;
        String host = "127.0.0.1";
        Scanner console = new Scanner(System.in);

        String finalName = null;
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        boolean ifLogged = false;

        while (!ifLogged) {
            try {
                System.out.println("1.登录 2.注册");
                System.out.print("请输入数字：");
                String choice = console.nextLine();

                socket = new Socket(host, port);
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                if ("1".equals(choice)) {
                    System.out.print("输入昵称：");
                    String name = console.nextLine();
                    System.out.print("输入密码：");
                    String password = console.nextLine();

                    out.println("LOGIN " + name + " " + password);
                    String reply = in.readLine();
                    System.out.println("服务端回复: " + reply);

                    if ("登录成功".equals(reply)) {
                        finalName = name;
                        ifLogged = true;
                    } else {
                        socket.close();
                    }

                } else if ("2".equals(choice)) {
                    System.out.print("输入昵称：");
                    String name = console.nextLine();
                    System.out.print("输入密码：");
                    String password = console.nextLine();

                    out.println("REG " + name + " " + password);
                    String reply = in.readLine();
                    System.out.println("服务端回复: " + reply);

                    socket.close();
                } else {
                    System.out.println("输入错误，请重新选择");
                }
            } catch (IOException e) {
                System.out.println("网络连接异常，请重试");
            }
        }

        System.out.println("成功进入聊天室");

        String chatName = finalName;
        BufferedReader finalIn = in;
        Thread thread = new Thread(new Runnable() {
            String msg;

            @Override
            public void run() {
                try {
                    while ((msg = finalIn.readLine()) != null) {
                        System.out.println("\n" + msg);
                        System.out.print(chatName + "(客户端): ");
                    }
                } catch (IOException e) {
                    System.out.println("\n与服务器断开连接");
                }
            }
        });
        thread.start();

        while (true) {
            System.out.print(chatName + "(客户端): ");
            String msg = console.nextLine();
            if ("bye".equalsIgnoreCase(msg)) {
                out.println("bye");
                break;
            }
            out.println(chatName + ":" + msg);
        }

        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("客户端已关闭");
    }
}