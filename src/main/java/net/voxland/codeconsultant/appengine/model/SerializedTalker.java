package net.voxland.codeconsultant.appengine.model;

import com.google.appengine.api.datastore.Key;
import net.voxland.codeconsultant.ai.ElizaTalker;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;

@PersistenceCapable
public class SerializedTalker {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private String userId;

    @Persistent(serialized = "true")
    private ElizaTalker elizaTalker;
    private Date lastUsedDate;

    public Key getKey() {
        return key;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ElizaTalker getElizaTalker() {
        return elizaTalker;
    }

    public void setElizaTalker(ElizaTalker elizaTalker) {
        this.elizaTalker = elizaTalker;
    }

    public Date getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Date lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }
}
