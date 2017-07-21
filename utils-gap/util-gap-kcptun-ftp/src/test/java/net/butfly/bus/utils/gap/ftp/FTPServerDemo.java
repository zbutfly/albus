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
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class FTPServerDemo {

    private static final Logger LOG = LoggerFactory.getLogger(FTPServerDemo.class);

    public static void main(String[] args) throws FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();
        // add a user and set his home dir
        BaseUser user = new BaseUser();
        user.setName("abc");
        user.setPassword("123456");
        user.setHomeDirectory("E:\\ftp_server");

        // set user authority
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        serverFactory.getUserManager().save(user);

        CommandFactoryFactory factoryFactory = new CommandFactoryFactory();
        factoryFactory.addCommand("STOR", (session, context, request) -> {
            try {

                // get state variable
                long skipLen = session.getFileOffset();

                // argument check
                String fileName = request.getArgument();
                if (fileName == null) {
                    session
                            .write(LocalizedDataTransferFtpReply
                                    .translate(
                                            session,
                                            request,
                                            context,
                                            FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                                            "STOR", null, null));
                    return;
                }

                // 24-10-2007 - added check if PORT or PASV is issued, see
                // https://issues.apache.org/jira/browse/FTPSERVER-110
                DataConnectionFactory connFactory = session.getDataConnection();
                if (connFactory instanceof IODataConnectionFactory) {
                    InetAddress address = ((IODataConnectionFactory) connFactory)
                            .getInetAddress();
                    if (address == null) {
                        session.write(new DefaultFtpReply(
                                FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                                "PORT or PASV must be issued first"));
                        return;
                    }
                }

                // 不需要获取文件及其权限了。
                // get filename
                FtpFile file = null;
                try {
                    file = session.getFileSystemView().getFile(fileName);
                } catch (Exception ex) {
                    LOG.debug("Exception getting file object", ex);
                }
                if (file == null) {
                    session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                            "STOR.invalid", fileName, file));
                    return;
                }
                fileName = file.getAbsolutePath();

                // get permission
                if (!file.isWritable()) {
                    session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                            FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                            "STOR.permission", fileName, file));
                    return;
                }
                // 上面内容可以不要了。

                // 告诉client可以传输
                // get data connection
                session.write(
                        LocalizedFtpReply.translate(session, request, context,
                                FtpReply.REPLY_150_FILE_STATUS_OKAY, "STOR",
                                fileName)).awaitUninterruptibly(10000);

                // 获取数据传输连接
                DataConnection dataConnection;
                try {
                    dataConnection = session.getDataConnection().openConnection();
                } catch (Exception e) {
                    LOG.debug("Exception getting the input data stream", e);
                    session.write(LocalizedDataTransferFtpReply.translate(session, request, context,
                            FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "STOR",
                            fileName, file));
                    return;
                }

                // 传输数据
                // transfer data
                boolean success = true;
                long transSz = 0L;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    outStream = file.createOutputStream(skipLen);
//                    transSz = dataConnection.transferFromClient(session.getFtpletSession(), outStream);
                    transSz = dataConnection.transferFromClient(session.getFtpletSession(), baos);

                    // attempt to close the output stream so that errors in
                    // closing it will return an error to the client (FTPSERVER-119)
//                    outStream.close();

                    byte[] bytes = baos.toByteArray();
                    System.out.println("store size: " + bytes.length + " with name " + fileName);

                    LOG.info("File uploaded {}", fileName);

                    // 传输完成后，知会统计组件
                    // notify the statistics component
                    ServerFtpStatistics ftpStat = (ServerFtpStatistics) context
                            .getFtpStatistics();
                    ftpStat.setUpload(session, file, transSz);

                } catch (SocketException ex) {
                    LOG.debug("Socket exception during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                            "STOR", fileName, file));
                } catch (IOException ex) {
                    LOG.debug("IOException during data transfer", ex);
                    success = false;
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN,
                            "STOR", fileName, file));
                }

                // 告诉 client 已经传输完成且结果正确。
                // if data transfer ok - send transfer complete message
                if (success) {
                    session.write(LocalizedDataTransferFtpReply.translate(
                            session, request, context, FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                            "STOR", fileName, file, transSz));
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
