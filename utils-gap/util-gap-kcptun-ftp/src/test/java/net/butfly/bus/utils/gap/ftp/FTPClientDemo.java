package net.butfly.bus.utils.gap.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.stream.Stream;

public class FTPClientDemo {


    public static void main(String[] args) {
        FTPClient ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
//        config.setXXX(YYY); // change required options
        // for example config.setServerTimeZoneId("Pacific/Pitcairn")
        ftp.configure(config );
        boolean error = false;
        try {
            int reply;
            String server = "172.16.16.116";
            ftp.connect(server, 2221);
            System.out.println("Connected to " + server + "., port: " + ftp.getRemotePort());
            System.out.print(ftp.getReplyString());

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();
            boolean loginret = ftp.login("abc", "123456");
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.out.println("login code: " + reply); // 530
                System.exit(1);
            }

             reply = ftp.sendCommand("OPTS UTF8", "ON");
            if (FTPReply.isPositiveCompletion(reply)) {
                ftp.setControlEncoding("UTF-8");
                System.out.println("set encoding utf 8 :" );
            }

            // transfer files
            String filename = "C:\\Users\\taidl\\Desktop\\git-book.pdf";
//            ftp.changeWorkingDirectory("/abc");
            // 上传文件 storeFile 下载文件 retrieveFile
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            boolean success = ftp.storeFile(file.length() + "-" + file.getName(), fis);
            fis.close();
            if (!success) System.out.println("failed to store file:  " + file.getName());

            ftp.storeFile(UUID.randomUUID().toString(), new ByteArrayInputStream("good morning every".getBytes()));

            ftp.logout();
        } catch(IOException e) {
            error = true;
            e.printStackTrace();
        } finally {
            if(ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch(IOException ioe) {
                    // do nothing
                }
            }
            System.exit(error ? 1 : 0);
        }
    }
}
