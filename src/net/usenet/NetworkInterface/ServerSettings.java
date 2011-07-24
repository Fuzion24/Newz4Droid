package net.usenet.NetworkInterface;

public class ServerSettings {
    public Boolean requiresLogin;
    public Boolean requiresSSL;
    public String userName;
    public String passWord;
    public String hostName;
    public int port;
    public int connectTimeoutMs;
    public String DOWNLOAD_QUEUE_FILE = "DOWNLOAD_QUEUE.dat";
}