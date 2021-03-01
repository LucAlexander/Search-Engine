import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.select.*;
import org.jsoup.nodes.*;

public class Indexer{
	String fileDirectory;
	// map<pair<url, file>, map<word, count>>
	HashMap<String, IndexedDoc> index = new HashMap<String, IndexedDoc>();
	String schema;
	Pattern pattern;
	Stemmer stemmer;
	public static void main(String[] args){
		// Temporary entrypoint
		Indexer dexter = new Indexer();
		dexter.start();
		System.out.println("Program exited with code 0\n");
	}
	Indexer(){
		// Initialize indexer
		// regex scheme to tokenize text and remove stop words
		schema = "\\b(?!(is|to|if|it|and|the|where|how|what|or|i|a)\\s)\\b[A-Za-z0-9_]+";
		pattern = Pattern.compile(schema);
		stemmer = new Stemmer();
	}
	private void start(){
		// Creates a reverse-index file containing entries in the format:
		// pagename.html | {word : count} {word : count} ... \n
		try(
			BufferedWriter writer = new BufferedWriter(new FileWriter("reverseIndex.txt"));
		){
			// tokenize and store information about each file	
			for (int i = 0;i<1000;++i){
				String file = "../crawler/pages/page"+i+".html";
				addFile(file);
			}
			// Serialize content of index map into file
			for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
				String line = "";
				line += set.getKey() + " | ";
				HashMap<Term, Integer> cont = set.getValue().getContents();
				for (Map.Entry<Term, Integer> subset : cont.entrySet()){
					line += "{"+subset.getKey().getStem()+" : ";
					line += subset.getValue()+"} ";
				}
				line += "\n";
				writer.write(line);
			}
			writer.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public int getDocLength(String fileName){
		//returns the amount of unique terms in a document
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			if (fileName == set.getKey()){
				return set.getValue().getTotalTerms();
			}
		}
		return -1;
	}
	public int getDocFreq(Term t){
		// returns the number of documents a unique term appears in
		int freq = 0;
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			if (set.getValue().containsTerm(t)){
				freq++;
			}
		}
		return freq;
	}
	public int getTermFreq(String fileName, Term t){
		// returns the frequency of a term in a document
		int freq = 0;
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			if (set.getKey()==fileName){
				return set.getValue().termCount(t);
			}
		}
		return freq;
	}
	public int getDocCount(){
		// returns the number of documents in our index
		return index.size();
	}
	public int getTermNo(String file){
		// the number of distinct terms in a document
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			if (set.getKey() == file){
				return set.getValue().getLength();
			}
		}
		return 0;
	}
	public int getTermTotal(){
		// the total number of appearences of all terms in all documents
		int total = 0;
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			total += set.getValue().getTotalTerms();
		}
		return total;
	}
	public int getTotalTermFrequency(Term t){
		int total = 0;
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			total += set.getValue().termCount(t);
		}
		return total;
	}
	private String processWord(String word){
		/* Stemms the given word
		 * returns the root of the word, or the word if no further root exists
		*/
		for (int i = 0;i<word.length();++i){
			stemmer.add(word.charAt(i));
		}
		stemmer.stem();
		return stemmer.toString();
	}
	public Term[] getTerms(String file){
		// retrieve all terms of a document by file reference
		for (Map.Entry<String, IndexedDoc> set : index.entrySet()){
			if (set.getKey()==file){
				return set.getValue().getTerms();
			}
		}
		return null;
	}
	public Term[] getTermByDoc(IndexedDoc d){
		// retrieve all terms of a document by document reference
		return d.getTerms();
	}
	private void addFile(String file){
		/* Parses file for html text content and counts stemmed words
		 * Stores in index map
		 * String url: the matching url for the files sources
		 * String file: the file to parse
		*/
		try(
			BufferedReader reader = new BufferedReader(new FileReader(file));
		){
			String html = "";
			String line = "";
			// read in html from passed file argument
			while((line=reader.readLine())!=null){
				html += line;
			}
			// retrieve all text content elements from page
			Elements paragraphs = Jsoup.parse(html).select("p");
			String content = "";
			for (Element i : paragraphs){
				content += i.text()+" ";
			}
			// tokenize and log all unique words
			Matcher matcher = pattern.matcher(content);
			String[] results = matcher.results().map(MatchResult::group).toArray(String[]::new);
			HashMap<String, Integer> count = new HashMap<String, Integer>();
			for (int i = 0;i<results.length;++i){
				String word = results[i];
				if (count.get(word) != null){
					count.put(word, count.get(word)+1);
				}
				else{
					count.put(word, 1);
				}
			}
			//create term objects with ids
			HashMap<Term, Integer> rCount = new HashMap<Term, Integer>();
			int id = 0;
			for (Map.Entry<String, Integer> subset : count.entrySet()){
				rCount.put(new Term(processWord(subset.getKey()),id), subset.getValue());
			}
			// store data from file
			index.put(file, new IndexedDoc(rCount));
			reader.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}

