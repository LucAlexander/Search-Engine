package indexer;
import java.util.*;

public class Term{
	int id;
	String stem;
	public Term(String s, int i){
		stem = s;
		id = i;
	}
	public int getId(){
		return id;
	}
	public String getStem(){
		return stem;
	}
}


