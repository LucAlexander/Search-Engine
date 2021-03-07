package indexer;
import java.util.*;
import java.net.*;
import java.io.*;

public class IndexWriter{
	public static void addIndex(String u, String n){
		// downloads and indexes a page from a url
		// download
		try{
			URL url = new URL(u);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			BufferedWriter writer = new BufferedWriter(new FileWriter(n));
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
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
