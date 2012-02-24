package models;

import helpers.CloneableAppCheck;
import play.data.validation.*;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
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
    @Unique(message = "A template with that ID already exists")
    @MinSize(3)
    @MaxSize(64)
    @Match(value="[a-zA-Z0-9\\-]*$", message="The Application ID must only contain letters, numbers, and dashes")
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
    @CheckWith(CloneableAppCheck.class)
    public String herokuAppName;
    
    @Required
    @URL
    public String documentationUrl;

    public String instructionsUrl;

    @Required
    @Email
    public String submitterEmail;

    @ManyToMany
    public List<Tag> tags;

    // todo: json ignore
    @Required
    public String suggestedTags;

    @Min(1)
    @Max(5)
    public int rating;
    
    public Status status;
    
    public Date lastUpdated;

    public String toFullString() {
        return  "App ID: " + appId + "\n" +
                "Title: " + title + "\n" +
                "Description:\n" + description + "\n" +
                "Demo URL: " + demoUrl + "\n" +
                "Source URL: " + sourceUrl + "\n" +
                "Heroku App Name: " + herokuAppName + "\n" +
                "Documentation URL: " + documentationUrl + "\n" +
                "Instructions URL: " + instructionsUrl + "\n" +
                "Suggested Tags: " + suggestedTags + "\n";
    }


    public static enum Status {
        PENDING, PUBLISHED;
    }
    
    @Override
    public String toString() {
        return title;
    }
}
