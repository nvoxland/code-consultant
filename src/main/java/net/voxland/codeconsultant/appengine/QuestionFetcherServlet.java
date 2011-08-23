package net.voxland.codeconsultant.appengine;

import com.google.appengine.api.datastore.Text;
import net.voxland.codeconsultant.appengine.model.MaxStackOverflowQuestion;
import net.voxland.codeconsultant.appengine.model.StackOverflowQuestion;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class QuestionFetcherServlet extends HttpServlet {
    private boolean gzipped;

    @Override
    public void init() throws ServletException {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PersistenceManager pm = PersistenceManagerSingleton.get().getPersistenceManager();

        if (req.getParameter("read-xml") != null) {
            try {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                PostsHandler postsHandler = new PostsHandler(req, pm);
                parser.parse(new GZIPInputStream(getClass().getClassLoader().getResourceAsStream("posts.extracted"+req.getParameter("read-xml")+".xml.gz")), postsHandler);

//                markLastFetchedQuestion(pm, postsHandler.lastStackOverflowQuestion);

                resp.getWriter().print("<html><head>");
                if (postsHandler.stackOverflowQuestions.size() > 0) {
                    resp.getWriter().print("<meta http-equiv=\"refresh\" content=\"1\">");
                }
                resp.getWriter().print("</head><body>Fetched "+postsHandler.stackOverflowQuestions.size()+"</body></html>");                
                return;
            } catch (Exception e) {
                throw new ServletException(e);
            } finally {
                pm.close();
            }
        }

        Date lastCreated;
        {
//        try {
            Query query = pm.newQuery(MaxStackOverflowQuestion.class);
            List list = (List) query.execute();
            if (list.size() == 0) {
                lastCreated = new Date(0);
            } else {
                lastCreated = ((MaxStackOverflowQuestion) list.iterator().next()).getCreatedDate();
            }
        }


        LogFactory.getLog(getClass()).info("Fetching Questions since " + lastCreated);
        URL url = new URL("http://api.stackoverflow.com/1.0/questions?key=V2f1_4yrckGfUSa8vHuQhg&body=true&pagesize=100&sort=creation&order=asc&fromdate=" + (lastCreated.getTime() / 1000));
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Content-Encoding", "gzip");
        JsonNode rootNode = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream stream = urlConnection.getInputStream(); //new GZIPInputStream
            if (gzipped) {
                stream = new GZIPInputStream(stream);
            }
            rootNode = mapper.readValue(stream, JsonNode.class);
        } catch (IOException e) {
            if (e.getMessage().contains("Illegal character")) {
                gzipped = true;
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
                StringBuffer content = new StringBuffer();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
                LogFactory.getLog(getClass()).fatal("Not gzip content: " + content, e);
            }
            throw e;
        }


        Iterator<JsonNode> questions;
        try {
            questions = rootNode.get("questions").iterator();
            StackOverflowQuestion stackOverflowQuestion = null;
            List<StackOverflowQuestion> stackOverflowQuestions = new ArrayList<StackOverflowQuestion>();

            while (questions.hasNext()) {
                JsonNode question = questions.next();

                String body = question.get("body").getValueAsText();

                stackOverflowQuestion = new StackOverflowQuestion();
                stackOverflowQuestion.setTitle(question.get("title").getValueAsText());
                stackOverflowQuestion.setQuestionId(question.get("question_id").getValueAsText());
                stackOverflowQuestion.setCreatedDate(new Date(question.get("creation_date").getLongValue() * 1000));
                stackOverflowQuestion.setBody(body);
                stackOverflowQuestion.computeSearchKeys();

                stackOverflowQuestions.add(stackOverflowQuestion);
            }

            pm.makePersistentAll(stackOverflowQuestions);

            if (stackOverflowQuestion != null) {
                markLastFetchedQuestion(pm, stackOverflowQuestion);
            }

            resp.getWriter().print("<html><head><meta http-equiv=\"refresh\" content=\"1\"><body>Fetched "+stackOverflowQuestions.size()+" since "+lastCreated+"</body></html>");
        } finally {
            pm.close();
        }

    }

    private void markLastFetchedQuestion(PersistenceManager pm, StackOverflowQuestion stackOverflowQuestion) {
        if (stackOverflowQuestion == null) {
            return;
        }

        MaxStackOverflowQuestion lastCreatedQuestion = null;

        Query query = pm.newQuery(MaxStackOverflowQuestion.class);
        List list = (List) query.execute();
        if (list.size() == 0) {
            lastCreatedQuestion = new MaxStackOverflowQuestion();
        } else {
            lastCreatedQuestion = (MaxStackOverflowQuestion) list.iterator().next();
        }

        lastCreatedQuestion.setCreatedDate(stackOverflowQuestion.getCreatedDate());
        lastCreatedQuestion.setQuestionId(stackOverflowQuestion.getQuestionId());
        lastCreatedQuestion.setFetchDate(new Date());
        if (lastCreatedQuestion.getKey() == null) {
            pm.makePersistent(lastCreatedQuestion);
        }
    }


    private static class PostsHandler extends DefaultHandler {
        private List<StackOverflowQuestion> stackOverflowQuestions = new ArrayList<StackOverflowQuestion>();
        private SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        public StackOverflowQuestion lastStackOverflowQuestion = null;

        private PersistenceManager pm;
        private String xmlFile;
        private HttpSession session;
        private Integer startLine;
        private int currentLine = 0;

        private int maxToAdd = 50;

        private PostsHandler(HttpServletRequest req, PersistenceManager pm) {
            this.pm = PersistenceManagerSingleton.get().getPersistenceManager();
            this.xmlFile = req.getParameter("read-xml");
            this.session = req.getSession();

            this.startLine = (Integer) this.session.getAttribute("lastLine="+xmlFile);
            if (startLine == null) {
                startLine = 0;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            try {
                if (currentLine >= startLine && stackOverflowQuestions.size() < maxToAdd && qName.equals("row") && attributes.getValue("PostTypeId").equals("1")) {

                    StackOverflowQuestion stackOverflowQuestion = new StackOverflowQuestion();
                    stackOverflowQuestion.setTitle(attributes.getValue("Title"));
                    stackOverflowQuestion.setQuestionId(attributes.getValue("Id"));
                    stackOverflowQuestion.setCreatedDate(dateParser.parse(attributes.getValue("CreationDate")));
                    stackOverflowQuestion.setBody(attributes.getValue("Body"));
                    stackOverflowQuestion.computeSearchKeys();

                    lastStackOverflowQuestion = stackOverflowQuestion;
                    stackOverflowQuestions.add(stackOverflowQuestion);

                } else {
                    System.out.println("not");
                }

            } catch (ParseException e) {
                throw new SAXException(e);
            }

            if (qName.equals("row")) {
                currentLine++;
            }

        }

        @Override
        public void endDocument() throws SAXException {
            pm.makePersistentAll(stackOverflowQuestions);
            pm.close();
            System.out.println("Finished reading document");
            session.setAttribute("lastLine="+xmlFile, startLine+stackOverflowQuestions.size());
        }
    }

}
