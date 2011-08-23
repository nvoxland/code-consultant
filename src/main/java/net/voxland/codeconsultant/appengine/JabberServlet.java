package net.voxland.codeconsultant.appengine;

import com.google.appengine.api.xmpp.*;
import net.voxland.codeconsultant.ai.ElizaTalker;
import net.voxland.codeconsultant.appengine.model.ChatResponse;
import net.voxland.codeconsultant.appengine.model.SerializedTalker;
import net.voxland.codeconsultant.appengine.search.SearchService;
import org.apache.commons.logging.LogFactory;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class JabberServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        Message message = xmpp.parseMessage(req);

        PersistenceManager pm = PersistenceManagerSingleton.get().getPersistenceManager();

        JID fromJid = message.getFromJid();
        String body = message.getBody();

        try {
            Query serializedTalkerQuery = pm.newQuery(SerializedTalker.class, "userId == passedUserId");
            serializedTalkerQuery.setOrdering("lastUsedDate desc");
            serializedTalkerQuery.declareParameters("String passedUserId");
            List<SerializedTalker> talkers = (List<SerializedTalker>) serializedTalkerQuery.execute(fromJid.getId());

            boolean newTalker = false;
            ElizaTalker talker;
            if (talkers.size() == 0) {
                talker = new ElizaTalker();
                LogFactory.getLog(getClass()).debug("new talker");
                newTalker = true;
            } else {
                SerializedTalker foundTalker = talkers.iterator().next();
                talker = foundTalker.getElizaTalker();
                if (foundTalker.getLastUsedDate().before(new Date(new Date().getTime() - 1000*60*60*24))) {
                    LogFactory.getLog(getClass()).info("talker from " + foundTalker.getUserId() + " on " + foundTalker.getLastUsedDate()+" is too old");
                    talker = new ElizaTalker();
                    newTalker = true;
                } else {
                    LogFactory.getLog(getClass()).debug("talker from " + foundTalker.getUserId() + " on " + foundTalker.getLastUsedDate());
                }
            }

            if (newTalker) {
                {
                     Message msg = new MessageBuilder()
                             .withRecipientJids(fromJid)
                             .withBody("Hello, I'm your Code Consulatant")
                             .build();
                     xmpp.sendMessage(msg);

                    msg = new MessageBuilder()
                            .withRecipientJids(fromJid)
                            .withBody("Ever notice how just talking through an issue with a co-worker is enough to help you solve a problem, even if they don't say a word?")
                            .build();
                    xmpp.sendMessage(msg);

                    msg = new MessageBuilder()
                            .withRecipientJids(fromJid)
                            .withBody("Let me help you with your problems so they can keep working.")
                            .build();
                    xmpp.sendMessage(msg);

                 }

            }

            String talkerBody = talker.processInput(body);
            {
                Message msg = new MessageBuilder()
                        .withRecipientJids(fromJid)
                        .withBody(talkerBody)
                        .build();

                xmpp.sendMessage(msg);
            }

            SearchService.StackOverflowQuestionHit hit = null;
            if (body.contains(" ") && body.length() > 10) { //enough to get a good query
                //            System.out.println(searchQuery);
                hit = SearchService.getInstance().findBest(body);
                if (hit != null) {
                    String stackOverflowBody = "Does '" + hit.title + "' help?\n[http://stackoverflow.com/questions/" + hit.questionId + "]";

                    Message msg = new MessageBuilder()
                            .withRecipientJids(fromJid)
                            .withBody(stackOverflowBody)
                            .build();

                    xmpp.sendMessage(msg);
                }
            }


            try {
                ChatResponse chatResponse = new ChatResponse();
                chatResponse.setUserName(fromJid.getId());
                chatResponse.setQuestion(body);
                chatResponse.setAnswer(talkerBody);
                if (hit != null) {
                    chatResponse.setStackOverflowQuestionScore(hit.score);
                    chatResponse.setStackOverflowQuestionId(hit.questionId);
                    chatResponse.setStackOverflowQuestionId(hit.title);
                }

                pm.makePersistent(chatResponse);

                pm.deletePersistentAll(talkers);

                SerializedTalker serializedTalker = new SerializedTalker();
                serializedTalker.setUserId(fromJid.getId());
                serializedTalker.setLastUsedDate(new Date());
                serializedTalker.setElizaTalker(talker);

                pm.makePersistent(serializedTalker);
            } finally {
                pm.close();
            }
        } catch (Throwable e) {
            Message msg = new MessageBuilder()
                    .withRecipientJids(fromJid)
                    .withBody("Sorry.  Something seems very wrong with me at the moment.  Please ask again soon.")
                    .build();

            xmpp.sendMessage(msg);
            if (e instanceof ServletException) {
                throw ((ServletException) e);
            }
            if (e instanceof IOException) {
                throw ((IOException) e);
            }
            throw new ServletException(e);
        }
    }
}
