package infer.util.rules;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.IStemmer;
import edu.mit.jwi.morph.WordnetStemmer;

public class WordNetUtil {
	public static String wnhome = "file:///usr/share/wordnet";
	private static IDictionary dict = null;
	final static Logger log = Logger.getLogger(WordNetUtil.class.getName());

	public static void main(String args[]) throws Exception {
		URL url = new URL(wnhome);
		// construct the dictionary object and open it
		dict = new Dictionary(url);
		dict.open();
		// look up first sense of the word " dog "
		String lookup = "sports";
		IIndexWord idxWord = dict.getIndexWord(lookup, POS.NOUN);
		if (idxWord == null || idxWord.getWordIDs().isEmpty()) {
			IStemmer stemmer = new WordnetStemmer(dict);
			List<String> stems = stemmer.findStems(lookup, POS.NOUN);
			if (stems.size() > 0) {
				lookup = stems.get(0);
				idxWord = dict.getIndexWord(lookup, POS.NOUN);
			}

		}
		System.out.println(" Tag Sense Count = " + idxWord.getTagSenseCount());
		IWordID wordID = idxWord.getWordIDs().get(0);
		IWord word = dict.getWord(wordID);
		System.out.println(" Id = " + wordID);
		System.out.println(" Lemma = " + word.getLemma());
		System.out.println(" Gloss = " + word.getSynset().getGloss());
		System.out.println(" synonyms are " + getSynonyms(lookup));
		System.out.println(" hypernyms are " + getHypernyms(lookup));

	}

	public static IDictionary getDictionary(String wnhome) {
		if (dict == null) {
			URL url;
			try {
				url = new URL(wnhome);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			dict = new Dictionary(url);
			try {
				dict.open();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		}
		return dict;
	}

	public static IDictionary getDictionary() {
		return getDictionary(wnhome);
	}

	public static List<String> getSynonyms(String term) {

		List<String> syn = new ArrayList<String>();
		if (term == null || term.trim().length() == 0) {
			return syn;
		}
		if (term.split(" ").length > 1) {
			return syn;
		}

		// look up first sense of the word " dog "
		IIndexWord idxWord = getDictionary().getIndexWord(term, POS.NOUN);

		if (idxWord == null || idxWord.getWordIDs().isEmpty()) {
			IStemmer stemmer = new WordnetStemmer(dict);
			List<String> stems = stemmer.findStems(term, POS.NOUN);
			if (stems.size() > 0) {
				term = stems.get(0);
				idxWord = getDictionary().getIndexWord(term, POS.NOUN);
			}

		}
		if (idxWord == null || idxWord.getWordIDs().size() == 0) {
			return syn;
		}

		IWordID wordID = idxWord.getWordIDs().get(0); // 1 st meaning
		IWord word = getDictionary().getWord(wordID);
		ISynset synset = word.getSynset();
		// iterate over words associated with the synset
		for (IWord w : synset.getWords()) {
			String text = w.getLemma().toLowerCase();
			if (!text.contains("_")) {
				syn.add(text);
			}
		}
		return syn;
	}

	public static List<String> getHypernyms(String term) {
		// get the synset
		List<String> hyp = new ArrayList<String>();
		if (term == null || term.trim().length() == 0) {
			return hyp;
		}
		if (term.split(" ").length > 1) {
			return hyp;
		}
		IIndexWord idxWord = getDictionary().getIndexWord(term, POS.NOUN);

		if (idxWord == null || idxWord.getWordIDs().isEmpty()) {
			IStemmer stemmer = new WordnetStemmer(dict);
			List<String> stems = stemmer.findStems(term, POS.NOUN);
			if (stems.size() > 0) {
				term = stems.get(0);
				idxWord = getDictionary().getIndexWord(term, POS.NOUN);
			}

		}

		if (idxWord == null || idxWord.getWordIDs().size() == 0) {
			return hyp;
		}

		IWordID wordID = idxWord.getWordIDs().get(0); // 1 st meaning
		IWord word = getDictionary().getWord(wordID);
		ISynset synset = word.getSynset();
		// get the hypernyms
		List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
		// print out each h y p e r n y m s id and synonyms

		List<IWord> words;
		for (ISynsetID sid : hypernyms) {
			words = dict.getSynset(sid).getWords();
			for (Iterator<IWord> i = words.iterator(); i.hasNext();) {
				String text = i.next().getLemma().toLowerCase();
				if (!text.contains("_")) {
					hyp.add(text);
				}
			}
		}
		return hyp;
	}

}
