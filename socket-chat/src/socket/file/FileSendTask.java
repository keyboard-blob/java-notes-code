package socket.file;

import java.io.*;
import java.net.Socket;

//文件传输的客户端，上传和下载各一个方法
public class FileSendTask {
    public static void sendFile(String host, int port, File file, String sender, String target, ProgressCallback callback) {

        if (!file.exists()) {
            System.out.println("根本不存在这个文件");
            return;
        }

        try {
            Socket socket = new Socket(host, port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);

            //先传名字和大小，再按4KB一块推内容
            dos.writeUTF("UPLOAD");
            dos.writeUTF(sender);
            dos.writeUTF(target);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());
            long totalSize = file.length();
            dos.flush();

            byte[] buffer = new byte[4096];
            long totalRead=0;
            int length;
            while ((length = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, length);
                totalRead+=length;

                if (callback!=null){
                    callback.onProgress((int)(totalRead*100/totalSize));
                }
            }
            dos.flush();

            System.out.println("文件发送完毕" + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean downloadFile(String host, int port, String saveFileNme, File destFile, ProgressCallback callback) {

        try {
            Socket socket = new Socket(host, port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(saveFileNme);
            dos.flush();

            //服务器回“OK”才真正开始收，按大小收够为止
            String status = dis.readUTF();
            if ("OK".equals(status)) {
                long fileSize = dis.readLong();
                FileOutputStream fos = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int length;
                while (totalRead < fileSize && (length = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, length);
                    totalRead += length;
                    if (callback!=null){
                        callback.onProgress((int)(totalRead*100/fileSize));
                    }
                }
                fos.close();
                System.out.println("文件下载成功：" + destFile.getAbsolutePath());
                return true;
            } else System.out.println("保存失败，未找到文件");
            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public interface ProgressCallback {
        void onProgress(int percent);
    }
}
