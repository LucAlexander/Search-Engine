import java.io.*;
import java.net.*;
import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.util.*;

public class Crawler{
	private boolean good;
	int fileCount;
	// Seed Links
	HashSet<String> seeds = new HashSet<String>();
	HashSet<String> keywords = new HashSet<String>();
	// <link, meta description>
	Hashtable<String, String> crawledPages = new Hashtable<String, String>();
	public static void main(String[] args){
		/*	 _______________
		 *	|		|
		 *	|		|
		 *	|   TESTCODE	|
		 *	|		|
		 *	|_______________|
		*/
		try{
			Crawler charlotte = new Crawler();
			if (charlotte.isGood()){
				System.out.println("Charlotte is good");
				Hashtable<String, String> query = charlotte.start("news about peaches", 1);
				Set<String> keys = query.keySet();
				for (String i : keys){
					System.out.printf("Webpage: %s\n",i);
				}
			}
			else{
				System.out.println("Charlotte is not good");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	Crawler() throws IOException{
		// default parameters
		this.good = false;
		fileCount = 0;
		// read in seed urls
		try(
			BufferedReader file = new BufferedReader(new FileReader("seeds.txt"));
		){
			String buffer;
			while((buffer=file.readLine())!=null){
				String newSeed = buffer.trim();
				if (!isUrlValid(newSeed)){
					String secure = "https://"+newSeed;
					if (!isUrlValid(secure)){
						newSeed = "http://"+newSeed;
					}
					else{
						newSeed = secure;
					}
				}
				if (isUrlValid(newSeed)){
					seeds.add(newSeed);
				}
			}
			this.good = true;
			file.close();
		}
		catch(Exception e){
			// setup failed
			this.good = false;
			e.printStackTrace();
		}
	}
	public boolean isGood(){
		// returns if web crawler setup was a success
		return this.good;
	}
	public Hashtable<String, String> start(String query, int depth){
		// reset query variables and retrieve keywords from curretn querry
		reset();
		getKeywords(query);
		// crawl seed urls
		for (String i : seeds){
			crawlPage(i, depth, getMetaData(i));
			System.out.print(".");
		}
		Set<String> keys = crawledPages.keySet();
		try{
			for (String i : keys){
				//downloadPage(crawledPages.get(i), i);
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
	public void crawlPage(String url, int depth, MetaData meta){
		/* Recursive crawling algorithm for web links
		 * String url: url of current page to scan
		 * int depth: the depth of search to continue
		*/
		try{
			if (depth > 0){
				depth --;
				// retrieve sublinks
				Elements links = getLinks(url);
				// parse through all links
				for (Element i : links){
					String tempUrl = i.absUrl("href");
					tempUrl = parseUrl(tempUrl, url);
					MetaData md = getMetaData(tempUrl);
					if (md != null){
						// scan for keyword matches
						if (compareKeywords(md, i)){
							crawlPage(tempUrl, depth, md);
						}
					}
				}
			}
		}
		catch(Exception e){
			//Skip over non workable link
		}
		// add metadata description and url to final list of pages
		crawledPages.put(url, meta.getDescription());
	}
	private boolean compareKeywords(MetaData md, Element link){
		// check for keyword matches in link data
		String content = md.getKeywords();
		content += md.getDescription();
		content += link.text();
		return (containsKeywords(content));
	}
	private String parseUrl(String u, String base){
		// returns whether a url string 'u' is relevant
		// returns null if url is same page as base 
		if (u.startsWith(base)){
			String work = u.substring(0, base.length());
			if (work.startsWith("#")||work.startsWith("javascript:")){
				return null;
			}
		}
		return u;
	}
	private String[] parseKeywords(String list){
		/* Use regex to serparate querry into significant keywords
		 * excludes "stop words"
		*/
		list = list.toLowerCase();
		String reg = "(\\.|,|:|;|!|\\?|)?\\s((is|to|if|it|and|the|where|how|what|or|i|a)(((\\.|,|:|;|!|\\?|)?\\s)))?";
		String[] parsed = list.split(reg);
		return parsed;
	}
	private boolean containsKeywords(String list){
		// returns true if there is overlap between the given list and the set of keywords
		String[] a = keywords.toArray(new String[keywords.size()]);
		for (int i = 0;i>a.length;++i){
			if (list.contains(a[i])){
				return true;
			}
		}
		return false;
	}
	public void reset(){
		// reset query sepcific variables
		crawledPages.clear();
		keywords.clear();
		fileCount = 0;
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
			BufferedWriter writer = new BufferedWriter(new FileWriter("page"+String.valueOf(fileCount)+".html"));
		){
			fileCount++;
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
			Document doc = getConnection(url);
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
			Document doc = getConnection(url);
			Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
			return images;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	private Document getConnection(String url){
		/* Complies with required tokens/cookies from website for valid connection
		 * String url: url to follow
		*/
		try{
			Connection.Response response = Jsoup.connect(url).method(Connection.Method.GET).timeout(50000).followRedirects(true).execute();
			Document document = Jsoup.connect(url).cookies(response.cookies()).get();
			return document;
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
			Document doc = getConnection(url);
			String keywords, description;
			Elements keywordList = doc.select("meta[name=keywords]");
			Elements descriptionList = doc.select("meta[name=description]");
			if (keywordList.isEmpty()){
				keywords = "";
			}
			else{
				keywords = doc.select("meta[name=keywords]").get(0).attr("content");
			}
			if (descriptionList.isEmpty()){
				description = "";
			}
			else{
				description = doc.select("meta[name=description]").get(0).attr("content");
			}
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
	private boolean isUrlValid(String url) {
		// attempts to make a connection to given url
		// returns whether or not it was a success
		try {
			URL obj = new URL(url);
			obj.toURI();
			return true;
		}
		catch (MalformedURLException e) {
			return false;
		}
		catch (URISyntaxException e) {
			return false;
		}
	}
	private String processLink(String link, String base){
		/* Processes url types into workable resource links
		 * String link: the link stub provided by the website
		 * String base: the full url of the current loaded page
		 * Returns the relative complete url of the subpage
		*/
		String processed = link;
		try{
			URL initial = new URL(link);
			if (isUrlValid(initial.toString())){
				return initial.toString();
			}
		}
		catch(Exception ex){
			try{
				URL u = new URL(base);
				if (link.startsWith("./")){
					processed = link.substring(2, link.length());
					processed = u.getProtocol() + "://" + u.getAuthority() + stripFileName(u.getPath()) + processed;
				}
				else if (link.startsWith("/")){
					processed = link.substring(1, link.length());
					processed = u.getProtocol() + "://" + u.getAuthority() + stripFileName(u.getPath()) + processed;
				}
				else if (link.startsWith("#")){
					processed = null;
				}
				else if (link.startsWith("javascript:")){
					processed = null;
				}
				else if (link.startsWith("../")||(!link.startsWith("http://")||!link.startsWith("https://"))){
					processed = u.getProtocol() + "://" + u.getAuthority() + stripFileName(u.getPath()) + link;
				}
				return processed;
			}
			catch(Exception e){
				return null;
			}
		}
		return processed;
	}
	private String encodeUrl(String string){
		/* encodes a String form of a url into appropriate url encoding scheme
		 * uses UTF-8
		*/
		try {
			String decodedURL = URLDecoder.decode(string, "UTF-8");
			URL url = new URL(decodedURL);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef()); 
			return uri.toURL().toString(); 
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
