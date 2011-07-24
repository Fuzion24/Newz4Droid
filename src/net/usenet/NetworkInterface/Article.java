package net.usenet.NetworkInterface;

import java.util.LinkedList;

    public class Article {

        String Subject;
        int Date;
        String Poster;
        public int fileSize;
        public String fileName;
        public LinkedList<Segment> Segments;
        LinkedList<String> Groups;
        public double percentComplete = 0;
        public double totalDownloaded = 0;

        Article() {
            Segments = new LinkedList<Segment>();
            Groups = new LinkedList<String>();
        }
        Article(String poster,String subject,int date){
            Segments = new LinkedList<Segment>();
            Groups = new LinkedList<String>();
            Subject = subject;
            Date = date;
            Poster = poster;
            
        	int firstIndex = Subject.indexOf("\"");
        	int lastIndex = Subject.lastIndexOf("\"");
        	try{
        	fileName = Subject.substring(firstIndex + 1, lastIndex);
        	}
        	catch(Exception e)
        	{
        		fileName = Subject;
        	}
        }

        void addSegment(Segment segment) {
            Segments.add(segment);
        }
        void addGroup(String group)
        {
            Groups.add(group);          
        }
        public String toString()
        {
          return (fileName == null) ? Subject : fileName;
        }
        public void calcFileSize()
        {
        	int size = 0;
        	
        	for(Segment seg : Segments)
        	{
        		size += seg.size;
        	}
        	fileSize = size;
        }
        public double updatePerectComplete(int downloadedAmount)
        {
            totalDownloaded += downloadedAmount;
        	percentComplete = (totalDownloaded / fileSize) * 100;
        	return percentComplete;
        }
    }