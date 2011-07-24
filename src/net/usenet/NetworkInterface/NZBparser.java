package net.usenet.NetworkInterface;
import java.io.FileReader;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;


/**
 * This is a SAX parser for parsing an NZB file.
 *
 * Typical usage of this parser would be:<p>
 * <code>
 * NzbParser parser = new NzbParser();<br>
 * NZB nzb = parser.parseFile(<i>path to file</i>);
 * </code>
 */

public class NZBparser {
	
	public NZB parse(String path) throws XmlPullParserException, IOException
	{
		return parse(new FileReader(path));
	}
	
	public NZB parse(FileReader source) throws XmlPullParserException, IOException
	{
		XmlPullParser parser = Xml.newPullParser();
		
		NZB nzb = new NZB();
		
		boolean inFileTag = false;
		boolean inGroupsTag = false;
		boolean inSegmentsTag = false;
		boolean inGroupTag = false;
		boolean inSegmentTag = false;
		
		Article currentFile = null;
		int currentBytes = 0;
		int currentNumber = 0;
		String currentMessageId = "";
		
		parser.setInput(source);
		int event = parser.getEventType();
		
		while (event != XmlPullParser.END_DOCUMENT)
		{
			switch (event)
			{
			case XmlPullParser.START_DOCUMENT:
				break;
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if (tagName.equalsIgnoreCase("file"))
				{
					if (inFileTag || inGroupsTag || inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inFileTag = true;
					
					String subject = "";
					String poster = "";
					int date = 0;
					
					
					for (int i=0; i<parser.getAttributeCount(); i++)
					{
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("subject"))
						{
							subject = parser.getAttributeValue(i);
						}
						else if (attribute.equalsIgnoreCase("poster"))
						{
							poster = parser.getAttributeValue(i);
						}
						else if (attribute.equalsIgnoreCase("date"))
						{
							try 
							{
								date = Integer.parseInt(parser.getAttributeValue(i));
							}
							catch (Exception e)
							{
								//lol
							}
						}
						
						currentFile = new Article(poster, subject, date);
					}
				} 
				else if (tagName.equalsIgnoreCase("groups"))
				{
					if (!inFileTag || inGroupsTag || inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inGroupsTag = true;
				}
				else if (tagName.equalsIgnoreCase("group"))
				{
					if (!inFileTag || !inGroupsTag || inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inGroupTag = true;
				}
				else if (tagName.equalsIgnoreCase("segments"))
				{
					if (!inFileTag || inGroupsTag || inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inSegmentsTag = true;
				}
				else if (tagName.equalsIgnoreCase("segment"))
				{
					if (!inFileTag || inGroupsTag || !inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inSegmentTag = true;
					
					for (int i=0; i<parser.getAttributeCount(); i++)
					{
						String attribute = parser.getAttributeName(i);
						if (attribute.equalsIgnoreCase("bytes"))
						{
							try 
							{
								currentBytes = Integer.parseInt(parser.getAttributeValue(i));
							}
							catch (Exception e)
							{
								//lol
							}
						}
						else if (attribute.equalsIgnoreCase("number"))
						{
							try 
							{
								currentNumber = Integer.parseInt(parser.getAttributeValue(i));
							}
							catch (Exception e)
							{
								//lol
							}
						}
					}
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase("file"))
				{
					if (!inFileTag || inGroupsTag || inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inFileTag = false;
					
					nzb.addFile(currentFile);
				} 
				else if (tagName.equalsIgnoreCase("groups"))
				{
					if (!inFileTag || !inGroupsTag || inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inGroupsTag = false;
				}
				else if (tagName.equalsIgnoreCase("group"))
				{
					if (!inFileTag || !inGroupsTag || inSegmentsTag || !inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inGroupTag = false;
				}
				else if (tagName.equalsIgnoreCase("segments"))
				{
					if (!inFileTag || inGroupsTag || !inSegmentsTag || inGroupTag || inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inSegmentsTag = false;
				}
				else if (tagName.equalsIgnoreCase("segment"))
				{
					if (!inFileTag || inGroupsTag || !inSegmentsTag || inGroupTag || !inSegmentTag)
					{
						throw new XmlPullParserException("Invalid NZB file");
					}
					
					inSegmentTag = false;
					
					currentFile.addSegment(new Segment(currentBytes, currentNumber, currentMessageId,currentFile));
				}
				break;
			case XmlPullParser.TEXT:
				if (inGroupTag)
				{
					String debugshiz = parser.getText();
					currentFile.addGroup(debugshiz);
				}
				else if (inSegmentTag)
				{
					String debugshiz = parser.getText();
					currentMessageId = debugshiz;
				}
				break;
			}
			
			
			event = parser.next();
		}
		
		return nzb;
	}
}
