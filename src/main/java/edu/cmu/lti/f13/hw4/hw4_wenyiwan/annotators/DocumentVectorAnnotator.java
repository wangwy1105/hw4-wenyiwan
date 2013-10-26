package edu.cmu.lti.f13.hw4.hw4_wenyiwan.annotators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_wenyiwan.VectorSpaceRetrieval;
import edu.cmu.lti.f13.hw4.hw4_wenyiwan.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_wenyiwan.typesystems.Token;

/**
 * @author wwy This is the class for document annotation.
 */
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  HashSet<String> stopDic;

  /*
   * The method initialize reads stop words from file and makes the dict of stop words.
   */
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);

    stopDic = new HashSet<String>();
    URL docUrl = VectorSpaceRetrieval.class.getResource("/stopwords.txt");
    BufferedReader br;
    try {
      br = new BufferedReader(new InputStreamReader(docUrl.openStream()));
      String sLine;
      while ((sLine = br.readLine()) != null) {
        sLine.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
        stopDic.add(sLine);
      }
      br.close();
      br = null;
    } catch (IOException e) {
      throw new IllegalArgumentException("Error opening stopwords.txt");
    }

  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * The method createTermFreqVector uses straightforward technique to do tokenization,
   * namely, replacing non-alphabetical symbols, and split the string into tokens.
   * Here, stop words are removed using the dictionary and a token list is build for each doc.
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {
    String docText = doc.getText();
    // TO DO: construct a vector of tokens and update the tokenList in CAS
    String[] tokens = docText.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
    Map<String, Integer> tokenCounts = new HashMap<String, Integer>();
    for (String token : tokens) {
      // remove stopwords
      if (stopDic.contains(token.toLowerCase().trim()))
        continue;
      if (tokenCounts.containsKey(token))
        tokenCounts.put(token, tokenCounts.get(token) + 1);
      else
        tokenCounts.put(token, 1);
    }
    ArrayList<Token> tokenArr = new ArrayList<Token>();
    for (Entry<String, Integer> tokenCount : tokenCounts.entrySet()) {
      Token token = new Token(jcas);
      token.setFrequency(tokenCount.getValue());
      token.setText(tokenCount.getKey());
      token.addToIndexes();
      tokenArr.add(token);
    }
    doc.setTokenList(FSCollectionFactory.createFSList(jcas, tokenArr));
  }
}
