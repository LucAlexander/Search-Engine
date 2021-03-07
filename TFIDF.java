import indexer.*;
import indexer.Term;
import indexer.IndexedDoc;
import indexer.IndexReader;
import indexer.Stemmer;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.lang.Math;

public class TFIDF{
	int documentCount;
	double avgDocLength;
	IndexReader indexCorpus;
	Stemmer stemmer;
	Vector<SimpleEntry<String, Double>> results;
	public static void main(String[] args){
		//TEST CODE
		TFIDF frank = new TFIDF();
		Term[] querry = {new Term(frank.stem("chess"),3), new Term(frank.stem("amazon"), 4),new Term(frank.stem("chaturanga"),0), new Term(frank.stem("rules"),1), new Term(frank.stem("pieces"),2)};
		frank.start(querry);
		frank.printResults();
		frank.close();
		System.out.println("Program terminated with exit code 0");
	}
	public String stem(String word){
		word = word.toLowerCase();
		for (int i = 0;i<word.length();++i){
			stemmer.add(word.charAt(i));
		}
		stemmer.stem();
		return stemmer.toString();
	}
	public TFIDF(){
		indexCorpus = new IndexReader();
		indexCorpus.start();
		documentCount = indexCorpus.getDocCount();
		avgDocLength = indexCorpus.getAvgDocLength();
		stemmer = new Stemmer();
	}

	public void start(Term[] q){
		results = new Vector<SimpleEntry<String, Double>>();
		HashMap<String, IndexedDoc> indexed = indexCorpus.getMap();
		for (Map.Entry<String, IndexedDoc> set : indexed.entrySet()){
			double value = getScore(set.getValue(), q);
			if (value != 0){
				if (results.size()!=0){
					for (int i = 0;i<results.size();++i){
						if (results.get(0).getValue()<value){
							results.insertElementAt(new SimpleEntry<String, Double>(set.getKey(),value), i);
						}
					}
				}
				else{
					results.add(new SimpleEntry<String, Double>(set.getKey(),value));
				}
			}
		}
	}

	public void close(){
		indexCorpus.close();
	}

	public void printResults(){
		System.out.println("Results: ");
		for (int i = 0;i<results.size();++i){
			System.out.printf("%s | %f\n", results.get(i).getKey(), results.get(i).getValue());
		}
	}

	public double getScore(IndexedDoc d, Term[] q){
		double score = 0.0;
		for (Term w : q){
			double otf = okapiTF(w, d);
			int documentFrequency = indexCorpus.getDocFreq(w);
			if (documentFrequency != 0){
				score += otf*(Math.log((documentCount/documentFrequency)));
			}
		}
		return score;
	}

	public double okapiTF(Term w, IndexedDoc d){
		int termFrequency = d.termCount(w);
		int docLength = d.getLength();
		double denom = termFrequency+0.5+(1.5*(docLength/avgDocLength));
		return termFrequency/denom;
	}
}
