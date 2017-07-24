package net.butfly.bus.utils.gap.ftp;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.command.CommandFactoryFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedDataTransferFtpReply;
import org.apache.ftpserver.impl.LocalizedFtpReply;
import org.apache.ftpserver.impl.ServerFtpStatistics;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class FTPServerDemo {

    private static final Logger LOG = LoggerFactory.getLogger(FTPServerDemo.class);

    public static void main(String[] args) throws FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();

        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        // todo use @Config to set user properties file
        String usersPropFile = "src/test/resources/users.properties";
        userManagerFactory.setFile(new File(usersPropFile));
        serverFactory.setUserManager(userManagerFactory.createUserManager());

        // add a user and set his home dir
        BaseUser user = new BaseUser();
        user.setName("abc");
        user.setPassword("123456");
        user.setHomeDirectory("E:\\ftp_server");
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        serverFactory.getUserManager().save(user);

        CommandFactoryFactory factoryFactory = new CommandFactoryFactory();

        factoryFactory.addCommand("STOR", (session, context, request) -> {
            try {
                // get state variable
                long skipLen = session.getFileOffset();
                if (0L < skipLen) LOG.info("STOR offset [" + skipLen + "] will ignore.");

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
                    LOG.debug("Exception getting the input data stream", e);
                    session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                        FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION,
                        "STOR", fileName, null));
                    return;
                }

                // 传输数据
                boolean success = true;
                long transSz = 0L;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    transSz = dataConnection.transferFromClient(session.getFtpletSession(), baos);
                    byte[] bytes = baos.toByteArray();

                    LOG.info("File uploaded {} size :{}", fileName, transSz);
                    // 传输完成后，知会统计组件
                    ServerFtpStatistics ftpStat = (ServerFtpStatistics) context.getFtpStatistics();
                    ftpStat.setUpload(session, null, transSz);
                } catch (SocketException ex) {
                    LOG.debug("Socket exception during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                        session, request, context, FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                        "STOR", fileName, null));
                } catch (IOException ex) {
                    LOG.debug("IOException during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                        session, request, context, FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
                        "STOR", fileName, null));
                }

                // 告诉 client 已经传输完成且结果正确。
                if (success) {
                    session.write(LocalizedDataTransferFtpReply.translate(
                        session, request, context, FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                        "STOR", fileName, null, transSz));
                }
            } finally {
                session.resetState();
                session.getDataConnection().closeDataConnection();
            }
        });
        serverFactory.setCommandFactory(factoryFactory.createCommandFactory());

        ListenerFactory factory = new ListenerFactory();
        factory.setPort(2221);
        factory.setServerAddress("172.16.16.116");
        Listener listener = factory.createListener();
        // replace the default listener
        serverFactory.addListener("default", listener);
        FtpServer server = serverFactory.createServer();
        server.start();
        LOG.info("ftp server started....");
    }
}
