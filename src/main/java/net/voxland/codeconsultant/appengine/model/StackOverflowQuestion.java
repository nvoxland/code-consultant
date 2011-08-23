package net.voxland.codeconsultant.appengine.model;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import net.voxland.codeconsultant.appengine.search.SearchJanitor;
import org.apache.commons.lang.StringUtils;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@PersistenceCapable
public class StackOverflowQuestion {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    private String questionId;

    private String title;

    private String body1;
    private String body2;
    private String body3;

    private Date createdDate;

    @Persistent
    private Set<String> searchKeys = new HashSet<String>();


    public Key getKey() {
        return key;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Set<String> getSearchKeys() {
        return searchKeys;
    }

    public String getBody() {
        return StringUtils.trimToEmpty(body1)+ StringUtils.trimToEmpty(body2)+StringUtils.trimToEmpty(body3);
    }

    public void setBody(String body) {
        body = body.replaceAll("</?[^>]+>", "");
        if (body.length() < 500) {
            body1 = body;
        } else {
            body1 = body.substring(0, 500);
            body = body.substring(500);

            if (body.length() < 500) {
                body2 = body;
            } else {
                body2 = body.substring(0, 500);
                body = body.substring(500);

                if (body.length() < 500) {
                    body3 = body;
                } else {
                    body3 = body.substring(0, 500);
                }
            }

        }

    }

    public void computeSearchKeys() {
        SearchJanitor.updateFTSStuffForStackOverflowQuestion(this);
    }

    public void clearSearchKeys() {
        this.searchKeys = null;
    }
}
