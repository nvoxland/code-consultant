package net.voxland.codeconsultant.appengine.search;

import com.google.appengine.api.datastore.DatastoreNeedIndexException;
import com.google.appengine.api.datastore.DatastoreTimeoutException;
import net.voxland.codeconsultant.appengine.model.StackOverflowQuestion;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class SearchJanitor {

    private static final Logger log = Logger.getLogger(SearchJanitor.class.getName());

    public static final int MAXIMUM_NUMBER_OF_WORDS_TO_SEARCH = 15;

    public static final int MAX_NUMBER_OF_WORDS_TO_PUT_IN_INDEX = 500;

    public static List<StackOverflowQuestion> searchStackOverflowQuestionsDatabase(
            String queryString,
            PersistenceManager pm) {

        StringBuffer queryBuffer = new StringBuffer();

        queryBuffer.append("SELECT FROM " + StackOverflowQuestion.class.getName() + " WHERE ");

        Set<String> queryTokens = SearchJanitorUtils
                .getTokensForIndexingOrQuery(queryString,
                        MAXIMUM_NUMBER_OF_WORDS_TO_SEARCH,
                        DatabaseStopWords.STOP_WORDS);

        List<String> parametersForSearch = new ArrayList<String>(queryTokens);

        StringBuffer declareParametersBuffer = new StringBuffer();

        int parameterCounter = 0;

        while (parameterCounter < queryTokens.size()) {

            queryBuffer.append("searchKeys == param" + parameterCounter);
            declareParametersBuffer.append("String param" + parameterCounter);

            if (parameterCounter + 1 < queryTokens.size()) {
                queryBuffer.append(" && ");
                declareParametersBuffer.append(", ");

            }

            parameterCounter++;

        }


        Query query = pm.newQuery(queryBuffer.toString());

        query.declareParameters(declareParametersBuffer.toString());

        List<StackOverflowQuestion> result = null;

        try {
            result = (List<StackOverflowQuestion>) query.executeWithArray(parametersForSearch
                    .toArray());

        } catch (DatastoreTimeoutException e) {
            log.severe(e.getMessage());
            log.severe("datastore timeout at: " + queryString);// + " - timestamp: " + discreteTimestamp);
        } catch (DatastoreNeedIndexException e) {
            log.severe(e.getMessage());
            log.severe("datastore need index exception at: " + queryString);// + " - timestamp: " + discreteTimestamp);
        }

        return result;

    }


    public static void updateFTSStuffForStackOverflowQuestion(StackOverflowQuestion StackOverflowQuestion) {

        StringBuffer sb = new StringBuffer();

        sb.append(StackOverflowQuestion.getTitle())
                .append(" ")
                .append(StackOverflowQuestion.getBody());

        Set<String> new_ftsTokens = SearchJanitorUtils.getTokensForIndexingOrQuery(
                sb.toString(),
                MAX_NUMBER_OF_WORDS_TO_PUT_IN_INDEX,
                DatabaseStopWords.STOP_WORDS);


        Set<String> ftsTokens = StackOverflowQuestion.getSearchKeys();

        ftsTokens.clear();

        for (String token : new_ftsTokens) {
            ftsTokens.add(token);

        }
    }

}
