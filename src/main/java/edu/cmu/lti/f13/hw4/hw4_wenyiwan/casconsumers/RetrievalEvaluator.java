package edu.cmu.lti.f13.hw4.hw4_wenyiwan.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_wenyiwan.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_wenyiwan.typesystems.Token;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** global word dictionary **/
  private Set<String> globalWordDic;

  /** list of documents **/
  private List<Map<String, Integer>> docList;

  /** list of ranks of answers **/
  public ArrayList<Integer> rankList;

  /** list of similarities **/
  public ArrayList<Double> simList;

  /** positions of queries in documents **/
  public ArrayList<Integer> qPos;

  /** positions of answers in documents **/
  public HashMap<Integer, ArrayList<Integer>> aPos;

  public int prevQueryID = 0;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();
    relList = new ArrayList<Integer>();
    globalWordDic = new HashSet<String>();
    docList = new ArrayList<Map<String, Integer>>();
    rankList = new ArrayList<Integer>();
    simList = new ArrayList<Double>();
    qPos = new ArrayList<Integer>();
    aPos = new HashMap<Integer, ArrayList<Integer>>();

  }

  /**
   * Global word dictionary is contructed. Word frequency for each sentence is recorded.
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      rankList.add(0);
      simList.add(0.0);

      // Do something useful here
      Map<String, Integer> token2Freq = new HashMap<String, Integer>();
      for (Token token : FSCollectionFactory.create(fsTokenList, Token.class)) {
        globalWordDic.add(token.getText()); // Global word dictionary is contructed.
        token2Freq.put(token.getText(), token.getFrequency());
      }
      docList.add(token2Freq); // Word frequency for each sentence is recorded.

      if (doc.getQueryID() != prevQueryID) { // a new query
        prevQueryID = doc.getQueryID();
        // record the postions of queries in the document.
        qPos.add(docList.size() - 1);
      } else { // not a new query
        // record the postions of answers in the document.
        if (aPos.containsKey(doc.getQueryID())) {
          aPos.get(doc.getQueryID()).add(docList.size() - 1);
        } else
          aPos.put(doc.getQueryID(), new ArrayList<Integer>(Arrays.asList(docList.size() - 1)));
      }
    }
  }

  /**
   * Cosine Similarity computed and the retrieved sentences are ranked. The MRR metric is computed
   * and displayed here.
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    // TODO :: compute the cosine similarity measure
    for (int pos : qPos) {
      ArrayList<Integer> ansList = aPos.get(qIdList.get(pos));
      for (int aPos : ansList) {
        // record the similarity result
        simList.set(aPos, computeCosineSimilarity(docList.get(pos), docList.get(aPos)));
        // simList.set(aPos, computeDiceCoeff(docList.get(pos), docList.get(aPos)));
        // simList.set(aPos, computeJaccardCoeff(docList.get(pos), docList.get(aPos)));

      }
    }

    // TODO :: compute the rank of retrieved sentences
    for (int pos : qPos) {
      ArrayList<Integer> ansList = aPos.get(qIdList.get(pos));
      // use the treemap to store similarity
      TreeMap<Double, Integer> records = new TreeMap<Double, Integer>();
      for (int aPos : ansList) {
        records.put(simList.get(aPos), aPos);
      }
      ArrayList<Entry<Double, Integer>> keys = new ArrayList<Entry<Double, Integer>>(
              records.entrySet());
      int i = 0;
      while (i < keys.size()) {
        // System.out.println(String.format("%d,%d", keys.get(i).getValue(), keys.size() - i));
        rankList.set(keys.get(i).getValue(), keys.size() - i);
        i++;
      }
    }

    int prevQPos = Integer.MIN_VALUE;
    for (int i = 0; i < docList.size(); i++) {
      if (relList.get(i) == 99)
        prevQPos = i;
      if (relList.get(i) == 1) {
        System.out.println(String.format("Score: %.16f\trank=%d\trel=%d qid=%d sent=%d",
                simList.get(i), rankList.get(i), relList.get(i), qIdList.get(i), i - prevQPos));
      }
    }

    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr();
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  /**
   * This is the method to compute cosine_similarity.
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosSim = 0.0;

    // TODO :: compute cosine similarity between two sentences
    double sqrSumOne = 0.0;
    double sqrSumTwo = 0.0;
    for (String word : globalWordDic) {
      double freqOne = 0.0;
      double freqTwo = 0.0;
      if (queryVector.containsKey(word)) {
        freqOne = queryVector.get(word);
      }
      if (docVector.containsKey(word)) {
        freqTwo = docVector.get(word);
      }
      cosSim += freqOne * freqTwo;
      sqrSumOne += Math.pow(freqOne, 2);
      sqrSumTwo += Math.pow(freqTwo, 2);
    }
    if (cosSim != 0) {
      cosSim /= Math.pow((Math.sqrt(sqrSumOne) * Math.sqrt(sqrSumTwo)), 1.7);
      // 1.5-1.7 will be better.
//      cosSim = 1 - 2 * Math.acos(cosSim) / Math.PI;
    }
    return cosSim;
  }

  /**
   * This is the method to compute Dice Coeff.
   * 
   * @return DiceCoeff
   */
  private double computeDiceCoeff(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
    Set<String> listOne = new HashSet<String>();
    Set<String> common = new HashSet<String>();

    for (Entry<String, Integer> entry : queryVector.entrySet()) {
      listOne.add(entry.getKey());
    }
    for (Entry<String, Integer> entry : docVector.entrySet()) {
      if (!listOne.contains(entry.getKey())) {
        common.add(entry.getKey());
      }
    }
    return 2.0 * (common.size() + 1) / (queryVector.size() + docVector.size() + 2);
  }

  /**
   * This is the method to compute Jaccard Coeff
   * 
   * @return Jaccard Coeff
   */
  private double computeJaccardCoeff(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    Set<String> listOne = new HashSet<String>();
    Set<String> common = new HashSet<String>();

    for (Entry<String, Integer> entry : queryVector.entrySet()) {
      listOne.add(entry.getKey());
    }
    for (Entry<String, Integer> entry : docVector.entrySet()) {
      if (!listOne.contains(entry.getKey())) {
        common.add(entry.getKey());
      }
    }
    return (common.size() + 1) / (queryVector.size() + docVector.size() - common.size() + 2);
  }

  /**
   * This is the method to compute MRR.
   * 
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
    for (int i = 0; i < docList.size(); i++) {
      if (relList.get(i) == 1) {
        metric_mrr += 1.0 / rankList.get(i);
      }
    }
    metric_mrr /= qPos.size();
    return metric_mrr;
  }
}
