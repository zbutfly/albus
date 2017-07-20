#!/bin/bash

yum -y install vsftpd

setsebool ftpd_connect_db 1
setsebool ftpd_full_access 1
setsebool tftp_home_dir 1

adduser -g ftp -s /sbin/nologin username
passwd username


vi /etc/vsftpd/vsftpd.conf
chroot_local_user=YES
allow_writeable_chroot=YES

systemctl enable vsftpd.service
service vsftpd restart


#=============================
yum -y install ftp


# access ftp via ie or firefox
ftp://taidl:123456@172.16.16.242