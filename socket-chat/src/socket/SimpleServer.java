package socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServer {
    public static void main(String[] args) {
        int port = 8888;

        UserManager userManager = new UserManager();

        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port);) {
            System.out.println("正在监听 " + port + " 端口…");
            System.out.println("等待客户端连接…");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("链接到一个客户端，IP：" + socket.getInetAddress());

                pool.execute(new ClientHandler(socket, userManager));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}