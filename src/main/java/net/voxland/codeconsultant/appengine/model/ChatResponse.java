package net.voxland.codeconsultant.appengine.model;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;

@PersistenceCapable
public class ChatResponse {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    private Date date;

    private String userName;

    private String question;

    private String answer;

    private Float stackOverflowQuestionScore;
    private String stackOverflowQuestionId;
    private String stackOverflowQuestionTitle;

    public ChatResponse() {
        this.date = new Date();
    }

    public Key getKey() {
        return key;
    }    

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Float isStackOverflowAnswer() {
        return stackOverflowQuestionScore;
    }

    public void setStackOverflowQuestionScore(Float stackOverflowQuestionScore) {
        this.stackOverflowQuestionScore = stackOverflowQuestionScore;
    }

    public String getStackOverflowQuestionId() {
        return stackOverflowQuestionId;
    }

    public void setStackOverflowQuestionId(String stackOverflowQuestionId) {
        this.stackOverflowQuestionId = stackOverflowQuestionId;
    }

    public String getStackOverflowQuestionTitle() {
        return stackOverflowQuestionTitle;
    }

    public void setStackOverflowQuestionTitle(String stackOverflowQuestionTitle) {
        this.stackOverflowQuestionTitle = stackOverflowQuestionTitle;
    }
}
