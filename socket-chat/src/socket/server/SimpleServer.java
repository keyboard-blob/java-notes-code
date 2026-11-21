package socket.server;

import socket.client.UserManager;
import socket.file.FileReceiveTask;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//服务端入口：一条线程收文件，一个线程池应付聊天连接
public class SimpleServer {
    public static void main(String[] args) {

        //文件通道单独跑在9999端口
        new Thread(new FileReceiveTask()).start();

        UserManager userManager = new UserManager();

        //线程池（最多10个客户端同时处理）
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(8888);) {
            System.out.println("正在监听 " + 8888 + " 端口…");
            System.out.println("等待客户端连接…");

            //每个连接交给一个处理器（handler），互不阻塞
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
