import java.io.*;
import java.net.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.util.*;

public class Crawler{
	// Seed Links
	HashSet<String> seeds = new HashSet<String>();
	HashSet<String> keywords = new HashSet<String>();
	// <meta description, link>
	Hashtable<String, String> crawledPages = new Hashtable<String, String>();
	public static void main(String[] args){
		System.out.println("program compiled, no immediate runtime errors");
	}
	Crawler(){
		//TODO fill seeds set
	}
	public Hashtable<String, String> start(String query, int depth){
		reset();
		getKeywords(query);
		for (String i : seeds){
			crawlPage(i, depth);
		}
		Set<String> keys = crawledPages.keySet();
		try{
			for (String i : keys){
				downloadPage(crawledPages.get(i), i);
			}
			return crawledPages;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	public void getKeywords(String query){
		// split query string into words
		// parse words into unique set
		String[] list = parseKeywords(query);
		for (int i = 0;i<list.length;++i){
			String temp = list[i];
			keywords.add(temp);
		}
	}
	public void crawlPage(String url, int depth){
		/* Recursive crawling algorithm for web links
		 * String url: url of current page to scan
		 * int depth: the depth of search to continue
		*/
		if (depth > 0){
			depth --;
			// retrieve sublinks
			Elements links = getLinks(url);
			// retrieve subimages	
			Elements images = getImages(url);
			// parse through all links
			for (Element i : links){
				String tempUrl = i.attr("href");
				tempUrl = processLink(tempUrl, url);
				String displayText = i.text();
				MetaData md = getMetaData(tempUrl);
				// scan for keyword matches
				String[] linkKeywords = parseKeywords(md.getKeywords());
				String[] linkDescription = parseKeywords(md.getDescription());
				String[] textKeywords = parseKeywords(displayText);
				if (containsKeywords(linkKeywords)||
				containsKeywords(linkDescription) ||
				containsKeywords(textKeywords)){
					crawlPage(tempUrl, depth);
				}
			}
			//TODO handle images
		}
		MetaData md = getMetaData(url);
		// add metadata description and url to final list of pages
		crawledPages.put(md.getDescription(), url);
	}
	private String[] parseKeywords(String list){
		list = list.toLowerCase();
		String reg = "(\\.|,|:|;|!|\\?|)?\\s((is|to|if|it|and|the|where|how|what|or|i|a)(((\\.|,|:|;|!|\\?|)?\\s)))?";
		String[] parsed = list.split(reg);
		return parsed;
	}
	private boolean containsKeywords(String[] list){
		// returns true if there is overlap between the given list and the set of keywords
		String[] a = keywords.toArray(new String[keywords.size()]);
		for (int i = 0;i<list.length;++i){
			for (int k = 0;k<a.length;++k){
				if (list[i].equals(a[k])){
					return true;
				}
			}
		}
		return false;
	}
	public void reset(){
		// reset query sepcific variables
		crawledPages.clear();
		keywords.clear();
	}
	private boolean downloadPage(String u, String fileName) throws IOException{
		/* Downloads the source code for a given string to an html file within the directory
		 * String u: the url for the page to retrieve sources code
		 * Returns boolean wheather or not the download was successful
		*/
		URL url = new URL(u);
		if (fileName.length() > 128){
			fileName = fileName.substring(0, 128);
		}
		try(
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName+".html"));
		){
			String insertLine = "<base href=\"";
			insertLine += u;
			insertLine += "\">";
			String line;
			boolean inserted = false;
			while ((line = reader.readLine())!=null){
				if (!inserted){
					if (line.contains("</head")&&line.contains(">")){
						inserted =true;
						writer.write(line);
						writer.write(insertLine);
					}
					else if (line.contains("<body")){
						inserted = true;
						writer.write("<head>"+insertLine+"</head>");
						writer.write(line);
					}
					else{
						writer.write(line);
					}
				}
				else{
					writer.write(line);
				}
			}
			reader.close();
			writer.close();
			return true;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	private Elements getLinks(String url){
		/* Access weblinks from within a given web page
		 * String url: the link to the page to pull sublinks from
		 * accesses Source reference link, and hyperlink text content
		*/
		try{
			Document doc = Jsoup.connect(url).get();
			Elements links = doc.select("a[href]");
			return links;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	private Elements getImages(String url){
		/* Access Image links from within a given page
		 * String url: the link to the page to pull images from
		 * Accesses source link, width, heiht, and description
		*/
		try{
			Document doc = Jsoup.connect(url).get();
			Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
			return images;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	private MetaData getMetaData(String url){
		/* Access the metadata of a web page
		 * String url: the resource link of the page to access
		 * Returns a meta data object with keywords and a description
		*/
		try{
			Document doc = Jsoup.connect(url).get();
			String keywords, description;
			keywords = doc.select("meta[name=keywords]").first().attr("content");
			description = doc.select("meta[name=description]").get(0).attr("content");
			MetaData obj = new MetaData(keywords, description);
			return obj;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	private String getHtml(String url){
		/* Returns the html of a web page
		 * String url: the link to access
		 * Returns a string with the html of the page
		*/
		String html = "";
		URL u;
		try{
			u = new URL(url);
			URLConnection conn = u.openConnection();
			conn.setRequestProperty("User-Agent", "Shell");
			conn.setRequestProperty("Accept-Charset", "UTF-8");
			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			while((line = reader.readLine())!=null){
				html += line;
				html += "\n";
			}
			html = html.trim();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return html;
	}
	private String stripFileName(String path){
		// Strips the given path by one directory to access its root path
		int index = path.lastIndexOf("/");
		return index <= -1 ? path : path.substring(0, index+1);
	}
	private String processLink(String link, String base){
		/* Processes url types into workable resource links
		 * String link: the link stub provided by the website
		 * String base: the full url of the current loaded page
		 * Returns the relative complete url of the subpage
		*/
		String processed = null;
		try {
			URL u = new URL(base);
			if (link.startsWith("./")){
				processed = link.substring(2, link.length());
				processed = u.getProtocol() + "://" + u.getAuthority() + stripFileName(u.getPath()) + processed;
			}
			else if (link.startsWith("#")){
				processed = base + link;
			}
			else if (link.startsWith("javascript:")){
				processed = null;
			}
			else if (link.startsWith("../")||(!link.startsWith("http://")||!link.startsWith("https://"))){
				processed = u.getProtocol() + "://" + u.getAuthority() + stripFileName(u.getPath()) + link;
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return processed;
	}

}
