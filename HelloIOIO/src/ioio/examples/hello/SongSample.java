package ioio.examples.hello;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.util.Log;
import cz.versarius.xchords.Chord;
import cz.versarius.xchords.ChordLibrary;
import cz.versarius.xchords.ChordManager;
import cz.versarius.xsong.ChordRef;
import cz.versarius.xsong.Line;
import cz.versarius.xsong.Part;
import cz.versarius.xsong.Song;
import cz.versarius.xsong.Verse;

public class SongSample {
	private static final Logger LOG = LoggerFactory.getLogger(SongSample.class);

	private Song song;
	
	public SongSample() {
		init();
	}
	
	private void init() {
		// create testing song sample in memory
		song = new Song();
		song.setInfo("in memory testin song");
		song.setTitle("sample song title");
		song.setInterpret("sample song interpret");

		// chords ref
		ChordRef firstRef = new ChordRef("robotar-E7", 1);
		ChordRef secondRef = new ChordRef("robotar-A7", 16);
		ChordRef thirdRef = new ChordRef("robotar-E7", 26);
		ChordRef forthRef = new ChordRef("robotar-B7", 26);
		ChordRef fifthRef = new ChordRef("robotar-A7", 26);
		ChordRef sixthRef = new ChordRef("robotar-E7", 26);
		List<ChordRef> chordsRef = new ArrayList<ChordRef>();
		chordsRef.add(firstRef);
		chordsRef.add(secondRef);
		chordsRef.add(thirdRef);
		chordsRef.add(forthRef);
		chordsRef.add(fifthRef);
		chordsRef.add(sixthRef);
		
		// line text
		Line line = new Line();
		line.setText("Blues in E");
		line.setChords(chordsRef);

		// verse
		Part first = new Verse();
		first.addLine(line);
		
		// add all to the song
		song.addPart(first);
	}

	public Song getSong() {
		return song;
	}

	public boolean fillWith(ChordManager manager) {
		boolean error = false;
		/*
		for (Part part : parts) {
			// check empty lines
			for (Line line : part.getLines()) {
			}
		}
		 */
		Line line = song.getLine(0);
		for (ChordRef ref : line.getChords()) {
			String libraryName = Chord.getLibraryName(ref.getChordId());
			String chordName = Chord.getChordName(ref.getChordId());
			LOG.debug("libname: {}, chordname: {}", libraryName, chordName);
			//ChordManager pc = new ChordManager();
			//pc.
			ChordLibrary lib = manager.findByName(libraryName);
			if (lib == null) {
				LOG.debug("have to create {}", libraryName);
				lib = new ChordLibrary(libraryName);
			}
			Chord chord = lib.findByName(chordName);
			if (chord != null) {
				LOG.debug("chord with name: {} found", chordName);
				ref.setChord(chord);
			} else {
				LOG.debug("no chord for name: {}", chordName);
			} /*else {
				// chord was not in usedchords section
				ChordLibrary library = manager.getChordLibraries().get(libraryName);
				if (library != null) {
					Chord existing = library.findByName(chordName);
					if (existing != null) {
						ref.setChord(existing);
						used.add(existing);
						continue;
					}
				}
				
				if (libraryName.startsWith(ChordManager.USER_PREFIX)) {
					// look into current chord buffer on chords page
					RoboTarChordsPage chPage = mainFrame.getChordsPage();
					if (chPage != null) {
						@SuppressWarnings("unchecked")
						DefaultListModel<Chord> model = (DefaultListModel<Chord>)chPage.getChordListModel();
						if (model != null) {
							Chord existing = findByName(model, ref.getChordId()); 
							if (existing != null) {
								ref.setChord(existing);
								used.add(existing);
								continue;
							}
						}
					}
				}
			}*/
		}
		return error;
	}
}
