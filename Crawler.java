import java.io.*;
import java.net.*;
import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import java.util.*;

public class Crawler{
	private boolean good;
	String seed;
	// page limiting
	int pageLimit;
	// <link, filename>
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
			Crawler charlotte = new Crawler("https://www.chessvariants.com/",1000);
			if (charlotte.isGood()){
				System.out.println("Charlotte is good");
				Hashtable<String, String> query = charlotte.start(2);
				Set<String> keys = query.keySet();
				for (String i : keys){
					System.out.printf("Webpage: %s : %s\n",query.get(i),i);
				}
				System.out.println(query.size());
			}
			else{
				System.out.println("Charlotte is not good");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	Crawler(String seedUrl, int PageLimit) throws IOException{
		// default parameters
		this.good = false;
		pageLimit = PageLimit;
		seed = seedUrl;
		try {
			if (isUrlValid(seed)){
				good = true;
			}
			else{
				good = false;
			}

		}
		catch(Exception e){
			good = false;
		}
	}
	public boolean isGood(){
		// returns if web crawler setup was a success
		return this.good;
	}
	public Hashtable<String, String> start(int depth){
		// crawl seed url
		crawlPage(seed, depth);
		return crawledPages;
	}
	public void crawlPage(String url, int depth){
		/* Recursive crawling algorithm for web links
		 * String url: url of current page to scan
		 * int depth: the depth of search to continue
		*/
		if (crawledPages.size() < pageLimit){
			if (!crawledPages.containsKey(url)){
				try{
					String name;
					name = downloadPage(url);
					//name = String.valueOf(crawledPages.size());
					crawledPages.put(url, name);
					if (depth > 0){
						depth --;
						// retrieve sublinks
						Elements links = getLinks(url);
						// parse through all links
						for (Element i : links){
							String tempUrl = i.absUrl("href");
							crawlPage(tempUrl, depth);
						}
					}
				}
				catch(Exception e){
					//Skip over non workable link
				}
			}
		}
	}
	private String downloadPage(String u) throws IOException{
		/* Downloads the source code for a given string to an html file within the directory
		 * String u: the url for the page to retrieve sources code
		 * Returns file name of downloaded file
		*/
		URL url = new URL(u);
		String n = "page"+String.valueOf(crawledPages.size());
		try(
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			BufferedWriter writer = new BufferedWriter(new FileWriter("./pages/"+n+".html"));
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
			return n+".html";
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
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
