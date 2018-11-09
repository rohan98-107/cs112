package lse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.lang.InterruptedException;

public class Grader extends LittleSearchEngine {
	private static final int timeoutSecs=5;

	/**
	 * This method indexes all keywords found in all the input documents. When this
	 * method is done, the keywordsIndex hash table will be filled with all keywords,
	 * each of which is associated with an array list of Occurrence objects, arranged
	 * in decreasing frequencies of occurrence.
	 *
	 * @param docsFile Name of file that has a list of all the document file names, one name per line
	 * @param noiseWordsFile Name of file that has a list of noise words, one noise word per line
	 * @throws FileNotFoundException If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.add(word);
		}

		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String, Occurrence> kws = loadKeywordsFromDocument(docFile);
			mergeKeywords(kws);
		}
		sc.close();
	}

	private void timeMergeKeywords(HashMap<String, Occurrence> kws) throws ExecutionException,TimeoutException,InterruptedException {
		// System.out.println("Test mergeKeywords");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(()->super.mergeKeywords(kws));
		executor.shutdown(); // This does not cancel the already-scheduled task.

		try {
			future.get(timeoutSecs, TimeUnit.SECONDS);
			future.cancel(true);
		}
		catch(TimeoutException e){
			System.out.println("Student's code timed out. (mergeKeywords)");
			future.cancel(true);
			throw e;
		}
		finally{
			future.cancel(true);
		}
	}
	//Only mergeKeyWords is replaced by base class method
	public void makeIndexTest(String docsFile, String noiseWordsFile) throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.add(word);
		}

		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String, Occurrence> kws = loadKeywordsFromDocument(docFile);
			try {
				timeMergeKeywords(kws);
			} catch (Exception e) {
				sc.close();
				throw new IllegalArgumentException();
			}
		}
		sc.close();
	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of keyword occurrences
	 * in the document. Uses the getKeyWord method to separate keywords from other words.
	 *
	 * @param docFile Name of the document file to be scanned and loaded
	 * @return Hash table of keywords associated with Occurrences in the given document
	 * @throws FileNotFoundException If the document file is not found on disk
	 */
	public HashMap<String, Occurrence> loadKeywordsFromDocument(String docFile) throws FileNotFoundException {
		HashMap<String, Occurrence> keyWords = new HashMap<String, Occurrence>(1000, 2.0f);
		Scanner sc = new Scanner(new File(docFile));

		while (sc.hasNext()) {
			String word = sc.next();
			String keyWord = getKeyword(word);
			if (keyWord == null) {
				//noises++;
				continue;
			}
			//signal++;
			Occurrence occ = keyWords.get(keyWord);
			if (occ == null) { // first occurrence of key word in this doc
				occ = new Occurrence(docFile, 1);
				keyWords.put(keyWord, occ);
			} else {
				occ.frequency++;
			}
		}
		sc.close();
		return keyWords;
	}

	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document
	 * must be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash table.
	 * This is done by calling the insertLastOccurrence method.
	 *
	 * @param kws Keywords hash table for a document
	 */
	public void mergeKeywords(HashMap<String, Occurrence> kws) {
		Set<Map.Entry<String, Occurrence>> set = kws.entrySet();
		Iterator<Map.Entry<String, Occurrence>> iter = set.iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Occurrence> entry = iter.next();
			String kw = entry.getKey();
			Occurrence occ = entry.getValue();
			ArrayList<Occurrence> occs = keywordsIndex.get(kw);
			if (occs == null) { // does not exist
				// create a new list, add this Occurrence
				occs = new ArrayList<Occurrence>();
				occs.add(occ);
				// insert into table
				keywordsIndex.put(kw, occs);
			} else { // keyword exists, add this occ to end and the insert in right spot
				occs.add(occ);
				insertLastOccurrence(occs);
			}
		}
	}

	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped of any
	 * trailing punctuation, consists only of alphabetic letters, and is not
	 * a noise word. All words are treated in a case-INsensitive manner.
	 *
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 *
	 * @param word Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyword(String word) {
		// trim trailing punctuations - . or , or ? or ! or : or ;

		int p = 0;
		char ch = word.charAt(word.length() - (p + 1));
		while (ch == '.' || ch == ',' || ch == '?' || ch == ':' || ch == ';' || ch == '!') {
			p++;
			int index = word.length() - (p + 1);
			if (index == -1) {
				pw.println(word);
				System.out.flush();
			}
			ch = word.charAt(word.length() - (p + 1));
		}
		word = word.substring(0, word.length() - p);

		// are all characters alphabetic letters?
		for (int i = 0; i < word.length(); i++) {
			if (!Character.isLetter(word.charAt(i))) {
				return null;
			}
		}
		word = word.toLowerCase();
		if (noiseWords.contains(word)) {
			return null;
		}
		return word;
	}

	/**
	 * Inserts the last occurrence in the parameter list in the correct position in the
	 * list, based on ordering occurrences on descending frequencies. The elements
	 * 0..n-2 in the list are already in the correct order. Insertion is done by
	 * first finding the correct spot using binary search, then inserting at that spot.
	 *
	 * @param occs List of Occurrences
	 * @return Sequence of mid points in the list checked by the binary search process,
	 *         null if the size of the list is 1. This returned list is only used to test
	 *         your code - it is not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		// if list is size 1, return
		if (occs.size() == 1) {
			return null;
		}
		// do a binary search of list 0..size-2 to find the correct spot for last entry
		// ordering is on descending frequencies
		ArrayList<Integer> ret = new ArrayList<Integer>();
		Occurrence last = occs.get(occs.size() - 1);
		int lo = 0, hi = occs.size() - 2, freq = last.frequency, pos = 0;
		while (lo <= hi) {
			int mid = (lo + hi) / 2;
			ret.add(mid);
			int f = occs.get(mid).frequency;
			if (f == freq) {
				pos = mid;
			}
			if (freq < f) {
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		if (lo > hi) {
			pos = lo;
		}
		// move everything in the list to the right one step, pos..last-2
		for (int i = occs.size() - 2; i >= pos; i--) {
			occs.set(i + 1, occs.get(i));
		}
		occs.set(pos, last);
		return ret;
	}

	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or kw2 occurs in that
	 * document. Result set is arranged in descending order of document frequencies. (Note that a
	 * matching document will only appear once in the result.) Ties in frequency values are broken
	 * in favor of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and kw2 is in doc2
	 * also with the same frequency f1, then doc1 will take precedence over doc2 in the result.
	 * The result set is limited to 5 entries.
	 *
	 * @param kw1 First keyword
	 * @param kw1 Second keyword
	 * @return List of documents in which either kw1 or kw2 occurs, arranged in descending order of
	 *         frequencies. The result size is limited to 5 documents.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) {
		ArrayList<Occurrence> al1 = keywordsIndex.get(kw1);
		ArrayList<Occurrence> al2 = keywordsIndex.get(kw2);
		pw.println(al1);
		pw.println(al2);

		// if both lists are null then return null
		if (al1 == null && al2 == null) {
			return null;
		}

		ArrayList<String> res = new ArrayList<String>();

		// if only one is null, return (at most) first 5 from the other
		ArrayList<Occurrence> temp = null;
		if (al1 == null || al2 == null || al2.size()==0 || al2.size()==0) {
			if (al1 == null || al1.size()==0) {
				temp = al2;
			} else {
				temp = al1;
			}
			int i = 0;
			while (res.size() < 5 && i < temp.size()) {
				Occurrence occ = temp.get(i);
				res.add(occ.document);
				i++;
			}
			return res;
		}

		int j = 0, k = 0;
		while (res.size() < 5 && j < al1.size() && k < al2.size()) {
			Occurrence occ1 = al1.get(j);
			Occurrence occ2 = al2.get(k);
			if (occ1.frequency < occ2.frequency) {
				boolean found = false;
				for (int i = 0; i < res.size(); i++) {
					if (occ2.document.equals(res.get(i))) {
						found = true;
						break;
					}
				}
				if (!found) {
					res.add(occ2.document);
				}
				k++;
			} else {
				boolean found = false;
				for (int i = 0; i < res.size(); i++) {
					if (occ1.document.equals(res.get(i))) {
						found = true;
						break;
					}
				}
				if (!found) {
					res.add(occ1.document);
				}
				j++;
			}
		}
		// if fewer than 5 in result, and left over docs in either, copy over
		if (res.size() < 5) {
			int i = 0;
			if (j < al1.size()) {
				temp = al1;
				i = j;
			} else {
				temp = al2;
				i = k;
			}
			while (res.size() < 5 && i < temp.size()) {
				boolean found = false;
				for (String doc : res) {
					if (doc.equals(temp.get(i).document)) {
						found = true;
						break;
					}
				}
				if (!found) {
					res.add(temp.get(i).document);
					i++;
				}
			}
		}

		return res;
	}

	public static final String[] keywordTests = { "sWord", "paraphrase;", "really?!?!", "Between,", "either:or" };
	public static final String[] keywordTestResults = { "sword", "paraphrase", "really", null, null };
	public static final int[] keywordTestPoints = { 2, 2, 2, 2, 2 };

	private String timeGetKeyword(String keyword,PrintWriter pw) throws ExecutionException,TimeoutException,InterruptedException {
		// System.out.println("Test getKeyword");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(()->super.getKeyword(keyword));
		executor.shutdown(); // This does not cancel the already-scheduled task.
		try{
			String result=(String)future.get(timeoutSecs, TimeUnit.SECONDS);
			future.cancel(true);
			return result;
		}finally{
			future.cancel(true);
		}
	}

	public int getKeywordTest() {
		String result;
		int Points = 0;

		pw.println("-----------------");
		pw.println("method getKeyword");

		for (int k = 0; k < keywordTests.length; k++) {
			pw.println("\nTest " + (k + 1) + ": " + keywordTests[k]);
			pw.println("Expected Result: " + keywordTestResults[k]);

			try {
				try{
					result = timeGetKeyword(keywordTests[k],pw);
				}
				catch(TimeoutException e){
					System.out.println("Student's code timed out. (getKeyword)");
					pw.println("Code timed out.");
					pw.println("Points: 0");
					continue;
				}
				catch(Exception e){
					throw e;
				}
			} catch (Exception e) {
				pw.println("Your program threw an exception");
				pw.println("Points: 0");
				continue;
			}
			pw.println("Your Result: " + result);

			if (keywordTestResults[k] == null) {
				if (result == null) {
					pw.println("Points: " + keywordTestPoints[k]);

					Points += keywordTestPoints[k];
				} else {
					pw.println("Points: 0");
				}
				continue;
			}

			if (result != null && result.equals(keywordTestResults[k])) {
				pw.println("Points: " + keywordTestPoints[k]);
				Points += keywordTestPoints[k];
			} else {
				pw.println("Points: 0");
			}
		}

		pw.println("\nTotal: " + Points);
		return Points;
	}

	public static final int[] insertLastTestInitFreqs = { 82, 76, 71, 71, 70, 65, 61, 56, 54, 51, 48, 45, 41, 36, 34,
			30, 25, 20, 20, 18, 17, 17, 14, 12 };
	public static final String[] insertLastTestNames = { "Extreme left insertion", "Extreme right insertion",
			"In between insertion 1", "In between insertion 2", "Insertion with a frequency match" };

	public static final String[] insertLastTestDocs = { "d25.txt", "d25.txt", "d25.txt", "d25.txt", "d25.txt" };
	public static int[] insertLastTestFreqs = { 85, 4, 50, 26, 17 };
	public static int[][] insertLastTestResults = { { 11, 5, 2, 0 }, { 11, 17, 20, 22, 23 }, { 11, 5, 8, 9, 10 },
			{ 11, 17, 14, 15, 16 }, { 11, 17, 20 } };
	public static final int[] insertLastTestPoints = { 2, 2, 2, 2, 2 };

	private ArrayList<Integer> timeInsertLastOccurrence(ArrayList<Occurrence> list,PrintWriter pw) throws ExecutionException,TimeoutException,InterruptedException {
		// System.out.println("Test insertLastOccurence");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(()->super.insertLastOccurrence(list));
		executor.shutdown(); // This does not cancel the already-scheduled task.
		try{
			ArrayList<Integer> result=(ArrayList<Integer>)future.get(timeoutSecs, TimeUnit.SECONDS);
			future.cancel(true);
			return result;
		}finally{
			future.cancel(true);
		}
	}

	public int insertLastTest() {
		int Points = 0;
		ArrayList<Occurrence> occsList = new ArrayList<Occurrence>();
		// add documents, with frequencies
		for (int i = 0; i < insertLastTestInitFreqs.length; i++) {
			occsList.add(new Occurrence("d" + (i + 1) + ".txt", insertLastTestInitFreqs[i]));
		}

		pw.println("---------------------------");
		pw.println("method insertLastOccurrence");

		for (int i = 0; i < insertLastTestDocs.length; i++) {
			pw.println("");
			pw.println("Test " + (i + 1) + ": " + insertLastTestNames[i]);
			pw.println("");

			ArrayList<Occurrence> temp = new ArrayList<Occurrence>(occsList);
			temp.add(new Occurrence(insertLastTestDocs[i], insertLastTestFreqs[i]));
			pw.println("Occurrence list:");
			pw.println(temp);
			ArrayList<Integer> res = null;
			ArrayList<Integer> correct = new ArrayList<Integer>();
			for (int j = 0; j < insertLastTestResults[i].length; j++) {
				correct.add(insertLastTestResults[i][j]);
			}
			pw.println("Expected result: " + correct);
			try {
				try{
					res = timeInsertLastOccurrence(temp,pw);
				}catch(TimeoutException e){
					System.out.println("Student's code timed out. (insertLastOccurrence)");
					pw.println("Code timed out.");
					pw.println("Points: 0");
					continue;
				}
			} catch (Exception e) {
				pw.println("Your program threw an exception");
				pw.println("Points: 0");
				continue;
			}

			pw.println("Result: " + res);
			if (correct.equals(res)) {
				pw.println("Points: " + insertLastTestPoints[i]);
				Points += insertLastTestPoints[i];
			} else {
				pw.println("Points: 0");
			}

		}

		pw.println("");
		pw.println("Total: " + Points);

		return Points;
	}

	public static final String[] loadKeywordsTestNames = { "Lots of noise", "Lots of keywords",
			"Repetition of keywords" };

	public static final String[] loadKeywordsTestDocs = { "pohlx.txt", "Tyger.txt", "jude.txt" };

	public static final int[] loadKeywordsTestPoints = { 5, 5, 5 };

	private HashMap<String, Occurrence> timeLoadKeywordsFromDocument(String doc,PrintWriter pw) throws ExecutionException,TimeoutException,InterruptedException {
		// System.out.println("Test loadKeywordsFromDocument");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(()->super.loadKeywordsFromDocument(doc));
		executor.shutdown(); // This does not cancel the already-scheduled task.

		try {
			HashMap<String, Occurrence> result=(HashMap<String, Occurrence>)future.get(timeoutSecs, TimeUnit.SECONDS);
			future.cancel(true);
			return result;
		}
		finally{
			future.cancel(true);
		}
	}

	public int loadKeyWordsTest() throws FileNotFoundException {
		int Points = 0, testPoints = 0;

		HashMap<String, Occurrence> correct;
		HashMap<String, Occurrence> result;

		pw.println("-----------------");
		pw.println("method loadKeywordsFromDocument");

		for (int i = 0; i < loadKeywordsTestDocs.length; i++) {
			pw.println("");

			pw.println("Test " + (i + 1) + ": " + loadKeywordsTestNames[i] + " - " + loadKeywordsTestDocs[i]);

			correct = loadKeywordsFromDocument(loadKeywordsTestDocs[i]);
			try {
				try{
					result = timeLoadKeywordsFromDocument(loadKeywordsTestDocs[i],pw);
				}
				catch(TimeoutException e){
					System.out.println("Student's code timed out. (loadKeywordsFromDocument)");
					pw.println("Code timed out.");
					pw.println("Points: 0");
					continue;
				}
			} catch (Exception e) {
				pw.println("Your program threw an exception");
				pw.println(e.getMessage());
				pw.println("Points: 0");
				continue;
			}

			testPoints = loadKeywordsTestPoints[i];
			if (result == null || (correct.size() != result.size())) {
				pw.println("Hashmaps don't match in size!");
				testPoints = 0;
				if (result != null) {
					pw.println("Hashmap sizes: expected = " + correct.size() + ", your result = " + result.size());

				} else {
					pw.println("Result was null");
				}
			} else {
				testPoints = loadKeywordsTestPoints[i];
				for (Map.Entry<String, Occurrence> entry : correct.entrySet()) {
					Occurrence temp = null;

					temp = (Occurrence) result.get(entry.getKey());
					if (temp == null || temp.frequency != entry.getValue().frequency) {
						if (temp != null) {
							pw.println("Mismatch: " + entry.getKey() + ":" + temp.frequency + " to "
									+ entry.getValue().frequency);
						} else {
							pw.println("No value for key" + entry.getKey());
						}
						testPoints = 0;
					}
				}
				if (testPoints == loadKeywordsTestPoints[i]) {
					pw.println("Results match");
				}
			}

			pw.println("Points: " + testPoints);
			Points += testPoints;
		}

		pw.println("");
		pw.println("Total: " + Points);
		return Points;
	}

	public static final String[][] top5TestKeywords = { { "strange", "case" }, { "color", "strange" },
			{ "orange", "weird" }, { "red", "orange" }, { "red", "car" }, };

	public static final int[] top5TestPoints = { 2, 3, 5, 6, 6 };

	public static final String[] top5TestNames = { "No matches for either",
			"No match for 1 keyword, but more than 5 total matches",
			"Matches on both keywords, no common docs, more than 5 total matches",
			"4 common docs, no common result frequencies in top 5, more than 5 total matches",
			"2 docs in different lists with the same frequency, more than 5 results" };

	private ArrayList<String> timeTop5search(String keyword1, String keyword2,PrintWriter pw) throws ExecutionException,TimeoutException,InterruptedException {
		// System.out.println("Test top5search");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(()->super.top5search(keyword1,keyword2));
		executor.shutdown(); // This does not cancel the already-scheduled task.

		try{
			ArrayList<String> result=(ArrayList<String>)future.get(timeoutSecs, TimeUnit.SECONDS);
			future.cancel(true);
			return result;
		}finally{
			future.cancel(true);
		}
	}

	public int top5SearchTest() {
		ArrayList<String> corr, res;
		int Points = 0;

		pw.println("---------------");
		pw.println("method top5Search");

		for (int i = 0; i < top5TestNames.length; i++) {
			int testPoints = top5TestPoints[i];

			pw.println("");
			pw.println("Test " + (i + 1) + " (" + top5TestKeywords[i][0] + "," + top5TestKeywords[i][1] + "): "
					+ top5TestNames[i]);
			pw.println("");

			try {
				try{
					res = timeTop5search(top5TestKeywords[i][0], top5TestKeywords[i][1],pw);
				}catch(TimeoutException e){
					System.out.println("Student's code timed out. (top5search)");
					pw.println("Code timed out.");
					pw.println("Points: 0");
					continue;
				}
			} catch (Exception e) {
				pw.println("Your program threw an exception");
				pw.println("Points: 0");
				continue;
			}
			try {
				corr = top5search(top5TestKeywords[i][0], top5TestKeywords[i][1]);
			} catch (Exception e) {
				pw.println("Your program threw an exception");
				pw.println("Points: 0");
				continue;
			}
			pw.println("Your result:" + res);
			boolean over5 = false;
			if (corr == null || corr.size() == 0) {
				pw.println("Expected result: No list");
				if (res != null) {
					for (int r = 0; r < res.size(); r++) {
						if (res.get(r) != null) {
							testPoints = 0;
							break;
						}
					}
				}
			} else {
				pw.println("Expected result: " + corr);
				// check the top 5 only
				if (res != null && res.size() > 5) {
					testPoints -= testPoints / 2;
					over5 = true;
					while (res.size() > 5) {
						res.remove(5);
					}
				}
				if (!corr.equals(res)) {
					testPoints = 0;
				}
			}

			pw.print("Points: " + testPoints);
			if (over5) {
				pw.println(" (penalty for > 5 docs in result)");
			} else {
				pw.println();
			}
			Points += testPoints;
		}

		pw.println("\nTotal: " + Points);
		return Points;
	}

	static PrintWriter pw;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		pw = new PrintWriter(new FileWriter("report.txt"));

		Grader lset = new Grader();
		int getKeyWordPoints = 0;
		int insertLastPoints = 0;
		int loadKeyWordsPoints = 0;
		int mergePoints = 0;
		int top5Points = 0;

		pw.println("Little Search Engine Grade Report\n\n");

		try {
			lset.makeIndex("docsNone.txt", "noisewords.txt");
			getKeyWordPoints = lset.getKeywordTest();
			loadKeyWordsPoints = lset.loadKeyWordsTest();
			insertLastPoints = lset.insertLastTest();
			mergePoints = mergeKeywordsTest();
			lset = new Grader();
			try {
				lset.makeIndex("docs.txt", "noisewords.txt");
				top5Points = lset.top5SearchTest();
			} catch (FileNotFoundException e) {
				pw.println("Cannot find the files");
			}
			int finalScore = getKeyWordPoints + insertLastPoints + loadKeyWordsPoints + mergePoints + top5Points;

			pw.println("\n------------------\n");
			pw.println("Total Points = " + finalScore + "/80");
		} catch (FileNotFoundException e) {
			pw.println("Cannot find the files");
		}

		pw.close();
		System.exit(0);
	}

	public static final String[] mergeKeywordsTestNames = { "Single document", "2 documents with no intersection",
			"2 documents with a few intersections" };

	public static final String[][] mergeKeywordsTestDocs = { { "metamorphosis.txt" },
			{ "Tyger.txt", "metamorphosis.txt" }, { "pohlx.txt", "pohly.txt" } };

	public static final String[] mergeKeywordsTestDocFiles = { "makeIndexDocs1.txt", "makeIndexDocs2.txt",
			"makeIndexDocs3.txt" };

	public static final int[] mergeKeywordsTestPoints = { 7, 8, 8 };

	public static int mergeKeywordsTest() throws FileNotFoundException {

		Grader lset1, lset2;
		int mergePoints = 0;

		//Merge Keyword tests

		pw.println("------------------");
		pw.println("method mergeKeywords");

		for (int i = 0; i < mergeKeywordsTestNames.length; i++) {
			pw.print("\nTest " + (i + 1) + ": " + mergeKeywordsTestNames[i] + " - ");
			pw.print(mergeKeywordsTestDocs[i][0]);
			for (int d = 1; d < mergeKeywordsTestDocs[i].length; d++) {
				pw.print(", " + mergeKeywordsTestDocs[i][d]);
			}
			pw.println();

			lset1 = new Grader();
			lset2 = new Grader();

			HashMap<String, ArrayList<Occurrence>> correct, result;
			int testPoints = mergeKeywordsTestPoints[i];

			lset2.makeIndex(mergeKeywordsTestDocFiles[i], "noisewords.txt");
			try {
				lset1.makeIndexTest(mergeKeywordsTestDocFiles[i], "noisewords.txt");
			} catch (IllegalArgumentException e) {
				pw.println("Your program threw an exception");
				pw.println("Points: 0");
				continue;
			}

			//Compare hashmaps of lset1 and lset2
			correct = lset2.keywordsIndex;
			result = lset1.keywordsIndex;
			if (result == null || result.size() != correct.size()) {
				pw.println("Size mismatch between hashmaps");
				if (result != null) {
					pw.println("Hashmap sizes: expected = " + correct.size() + ", your result = " + result.size());
				}
				testPoints = 0;
			} else {

				for (Map.Entry<String, ArrayList<Occurrence>> entry : correct.entrySet()) {
					ArrayList<Occurrence> temp, corr;
					temp = (ArrayList<Occurrence>) result.get(entry.getKey());
					corr = entry.getValue();

					if ((temp == null) || (temp.size() != corr.size())) {
						pw.println("Mismatch for key " + entry.getKey() + ": expected value = " + corr
								+ ", your value = " + temp);
						testPoints = 0;
					} else {
						for (int k = 0; k < corr.size(); k++) {
							if (!corr.get(k).document.equals(temp.get(k).document)) {
								pw.println("Mismatch for key: " + entry.getKey() + ": expected value = " + corr
										+ ", your value = " + temp);
								testPoints = 0;
								break;
							}
						}
					}
				}
			}

			mergePoints += testPoints;
			pw.println("Points: " + testPoints);
		}
		pw.println("\nTotal: " + mergePoints);
		return mergePoints;
	}
}
