package net.butfly.bus.utils.gap.ftp;

import net.butfly.bus.utils.gap.Wharf;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.command.CommandFactoryFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedDataTransferFtpReply;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.impl.ServerFtpStatistics;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.function.Consumer;

public abstract class FtpFerry implements Wharf {
    private FtpServer ftpServer;
    private FTPClient ftpClient;

    private String umPropFile;       // server的用户配置文件
    private String ftpServerAddress; // 要创建的ftp server ip
    private int ftpServerPort;       // 要创建的ftp server port
    private String ftpRemoteAddress; // client要连接的server ip
    private int ftpRemotePort;       // client要连接的server port
    private String ftpAccessUser;    // 访问对端server使用的用户
    private String ftpAccessPasswd;  // 访问对端server使用的密码

    public FtpFerry(String umPropFile, String ftpServerAddress, int ftpServerPort,
                    String ftpRemoteAddress, int ftpRemotePort, String ftpAccessUser, String ftpAccessPasswd) {
        this.umPropFile = umPropFile;
        this.ftpServerAddress = ftpServerAddress;
        this.ftpServerPort = ftpServerPort;
        this.ftpRemoteAddress = ftpRemoteAddress;
        this.ftpRemotePort = ftpRemotePort;
        this.ftpAccessUser = ftpAccessUser;
        this.ftpAccessPasswd = ftpAccessPasswd;
    }

    public void init() throws Exception {
        initFtpServer();
        initFtpClient();
    }

    private void initFtpServer() throws FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();
        // 1. set users and their properties
        PropertiesUserManagerFactory umFactory = new PropertiesUserManagerFactory();
        umFactory.setFile(new File(umPropFile));
        serverFactory.setUserManager(umFactory.createUserManager());
        // 2. override STOR cmd to receive data using a buffer instead of writing to a file
        CommandFactoryFactory cfFactory = new CommandFactoryFactory();
        cfFactory.addCommand("STOR", ((session, context, request) -> {
            try {
                // get state variable
                long skipLen = session.getFileOffset();
                if (0L < skipLen) logger().info("STOR offset [" + skipLen + "] will ignore.");

                // argument check
                String fileName = request.getArgument();
                if (fileName == null) {
                    session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                            FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                            "STOR", null, null));
                    return;
                }

                // 24-10-2007 - added check if PORT or PASV is issued, see
                // https://issues.apache.org/jira/browse/FTPSERVER-110
                DataConnectionFactory connFactory = session.getDataConnection();
                if (connFactory instanceof IODataConnectionFactory) {
                    InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();
                    if (address == null) {
                        session.write(new DefaultFtpReply(
                                FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS, "PORT or PASV must be issued first"));
                        return;
                    }
                }

                // 告诉client可以传输
                session.write(LocalizedFtpReply.translate(session, request, context,
                        FtpReply.REPLY_150_FILE_STATUS_OKAY, "STOR",
                        fileName)).awaitUninterruptibly(10000);
                // 获取数据传输连接
                DataConnection dataConnection;
                try {
                    dataConnection = session.getDataConnection().openConnection();
                } catch (Exception e) {
                    logger().debug("Exception getting the input data stream", e);
                    session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                            FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION,
                            "STOR", fileName, null));
                    return;
                }

                // 传输数据
                boolean success = true;
                long transSize = 0L;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    transSize = dataConnection.transferFromClient(session.getFtpletSession(), baos);
                    byte[] bytes = baos.toByteArray();
                    logger().info("File uploaded {} size :{}", fileName, transSize);

                    InputStream in = new ByteArrayInputStream(bytes);
                    seen(fileName, in);
                    in.close();

                    // 传输完成后，知会统计组件
                    ServerFtpStatistics ftpStat = (ServerFtpStatistics) context.getFtpStatistics();
                    ftpStat.setUpload(session, null, transSize);
                } catch (SocketException ex) {
                    logger().debug("Socket exception during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                            "STOR", fileName, null));
                } catch (IOException ex) {
                    logger().debug("IOException during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
                            "STOR", fileName, null));
                }

                // 告诉 client 已经传输完成且结果正确。
                if (success) {
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                            "STOR", fileName, null, transSize));
                }
            } finally {
                session.resetState();
                session.getDataConnection().closeDataConnection();
            }
        }));
        serverFactory.setCommandFactory(cfFactory.createCommandFactory());
        // 3. config ftp server ip and port
        ListenerFactory lrFactory = new ListenerFactory();
        lrFactory.setServerAddress(ftpServerAddress);
        lrFactory.setPort(ftpServerPort);
        serverFactory.addListener("default#" + ftpServerAddress + "@" + ftpServerPort, lrFactory.createListener());
        // 4. create the server and start
        ftpServer = serverFactory.createServer();
        ftpServer.start();
        logger().info("FTP server [" + ftpServerAddress + "@" + ftpServerPort + "] started.");
    }

    private void initFtpClient() throws Exception {
        ftpClient = new FTPClient();
        int reply;

        FTPClientConfig config = new FTPClientConfig();
//        config.setXXX();
        ftpClient.configure(config);
        ftpClient.connect(ftpRemoteAddress, ftpRemotePort);
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            throw new RuntimeException("failed to connect FTP server [" + ftpRemoteAddress + "@" + ftpRemotePort + "]");
        }
        ftpClient.login(ftpAccessUser, ftpAccessPasswd);
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            ftpClient.disconnect();
//            break __abc;
            throw new RuntimeException("user [" + ftpAccessUser + "] authentication failed");
        }
    }

    @Override
    public void touch(String key, Consumer<OutputStream> outing) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            outing.accept(os);
            byte[] bytes = os.toByteArray();
            ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.storeFile(key, new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            logger().error("failed to send data via ftp.");
        }
    }
}
