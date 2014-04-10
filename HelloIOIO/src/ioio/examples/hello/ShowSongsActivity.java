package ioio.examples.hello;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ShowSongsActivity extends ActionBarActivity {
	private static final Logger LOG = LoggerFactory.getLogger(ShowSongsActivity.class);

	List<String> songList = new ArrayList<String>();
	private ListView list;
    private ArrayAdapter adapter;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_songs);
		
		/*if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}*/
		
		// get robotar folder
		File rtFolder = FileUtil.getRobotarStorageDir(FileUtil.ROBOTAR_FOLDER);
		if (!rtFolder.isDirectory()) {
			LOG.error("cannot get robotar folder");
			return;
		}
		// accept only .xml files
		File file[] = rtFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getPath().endsWith(".xml");
			}
		});
		// list files
		songList.clear();
		LOG.debug("Size: {}", file.length);
		for (int i = 0; i < file.length; i++) {
		    LOG.debug("FileName: {}", file[i].getName());
		    songList.add(file[i].getName());
		}
		// set them to the listview
		list = (ListView) findViewById(R.id.songsListView);
		adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, songList);
        list.setAdapter(adapter);
        
        // handle clicks
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                int position, long id) {
              final String item = (String) parent.getItemAtPosition(position);
              LOG.info("here we are with {}", item);
              showSong(item);
            }

          });
	}

	private void showSong(String filename) {
		Intent intent = new Intent(ShowSongsActivity.this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		LOG.debug("setting extra filename: {}", filename);
		intent.putExtra("myfilename", filename);
	    startActivity(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.show_songs, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	/*
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_show_songs,
					container, false);
			return rootView;
		}
	}*/

}
