package lse;

import java.io.*;
import java.util.*;


public class LittleSearchEngine {
	
	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and the associated value is
	 * an array list of all occurrences of the keyword in documents. The array list is maintained in 
	 * DESCENDING order of frequencies.
	 */
	
	HashMap<String,ArrayList<Occurrence>> keywordsIndex;
	HashSet<String> noiseWords;
	
	
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String,ArrayList<Occurrence>>(1000,2.0f);
		noiseWords = new HashSet<String>(100,2.0f);
	}
	
// GRADED METHODS BEGIN 
	 @SuppressWarnings("resource") 
	public HashMap<String,Occurrence> loadKeywordsFromDocument(String docFile) 
	throws FileNotFoundException 
	{
		HashMap< String, Occurrence > resultMap = new HashMap< String, Occurrence >();
		FileInputStream fileIn = new FileInputStream( docFile );
		Scanner sc1 = new Scanner ( fileIn );
		
		while ( sc1.hasNext() )
		{
			String wordPtr = sc1.next();
			wordPtr = makeCorrectformat( wordPtr );
			if ( getKeyword( wordPtr ) != null )
			{	
				if ( resultMap.containsKey( wordPtr ) )
				{
					Occurrence temp1 = resultMap.get( wordPtr );
					temp1.frequency++;
					resultMap.put( wordPtr, temp1 );
				}
				else
				{
					Occurrence temp2 = new Occurrence ( docFile , 1 );
					resultMap.put( wordPtr , temp2 );
				}
			}
		}
		
		return resultMap;
	}
	

	public void mergeKeywords(HashMap<String,Occurrence> kws) 
	{
		for ( Map.Entry<String, Occurrence> entry: kws.entrySet() )
		{
			if ( ! keywordsIndex.containsKey( entry.getKey() ) )
			{
				ArrayList<Occurrence> temp = new ArrayList<Occurrence>();
				temp.add(entry.getValue());
				keywordsIndex.put( entry.getKey() , temp );
			}
			else
			{
				keywordsIndex.get( entry.getKey() ).add( entry.getValue() );
				insertLastOccurrence( keywordsIndex.get( entry.getKey() ) );
			}
		}
		
		for ( Map.Entry< String, ArrayList<Occurrence>> entry2: keywordsIndex.entrySet() )
		{
			descendingSort( entry2.getValue() );
		}
	}
	
	
	public String getKeyword(String word) throws FileNotFoundException 
	{
		FileInputStream fileIn = new FileInputStream( "noisewords.txt" );
		@SuppressWarnings("resource") Scanner sc = new Scanner( fileIn );
		
		if ( word == null )
			return null;
		
		while ( sc.hasNextLine() )
		{
			String linePtr = sc.nextLine();

			if ( isSameWord( linePtr , word ) )
				return null;
		}
		

		return makeCorrectformat(word);
	}
	
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) 
	{
		ArrayList<Integer> mids = new ArrayList<Integer>();
		// ArrayList<Integer> temps = new ArrayList<Integer>();
		
		int hi = occs.size() - 2;
		int lo = 0;
		int mid = 0;
		int targetFreq = occs.get( occs.size() - 1 ).frequency;
		
		if ( occs.size() <= 1 )
			return null;
		
		while ( lo <= hi )
		{
			mid = (lo + hi)/2;
			mids.add(mid);
			int temp = occs.get(mid).frequency;
			
			if ( temp < targetFreq )
				hi = mid - 1;
			else if ( temp > targetFreq )
			{
				lo = mid + 1;
				if ( mid > hi + 1 )
					mid++;
			}
			else
				break;
		}

		mids.add(mid);
		Occurrence last = new Occurrence(null, 0);
		last = occs.get(occs.size() - 1);
		occs.remove(occs.size() - 1);
		occs.add( mids.get(mids.size() - 1), last );
		
		return mids;
	}
	
	public void makeIndex(String docsFile, String noiseWordsFile) 
	throws FileNotFoundException {
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
			HashMap<String,Occurrence> kws = loadKeywordsFromDocument(docFile);
			mergeKeywords(kws);
		}
		sc.close();
	}
	
	public ArrayList<String> top5search(String kw1, String kw2) 
	{
		ArrayList<Occurrence> unionSet = new ArrayList<Occurrence>();
		ArrayList<String> TopFive = new ArrayList<String>();
		
		int i = 0; 
		for ( Map.Entry<String, ArrayList<Occurrence>> entry: keywordsIndex.entrySet() )
		{
			if ( entry.getKey().equals(kw1) || entry.getKey().equals(kw2) )
			{
				unionSet.addAll( entry.getValue() );
				i++;
			}
			
			if ( i >= 2 )
				break;
		}
		
		if ( unionSet.isEmpty() )
			return null;
		
		descendingSort( unionSet );
		
		for( int j = 0; j < Math.min(5, unionSet.size()); j++ )
		{
			if ( TopFive.contains( unionSet.get(j).document ) )
				continue;
			else
				TopFive.add( unionSet.get(j).document );
		}
		
		return TopFive;
	
	}

// GRADED METHODS END 

// HELPER METHODS 
	
	private boolean isSameWord( String str1, String str2 )
	{
		String temp1 = str1;
		String temp2 = str2;
		
		temp1 = temp1.replace(".", "" ); temp2 = temp2.replace(".", "" );
		temp1 = temp1.replace(",", "" ); temp2 = temp2.replace(",", "" );
		temp1 = temp1.replace("?", "" ); temp2 = temp2.replace("?", "" );
		temp1 = temp1.replace(":", "" ); temp2 = temp2.replace(":", "" );
		temp1 = temp1.replace(";", "" ); temp2 = temp2.replace(";", "" );
		temp1 = temp1.replace("!", "" ); temp2 = temp2.replace("!", "" );

		temp1 = temp1.trim(); temp2 = temp2.trim();
		
		if ( temp1.compareToIgnoreCase(temp2) == 0 )
			return true;
		else
			return false;
	}
	private String makeCorrectformat( String str )
	{
		str = str.replace(".", " " ); 
		str = str.replace(",", " " ); 
		str = str.replace("?", " " ); 
		str = str.replace(":", " " );
		str = str.replace(";", " " ); 
		str = str.replace("!", " " );
		str = str.replace("'", " ");


		str = str.trim();
		str = str.toLowerCase();
		
	
		if ( str.chars().allMatch( Character::isLetter ) && !str.equals("") )
			return str;
		else
			return null;

	}
	private void descendingSort( ArrayList< Occurrence > occs )
	{
		int n = occs.size();
		
		 for ( int i = 0; i < n; i++ )
		{
			for ( int j = i + 1; j < n; j++ )
			{
				if ( occs.get(i).frequency < occs.get(j).frequency )
				{
					Collections.swap( occs, i, j );
				}
			}
		}
	}

// HELPER METHODS END
}