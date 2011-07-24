package net.usenet.NetworkInterface;

import java.util.LinkedList;

public class NZB {
    public LinkedList<Article> files;
    public String mNZBName;
    public NZB()
    {
        files = new LinkedList<Article>();
    }
    void addFile(Article file)
    {
        files.add(file);
    }
    public String toString()
    {
    	return mNZBName;
    }

}
