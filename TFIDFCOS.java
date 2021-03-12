import indexer.*;
import indexer.Term;
import indexer.IndexedDoc;
import indexer.IndexReader;
import indexer.Stemmer;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.lang.Math;

public class TFIDFCOS{
	int resultLimit;
	int documentCount;
	double avgDocLength;
	IndexReader indexCorpus;
	Stemmer stemmer;
	ArrayList<SimpleEntry<String, Double>> cosineSim;
	ArrayList<SimpleEntry<String, ArrayList<Double>>> results;
	public static void main(String[] args){
		//TEST CODE
		TFIDFCOS frank = new TFIDFCOS();
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
	public TFIDFCOS(){
		resultLimit = 20;
		indexCorpus = new IndexReader();
		indexCorpus.start();
		documentCount = indexCorpus.getDocCount();
		avgDocLength = indexCorpus.getAvgDocLength();
		stemmer = new Stemmer();
	}

	public void start(Term[] q){
		cosineSim = new ArrayList<SimpleEntry<String, Double>>();
		results = new ArrayList<SimpleEntry<String, ArrayList<Double>>>();
		HashMap<String, IndexedDoc> indexed = indexCorpus.getMap();
		// generate complete vocab vector
		ArrayList<String> vocab = new ArrayList<String>();
		for (Map.Entry<String, IndexedDoc> set : indexed.entrySet()){
			Term[] arr = set.getValue().getTerms();
			for (Term t : arr){
				if (!vocab.contains(t.getStem())){
					vocab.add(t.getStem());
				}
			}
		}
		// Create Vectors
		ArrayList<Double> qVector = getVec(vocab, q);
		for (Map.Entry<String, IndexedDoc> set : indexed.entrySet()){
			results.add(0, new SimpleEntry<String, ArrayList<Double>>(set.getKey(), getVec(vocab, set.getValue().getTerms())));
			double cs = getCosSim(results.get(0).getValue(), qVector);
			if (cs != 0){
				if (cosineSim.size()!=0){
					boolean found = false;
					for (int i = 0;!found&&i<cosineSim.size();++i){
						if (cosineSim.get(i).getValue()<cs){
							cosineSim.add(i, new SimpleEntry<String, Double>(set.getKey(),cs));
							found = true;
						}
						
					}
					if (!found){
						cosineSim.add(new SimpleEntry<String, Double>(set.getKey(),cs));
					}
				}
				else{
					cosineSim.add(new SimpleEntry<String, Double>(set.getKey(), cs));
				}
				if (cosineSim.size()>resultLimit){
					cosineSim.remove(resultLimit);
				}
			}
		}
	}

	public double getCosSim(ArrayList<Double> d, ArrayList<Double> q){
		double numerator = 0;
		double denTermD = 0;
		double denTermQ = 0;
		for (int i = 0;i<d.size();++i){
			double dTerm = d.get(i);
			double qTerm = q.get(i);
			numerator += dTerm*qTerm;
			denTermD += dTerm*dTerm;
			denTermQ += qTerm*qTerm;
		}
		double denominator = Math.sqrt(denTermD*denTermQ);
		return (numerator==0||denominator==0)?0:numerator/denominator;
	}

	public ArrayList<Double> getVec(ArrayList<String> v, Term[] list){
		ArrayList<Double> vec = new ArrayList<Double>();
		for (int i= 0;i<v.size();++i){
			vec.add(0.0);
		}
		for (Term t : list){
			int index = v.indexOf(t.getStem());
			if (index != -1){
				vec.set(index, getScore(t, v));
			}
		}
		return vec;
	}

	public void close(){
		indexCorpus.close();
	}

	public void printResults(){
		System.out.println("Results: ");
		for (int i = 0;i<cosineSim.size();++i){
			System.out.printf("%s | %f\n", cosineSim.get(i).getKey(), cosineSim.get(i).getValue());
		}
	}

	public double getScore(Term w, ArrayList<String> v){
		double otf = 1.5+(1.5*(v.size()/avgDocLength));
		int documentFrequency = indexCorpus.getDocFreq(w);
		if (documentFrequency != 0){
			return otf*(Math.log((documentCount/documentFrequency)));
		}
		return 0;
	}
}
