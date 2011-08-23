package net.voxland.codeconsultant.appengine.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class SearchJanitorUtils {


        private static final Logger log = Logger.getLogger(SearchJanitorUtils.class.getName());

//        /** From StopAnalyzer Lucene 2.9.1 */
//        public final static String[] stopWords = new String[]{
//                    "a", "an", "and", "are", "as", "at", "be", "but", "by",
//                    "for", "if", "in", "into", "is", "it",
//                    "no", "not", "of", "on", "or", "such",
//                    "that", "the", "their", "then", "there", "these",
//                    "they", "this", "to", "was", "will", "with",
//                  };
//
        /**
         * Uses english stemming (analyzer + lucene) + stopwords for getting the words.
         *
         * @return
         */
        public static Set<String> getTokensForIndexingOrQuery(
                        String index_raw,
                        int maximumNumberOfTokensToReturn,
                        String[] stopWords) {

                String indexCleanedOfHTMLTags = index_raw.replaceAll("\\<.*?>"," ");


                Set<String> returnSet = new HashSet<String>();

                try {

                        Analyzer analyzer =  new StandardAnalyzer(stopWords);


                        TokenStream tokenStream = analyzer.tokenStream(
                                        "content",
                                        new StringReader(indexCleanedOfHTMLTags));

                        Token token = new Token();

                while (((token = tokenStream.next()) != null)
                                && (returnSet.size() < maximumNumberOfTokensToReturn)) {

                                        returnSet.add(token.term().replaceAll("'",""));

                        }

                } catch (IOException e) {
                        log.severe(e.getMessage());
                }

                return returnSet;


        }
}
