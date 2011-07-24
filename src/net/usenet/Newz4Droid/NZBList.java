package net.usenet.Newz4Droid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParserException;

import net.usenet.NetworkInterface.Article;
import net.usenet.NetworkInterface.NZB;
import net.usenet.NetworkInterface.NZBparser;
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;


public class NZBList extends ExpandableListActivity implements OnChildClickListener{
    ExpandableListAdapter mAdapter;
	ArrayList<String> mNZBFileList;
	ArrayList<NZB> mNZBList;
	NZBparser nzbparser;
	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_SETTINGS = Menu.FIRST + 1;
	private static final int MENU_QUIT = Menu.FIRST + 2;
	
	private File extStorageDir;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extStorageDir = Environment.getExternalStorageDirectory();
        refreshNZBs();
        
        mAdapter = new MyExpandableListAdapter();
        setListAdapter(mAdapter);
        registerForContextMenu(getExpandableListView());
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REFRESH, 0, "Refresh NZBs");
		menu.add(0, MENU_SETTINGS, 0, "Server Settings");
		menu.add(0, MENU_QUIT, 0, "Quit");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			refreshNZBs();
			return true;
		case MENU_QUIT:
			this.finish();
			return true;
		case MENU_SETTINGS:
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			return true;
		}
		return false;
	}
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,int childPosition, long id) {
        
        CheckBox cb = (CheckBox)v.findViewById( R.id.checkBox );
        if( cb != null )
            cb.toggle();
        //cb.isChecked();
          return false;
    
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

        
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
           // int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
           // int childPos = ExpandableListView.getPackedPositionChild(info.packedPosition); 
        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
    		menu.setHeaderTitle("NZB Menu");
    		menu.add(0, v.getId(), 0, "Add NZB to Queue");
    		menu.add(0, v.getId(), 0, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        		
        int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition); 
        NZB nzbContexted = (NZB)  mAdapter.getGroup(groupPos);
		if (item.getTitle() == "Delete") {
			Delete(nzbContexted.toString());
		} else if (item.getTitle() == "Add NZB to Queue") {			
					
					if(DownloadService.downloadQueue == null) DownloadService.downloadQueue = new LinkedList<Article>();
					DownloadService.downloadQueue.addAll(nzbContexted.files);
									 
		} else {
			return false;
		}
		return true;
    }
    
	public void Delete(String NZBid) {
		File f = new File(extStorageDir.getPath() + "/NZBs/" + NZBid);
		if(f.exists())
		{
			f.delete();
		}
		refreshNZBs();
	}

	private void refreshNZBs(){
		mNZBFileList = new ArrayList<String>();
		mNZBList = new ArrayList<NZB>();
		nzbparser = new NZBparser();
		File nzbFolder = new File(extStorageDir.getPath() + "/NZBs/");
			nzbFolder.mkdir();
		File[] nzbList = nzbFolder.listFiles();
		for (File m : nzbList) {
			mNZBFileList.add(m.getName());
		}
		for(String nzbName : mNZBFileList)
		{
			NZB nzb = null;
			try {
				 nzb = nzbparser.parse(extStorageDir.getPath() + "/NZBs/"
						+ nzbName);
				 nzb.mNZBName = nzbName;
				 mNZBList.add(nzb);
			} catch(XmlPullParserException e)
			{
					System.out.println(e.toString());
			}
			catch (IOException e) {
				nzb = new NZB();
				nzb.mNZBName = nzbName;
							mNZBList.add(nzb);
				Toast.makeText(getApplicationContext(), "There was an error parsing: " + nzbName, Toast.LENGTH_LONG);
			}
		}
		

        
	}
	
	
	public class MyExpandableListAdapter extends BaseExpandableListAdapter {
	
	    
	    public MyExpandableListAdapter(){
	    	
	    
	
	    }

		public Object getChild(int groupPosition, int childPosition) {
			NZB nzb = mNZBList.get(groupPosition);			
			Article a = nzb.files.get(childPosition);
	        return a.fileName;
	    }

	    public long getChildId(int groupPosition, int childPosition) {
	        return childPosition;
	    }

	    public int getChildrenCount(int groupPosition) {
	    	NZB nzb = mNZBList.get(groupPosition);
			return nzb.files.size();
	    	    }

	    public TextView getGenericView() {
	        // Layout parameters for the ExpandableListView
	        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
	                ViewGroup.LayoutParams.WRAP_CONTENT, 64);

	        TextView textView = new TextView(NZBList.this);
	        textView.setLayoutParams(lp);
	        // Center the text vertically
	        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	        // Set the text starting position
	        textView.setPadding(60, 0, 0, 0);
	        return textView;
	    }

	    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	    	 LayoutInflater inflater = LayoutInflater.from(getBaseContext());
	    	 View v = inflater.inflate(R.layout.child_row, null, false);
	    	TextView childName = (TextView) v.findViewById(R.id.childname);
	    	childName.setText(getChild(groupPosition, childPosition).toString());
	      return v;
	    }

	    public Object getGroup(int groupPosition) {

		return mNZBList.get(groupPosition);
		}

	    public int getGroupCount() {
	        return mNZBList.size();
	    }

	    public long getGroupId(int groupPosition) {
	        return groupPosition;
	    }

	    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
	            ViewGroup parent) {
	        TextView textView = getGenericView();
	        textView.setText(getGroup(groupPosition).toString());
	        textView.setPadding(60, 0, 0, 0);
	        return textView;
	    }

	    public boolean isChildSelectable(int groupPosition, int childPosition) {
	        return true;
	    }

	    public boolean hasStableIds() {
	        return true;
	    }

	}
}
