package net.usenet.NetworkInterface;

import java.io.ByteArrayOutputStream;

public class Segment {

			public int size; //Size in Bytes
            public int segmentNumber;
            public String msgID;
            public String fileName;
            public int partNum;
            public int partBegin;
            public int partEnd;
            public Boolean decoded;
            public Boolean messedUp = false;
            public ByteArrayOutputStream decodedStream = null;
            public Article parentFile;
            Segment(int sizeOfSegment, int numOfSegment, String messageID, Article pFile)
            {
                decoded = false;
                size = sizeOfSegment;
                segmentNumber = numOfSegment;
                msgID = messageID;
                parentFile = pFile;
             }
        }