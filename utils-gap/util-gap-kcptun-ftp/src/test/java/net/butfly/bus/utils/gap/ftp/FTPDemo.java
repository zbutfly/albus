package net.butfly.bus.utils.gap.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.stream.Stream;

public class FTPDemo {
    public static void main(String[] args) {
        FTPClient ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
//        config.setXXX(YYY); // change required options
        // for example config.setServerTimeZoneId("Pacific/Pitcairn")
        ftp.configure(config );
        boolean error = false;
        try {
            int reply;
            String server = "172.16.16.242";
            ftp.connect(server);
            System.out.println("Connected to " + server + "., port: " + ftp.getRemotePort());
            System.out.print(ftp.getReplyString());

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();
            boolean loginret = ftp.login("taidl", "123456");
//            ftp.login("taidl", "12111113456");
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.out.println("login code: " + reply); // 530
                System.exit(1);
            }

            if(!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                System.exit(1);
            }
            // transfer files
            String filename = "C:\\Users\\taidl\\Desktop\\git-book.pdf";
//            ftp.changeWorkingDirectory("/abc");
            // 上传文件 storeFile 下载文件 retrieveFile
            System.out.println("store fiile:" + filename + ", result: "
                    + ftp.storeFile(new File(filename).getName(), new FileInputStream(filename)));

            Stream.of(ftp.listDirectories()).forEach(ftpFile -> {
                System.out.println("\n");
                System.out.println(ftpFile.toFormattedString());
            });
            //
            ftp.logout();
            ftp.storeFile(new File(filename).getName(), new FileInputStream(filename));
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.out.println("storefile after logout, ret : " + reply); // FTPConnectionClosedException
                System.exit(1);
            }

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
