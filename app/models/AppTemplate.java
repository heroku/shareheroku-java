package models;

import play.data.validation.*;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.Date;
import java.util.List;

@Entity
public class AppTemplate extends Model {

    public AppTemplate() {
        rating = 1;
        lastUpdated = new Date();
        status = Status.PENDING;
    }
    
    @Required
    @Unique
    @MinSize(3)
    @MaxSize(64)
    @Match(value="^\\w*$", message="Not a valid app id")
    public String appId;
    
    @Required
    @MinSize(3)
    @MaxSize(64)
    public String title;

    @Required
    @MinSize(64)
    @MaxSize(65536)
    public String description;
    
    @Required
    @URL
    public String demoUrl;
    
    @Required
    @URL
    public String sourceUrl;

    @Required
    public String gitUrl;
    
    @Required
    @URL
    public String documentationUrl;

    @Required
    @URL
    public String instructionsUrl;

    @ManyToMany
    public List<Tag> tags;

    // todo: json ignore
    public String suggestedTags;

    @Min(1)
    @Max(5)
    public int rating;
    
    public Status status;
    
    public Date lastUpdated;


    public static enum Status {
        PENDING, PUBLISHED;
    }
    
    @Override
    public String toString() {
        return title;
    }
}
