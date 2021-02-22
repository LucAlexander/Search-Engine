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
	HashMap<String, HashMap<String, Integer>> index = new HashMap<String, HashMap<String, Integer>>();
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
			for (Map.Entry<String, HashMap<String, Integer>> set : index.entrySet()){
				String line = "";
				line += set.getKey() + " | ";
				for (Map.Entry<String, Integer> subset : set.getValue().entrySet()){
					line += "{"+subset.getKey()+" : ";
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
			Document doc = Jsoup.parse(html);
			Elements paragraphs = doc.select("p");
			String content = "";
			for (Element i : paragraphs){
				content += i.text()+" ";
			}
			// tokenize and log all unique words
			Matcher matcher = pattern.matcher(content);
			String[] results = matcher.results().map(MatchResult::group).toArray(String[]::new);
			HashMap<String, Integer> count = new HashMap<String, Integer>();
			for (int i = 0;i<results.length;++i){
				String word = processWord(results[i]);
				if (count.get(word) != null){
					count.put(word, count.get(word)+1);
				}
				else{
					count.put(word, 1);
				}
			}
			// store data from file
			index.put(file, count);
			reader.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
