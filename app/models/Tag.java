package models;

import play.data.validation.Match;
import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import java.util.List;

@Entity
public class Tag extends Model {
    
    @Required
    @MinSize(value = 3)
    @MaxSize(value = 64)
    @Match(value="^\\w*$", message="Not a valid tag id")
    public String tagId;
    
    @Required
    @MinSize(value = 3)
    @MaxSize(value = 64)
    public String name;

    
    @Override
    public String toString() {
        return name;
    }
}
