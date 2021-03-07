package indexer;
import java.util.*;

public class IndexedDoc{
	// term, term count
	HashMap<Term, Integer> contents;
	public IndexedDoc(HashMap<Term, Integer> c){
		contents = c;
	}
	public HashMap<Term, Integer> getContents(){
		return contents;
	}
	public int getTotalTerms(){
		return contents.size();
	}
	public int getLength(){
		int total = 0;
		for (Map.Entry<Term, Integer> set : contents.entrySet()){
			total += set.getValue();
		}
		return total;
	}
	public int termCount(Term t){
		// number of times a term t occurs in the document
		int freq = 0;
		for (Map.Entry<Term, Integer> set : contents.entrySet()){
			if (set.getKey().getStem().equals(t.getStem())){
				freq++;
			}
		}
		return freq;
	}
	public boolean containsTerm(Term t){
		// if the document contains term t
		for (Map.Entry<Term, Integer> set : contents.entrySet()){
			if (set.getKey().getStem().equals(t.getStem())){
				return true;
			}
		}
		return false;
	}
	public Term[] getTerms(){
		// retrieves all terms
		Term[] l = new Term[contents.size()];
		int index = 0;
		for (Map.Entry<Term, Integer> set : contents.entrySet()){
			l[index] = set.getKey();
			index++;
		}
		return l;
	}
}


