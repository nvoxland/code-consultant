package net.voxland.codeconsultant.appengine;

import com.google.appengine.api.datastore.*;
import net.voxland.codeconsultant.appengine.model.StackOverflowQuestion;
import net.voxland.codeconsultant.appengine.search.SearchService;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class IndexerServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(IndexerServlet.class.getName());

    @Override
    public void init() throws ServletException {

    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String query = req.getParameter("q");

        List<StackOverflowQuestion> hits = SearchService.getInstance().search(query);

        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();
        writer.println("size: " + hits.size());
        for (StackOverflowQuestion question : hits) {
            writer.println(((StackOverflowQuestion) question).getTitle());
        }

        try {
            writer.println("Best match: " + SearchService.getInstance().findBest(query));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
