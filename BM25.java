import indexer.*;
import indexer.Term;
import indexer.IndexedDoc;
import indexer.IndexReader;
import indexer.Stemmer;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.lang.Math;

public class BM25{
	int resultLimit;
	int documentCount;
	double avgDocLength;
	IndexReader indexCorpus;
	Stemmer stemmer;
	Vector<SimpleEntry<String, Double>> results;
	
	public static void main(String[] args){
		//TEST CODE
		BM25 frank = new BM25();
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

	public BM25(){
		resultLimit = 20;
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
					boolean found = false;
					for(int i = 0;!found&&i<results.size();++i){
						if (results.get(i).getValue()<value){
							results.insertElementAt(new SimpleEntry<String, Double>(set.getKey(),value), i);
							found = true;
						}
					}
					if (!found){
						results.add(new SimpleEntry<String, Double>(set.getKey(), value));
					}
				}
				else{
					results.add(new SimpleEntry<String, Double>(set.getKey(), value));
				}
				if (results.size()>resultLimit){
					results.remove(resultLimit);
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
			int documentFrequency = indexCorpus.getDocFreq(w);
			if (documentFrequency != 0){
				double p1 = Math.log((documentCount+0.5)/(documentFrequency+0.5));
				int termFrequency = d.termCount(w);
				double p2 = getP2(termFrequency, d);
				double p3 = getP3(termFrequency);
				score += (p1*p2*p3);
			}
		}
		return score;
	}

	public double getP2(int tf, IndexedDoc d){
		double k1 = 1.2;
		double b = 0.75;
		int docLength = d.getLength();
		double num = (tf+(k1*tf));
		double den = tf+(k1*((1-b)+(b*(docLength/avgDocLength))));
		return (num/den);
	}

	public double getP3(int tf){
		int k2 = 100;
		double num = (tf+(k2*tf));
		double den = (tf+k2);
		return (num/den);
	}
}
