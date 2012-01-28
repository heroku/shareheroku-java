package models;

import play.data.validation.*;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.List;

@Entity
public class AppTemplate extends Model {

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
    @URL
    public String gitUrl;
    
    @Required
    @URL
    public String documentationUrl;

    @Required
    @URL
    public String instructionsUrl;

    @ManyToMany
    public List<Tag> tags;

    public Status status;


    public static enum Status {
        PENDING, PUBLISHED;
    }
}
