import os
import paramiko
import socket

username = 'ubc_cpen431_8'
password = 'CPEN431'
key = '../../Key/id_rsa'


def connect(hostname):
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(hostname, username=username, password=password, key_filename=key)
        return ssh
    except (paramiko.BadHostKeyException, paramiko.AuthenticationException, paramiko.SSHException, socket.error):
        return None


def get_ip(hostname):
    if hostname is None:
        ip = socket.gethostbyname(socket.gethostname())
        if ip.startswith("127.") and os.name != "nt":
            interfaces = [
                "eth0",
                "eth1",
                "eth2",
                "wlan0",
                "wlan1",
                "wifi0",
                "ath0",
                "ath1",
                "ppp0",
            ]
            for ifname in interfaces:
                try:
                    import fcntl
                    import struct

                    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    ip = socket.inet_ntoa(fcntl.ioctl(s.fileno(), 0x8915, struct.pack('256s', ifname[:15]))[20:24])
                    break
                except IOError:
                    pass
    else:
        ip = socket.gethostbyname(hostname)
    return ip
