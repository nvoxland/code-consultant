package net.voxland.codeconsultant.appengine.search;

import net.voxland.codeconsultant.appengine.PersistenceManagerSingleton;
import net.voxland.codeconsultant.appengine.model.StackOverflowQuestion;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.query.QueryAutoStopWordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchService {
    private static SearchService instance;

    public static synchronized SearchService getInstance() {
        if (instance == null) {
            instance = new SearchService();
        }
        return instance;
    }

    public List<StackOverflowQuestion> search(String query) {
        PersistenceManager pm = PersistenceManagerSingleton.get().getPersistenceManager();
        return SearchJanitor.searchStackOverflowQuestionsDatabase(query, pm);
    }

    public StackOverflowQuestionHit findBest(String query) throws Exception {
        Log log = LogFactory.getLog(getClass());
        log.info("Original query: " + query);
        String databaseQuery = StringUtils.join(SearchJanitorUtils.getTokensForIndexingOrQuery(query, SearchJanitor.MAXIMUM_NUMBER_OF_WORDS_TO_SEARCH, CommonStopWords.STOP_WORDS).iterator(), " ");
        databaseQuery = databaseQuery.replaceAll("\\b\\w\\b", "");//remove single letter words
        databaseQuery = StringUtils.trimToNull(databaseQuery);
        if (databaseQuery == null) {
            return null;
        }
        log.info("Database query: " + databaseQuery);

        List<StackOverflowQuestion> matchingQuestions = search(databaseQuery);

        log.info("Matched " + matchingQuestions.size() + " records from the database");

        if (matchingQuestions.size() > 0) {
            RAMDirectory directory = new RAMDirectory();

            Analyzer analzer = new StandardAnalyzer(CommonStopWords.STOP_WORDS);
            IndexWriter indexWriter = new IndexWriter(directory, analzer, IndexWriter.MaxFieldLength.UNLIMITED);
            for (StackOverflowQuestion question : matchingQuestions) {
                Document doc = new Document();
                doc.add(new Field("title", question.getTitle(), Field.Store.YES, Field.Index.ANALYZED));
                doc.add(new Field("body", question.getBody(), Field.Store.NO, Field.Index.ANALYZED));
                doc.add(new Field("questionId", question.getQuestionId(), Field.Store.YES, Field.Index.NO));
                indexWriter.addDocument(doc);

            }
            indexWriter.commit();
            Map boost = new HashMap();
            boost.put("title", 10F);
            boost.put("body", 5F);
            IndexSearcher indexSearcher = new IndexSearcher(directory);
//            String luceneQuery = "\"" + query.replaceAll("[^\\w\\s]", "") + "\"~20";
            String luceneQuery = query;
            log.info("Lucene query: "+luceneQuery);
            TopDocs docs = indexSearcher.search(new MultiFieldQueryParser(new String[]{"title", "body"}, analzer, boost).parse(luceneQuery), 1);


            if (docs.totalHits == 0) {
                log.info("No hits to test");
            } else {
                Document hit = indexSearcher.doc(docs.scoreDocs[0].doc);
                log.info("Best Match: " + hit.get("title") + " with a score of " + docs.scoreDocs[0].score + " vs " + docs.getMaxScore() + " total hits: " + docs.totalHits);
                if (docs.scoreDocs[0].score > .8) {
                    return new StackOverflowQuestionHit(hit.get("questionId"), hit.get("title"), docs.scoreDocs[0].score);
                } else {
                    log.info("Not high enough");
                }
            }
        }

        return null;
    }

    public void reindex() {
        Log log = LogFactory.getLog(getClass());
        log.info("Reindexing start");
        int questionsPerIndex = 50;

        PersistenceManager pm = PersistenceManagerSingleton.get().getPersistenceManager();
        Query missingKeysQuery = pm.newQuery(StackOverflowQuestion.class, "searchKeys == null");
        missingKeysQuery.setRange(0, questionsPerIndex);
        List<StackOverflowQuestion> toIndexList = (List<StackOverflowQuestion>) missingKeysQuery.execute();
        log.info("Reindexing " + toIndexList.size() + " questions");
        for (StackOverflowQuestion question : toIndexList) {
            log.info("Reindexing "+question.getTitle());
            pm.currentTransaction().begin();
            question.computeSearchKeys();
            pm.currentTransaction().commit();
        }

        log.info("Reindexing done");
    }

    public int cleanIndex() {
        Log log = LogFactory.getLog(getClass());
        log.info("Cleaning start");

        PersistenceManager pm = PersistenceManagerSingleton.get().getPersistenceManager();

        Query missingKeysQuery = pm.newQuery(StackOverflowQuestion.class, "searchKeys!=null");
        missingKeysQuery.setRange(0,100);

        List<StackOverflowQuestion> toIndexList = (List<StackOverflowQuestion>) missingKeysQuery.execute();
        log.info("Removing keys from " + toIndexList.size() + " questions");
        for (StackOverflowQuestion question : toIndexList) {
            pm.currentTransaction().begin();
//            log.info("Removing search keys from "+question.getQuestionId());
            question.clearSearchKeys();
            pm.currentTransaction().commit();
        }

        log.info("Cleaning done");
        return toIndexList.size();
    }

    public static class StackOverflowQuestionHit {
        public String questionId;
        public String title;
        public float score;

        public StackOverflowQuestionHit(String questionId, String title, float score) {
            this.questionId = questionId;
            this.title = title;
            this.score = score;
        }

        @Override
        public String toString() {
            return score + ":" + questionId + ":" + title;
        }
    }

}
