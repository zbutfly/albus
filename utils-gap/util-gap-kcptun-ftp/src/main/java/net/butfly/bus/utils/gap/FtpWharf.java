package net.butfly.bus.utils.gap;

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

public abstract class FtpWharf extends Thread implements Wharf {
    private FtpServer ftpServer;

    private String umPropFile;       // server的用户配置文件
    private String ftpServerAddress; // 要创建的ftp server ip
    private int ftpServerPort;       // 要创建的ftp server port
    private String ftpRemoteAddress; // client要连接的server ip
    private int ftpRemotePort;       // client要连接的server port
    private String ftpAccessUser;    // 访问对端server使用的用户
    private String ftpAccessPasswd;  // 访问对端server使用的密码

    static final int UDP_DIAGRAM_MAX_LEN = 0xFFFF - 8 - 20;

    public FtpWharf(String umPropFile, String ftpServer, String ftpRemote, String ftpAccount) throws FtpException {
        this.umPropFile = umPropFile;
        String[] tuple = ftpServer.split(":", 2);
        this.ftpServerAddress = tuple[0];
        this.ftpServerPort = Integer.parseInt(tuple[1]);
        tuple = ftpRemote.split(":", 2);
        this.ftpRemoteAddress = tuple[0];
        this.ftpRemotePort = Integer.parseInt(tuple[1]);
        tuple = ftpAccount.split(":", 2);
        this.ftpAccessUser = tuple[0];
        this.ftpAccessPasswd = tuple[1];

        initFtpServer();
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
                if (0L < skipLen) logger().warn("STOR offset [{}] will ignore.", skipLen);

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
                    logger().error("Exception getting the input data stream", e);
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
                    logger().debug("ftp receive [{} bytes] with key {}.", transSize, fileName);

                    InputStream in = new ByteArrayInputStream(bytes);
                    seen(fileName, in);
                    in.close();

                    // 传输完成后，知会统计组件
                    ServerFtpStatistics ftpStat = (ServerFtpStatistics) context.getFtpStatistics();
                    ftpStat.setUpload(session, null, transSize);
                } catch (SocketException ex) {
                    logger().error("Socket exception during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                            "STOR", fileName, null));
                } catch (IOException ex) {
                    logger().error("IOException during data transfer", ex);
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
        lrFactory.setPort(ftpServerPort);
        lrFactory.setServerAddress(ftpServerAddress);
        serverFactory.addListener("default", lrFactory.createListener());
        // 4. create the server and start
        ftpServer = serverFactory.createServer();
        ftpServer.start();
        logger().info("FTP server [" + ftpServerAddress + "@" + ftpServerPort + "] started.");
    }

    @Override
    public void touch(String key, Consumer<OutputStream> outing) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            outing.accept(os);
            byte[] bytes = os.toByteArray();
            send(key, bytes);
        } catch (IOException e) {
            logger().error("failed to send data via ftp.");
        }
    }

    private void send(String key, byte[] data) throws IOException {
        FTPClient ftpClient = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
        ftpClient.configure(config);
        ftpClient.connect(ftpRemoteAddress, ftpRemotePort);
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            throw new RuntimeException("failed to connect FTP server [" + ftpRemoteAddress + "@" + ftpRemotePort + "]");
        }
        ftpClient.login(ftpAccessUser, ftpAccessPasswd);
        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            ftpClient.disconnect();
            throw new RuntimeException("user [" + ftpAccessUser + "] authentication failed");
        }
        ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.storeFile(key, new ByteArrayInputStream(data));
        ftpClient.logout();
        ftpClient.disconnect();

        logger().debug("ftp send [{} bytes] with key {}.", data.length, key);
    }
}
