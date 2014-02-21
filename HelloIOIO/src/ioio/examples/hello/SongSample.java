package ioio.examples.hello;

import java.util.ArrayList;
import java.util.List;

import cz.versarius.xchords.Chord;
import cz.versarius.xchords.ChordLibrary;
import cz.versarius.xchords.ChordManager;
import cz.versarius.xsong.ChordRef;
import cz.versarius.xsong.Line;
import cz.versarius.xsong.Part;
import cz.versarius.xsong.Song;
import cz.versarius.xsong.Verse;

public class SongSample {
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
		ChordRef firstRef = new ChordRef("robotar-C", 1);
		ChordRef secondRef = new ChordRef("robotar-G", 16);
		ChordRef thirdRef = new ChordRef("robotar-C", 26);
		List<ChordRef> chordsRef = new ArrayList<ChordRef>();
		chordsRef.add(firstRef);
		chordsRef.add(secondRef);
		chordsRef.add(thirdRef);
		
		// line text
		Line line = new Line();
		line.setText("This is sample song line text");
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
		Line line = song.getLine(0);
		for (ChordRef ref : line.getChords()) {
			String libraryName = Chord.getLibraryName(ref.getChordId());
			String chordName = Chord.getChordName(ref.getChordId());
			System.out.println("libname: " + libraryName + ", chordname: " + chordName);
			//ChordManager pc = new ChordManager();
			//pc.
			ChordLibrary lib = manager.findByName(libraryName);
			if (lib == null) {
				System.out.println("have to create libraryName");
				lib = new ChordLibrary(libraryName);
			}
			Chord chord = lib.findByName(chordName);
			if (chord != null) {
				System.out.println("chord set with: " + "chordName" + " found");
				ref.setChord(chord);
			} else {
				System.out.println("no chord");
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
