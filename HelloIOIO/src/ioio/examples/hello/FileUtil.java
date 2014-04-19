package ioio.examples.hello;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

public class FileUtil {
	private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

	public static final String ROBOTAR_FOLDER = "robotar/";
	public static final String ROBOTAR_SONGS_FOLDER = "songs/";
	public static final String ROBOTAR_SETTINGS_FOLDER = "cfg/";
	
	public static final String CORRECTIONS = "corrections.xml";
	
	/* Checks if external storage is available for read and write */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/* Checks if external storage is available to at least read */
	public static boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}

	private static File getCommonDir(String path) {
		// Get the directory for the user's public documents directory.
		// this required api level 19, therefore we put our robotarfolder only
		// at the root of external storage
		// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS
		
		File file = new File(Environment.getExternalStorageDirectory(), path);
		file.mkdirs();
		if (!file.isDirectory()) {
			LOG.error("Robotar directory could not be created, path {}", path);
			return null;
		}
		return file;
	}
	
	public static File getRobotarDir() {
		return getCommonDir(ROBOTAR_FOLDER);
	}

	public static File getRobotarStorageDir() {
		return getCommonDir(ROBOTAR_FOLDER + ROBOTAR_SONGS_FOLDER);
	}
	
	public static File getRobotarSettingsDir() {
		return getCommonDir(ROBOTAR_FOLDER + ROBOTAR_SETTINGS_FOLDER);
	}
	
	public static void copyCorrections(Context context) {
		copyAssetFiles(context, new String[] { CORRECTIONS });
	}
	
	private static void copyAssetFiles(Context context, String[] files) {
	    AssetManager assetManager = context.getAssets();
	    File settingsDir = getRobotarSettingsDir();
	    if (settingsDir == null) {
	    	LOG.error("Could not get robotar settings directory. Is SD attached?");
	    	return;
	    }
	    for (String filename : files) {
	        InputStream in = null;
	        OutputStream out = null;
	        try {
	        	in = assetManager.open(filename);
	        	File outFile = new File(settingsDir, filename);
	        	out = new FileOutputStream(outFile);
	        	copyFile(in, out);
	        	in.close();
	        	in = null;
	        	out.flush();
	        	((FileOutputStream)out).getFD().sync(); 
	        	out.close();
	        	out = null;
	        	LOG.debug("File copied from assets to SD: {}", filename);
	        } catch(IOException e) {
	            LOG.error("Failed to copy asset file {}", filename);
	            LOG.error("stacktrace: ", e);
	        }       
	    }
	}
	
	private static void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while ((read = in.read(buffer)) != -1) {
	    	out.write(buffer, 0, read);
	    }
	}

	public static boolean correctionsExists() {
		File settingsDir = getRobotarSettingsDir();
	    if (settingsDir == null) {
	    	LOG.error("Could not get robotar settings directory. Is SD attached?");
	    	return false;
	    }
	    File corrections = new File(settingsDir, CORRECTIONS);
	    return corrections.exists();
	}

	public static File getCorrections() {
		return new File(Environment.getExternalStorageDirectory(),
				ROBOTAR_FOLDER + ROBOTAR_SETTINGS_FOLDER + CORRECTIONS);
	}
}
