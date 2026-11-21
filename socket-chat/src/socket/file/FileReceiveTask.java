package socket.file;

import socket.server.ClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

//文件服务端：9999端口，处理上传和下载两种请求
public class FileReceiveTask implements Runnable {
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(9999);
            System.out.println("文件接收服务端已启动，监听 9999 端口...");

            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                String action = dis.readUTF();
                if ("UPLOAD".equals(action)) {
                    String sender = dis.readUTF();
                    String target = dis.readUTF();
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    //存成“server_发送人_文件名”，避免重名覆盖
                    String saveFileName = sender + "_" + fileName;
                    String savePath = "server_" + saveFileName;
                    FileOutputStream fos = new FileOutputStream(savePath);

                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int length;

                    while (totalRead < fileSize && (length = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                        totalRead += length;
                    }

                    fos.close();
                    socket.close();
                    System.out.println("文件接收完毕，已保存为 " + savePath);

                    String notifyMsg = "[FILE]\u0001" + target + "\u0001" +sender + "\u0001" + fileName + "\u0001" + fileSize + "\u0001" + saveFileName;

                    //群聊广播给所有人，私聊只发给对方和发送者自己
                    if ("群聊".equals(target)) {
                        for (PrintWriter p : ClientHandler.onlineClients.values()) p.println(notifyMsg);
                    } else {
                        PrintWriter targetOut = ClientHandler.onlineClients.get(target);
                        if (targetOut != null) targetOut.println(notifyMsg);
                        PrintWriter senderOut = ClientHandler.onlineClients.get(sender);
                        if (senderOut != null) senderOut.println(notifyMsg);
                    }

                } else if ("DOWNLOAD".equals(action)) {
                    String downloadFileName = dis.readUTF();
                    File file = new File("server_" + downloadFileName);
                    if (file.exists()) {
                        dos.writeUTF("OK");
                        dos.writeLong(file.length());
                        dos.flush();

                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = fis.read(buffer)) > 0) dos.write(buffer, 0, len);
                        fis.close();
                        dos.close();
                        socket.close();
                    } else {
                        dos.writeUTF("ERROR");
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
