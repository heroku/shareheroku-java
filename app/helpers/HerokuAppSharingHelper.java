package helpers;

import com.heroku.api.App;
import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.SshSessionFactory;
import play.jobs.Job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HerokuAppSharingHelper extends Job<App> {

    private static final String SSH_KEY_COMMENT = "share@heroku";
    
    String emailAddress;
    String gitUrl;

    public Throwable exception;

    public HerokuAppSharingHelper(String emailAddress, String gitUrl) {
        this.emailAddress = emailAddress;
        this.gitUrl = gitUrl;
    }

    @Override
    public App doJobWithResult() throws IOException, JSchException, URISyntaxException, RefNotFoundException, WrongRepositoryStateException, DetachedHeadException, InvalidRemoteException, InvalidConfigurationException, CanceledException, NoHeadException {

        HerokuAPI herokuAPI = new HerokuAPI(System.getenv("HEROKU_API_KEY"));
        
        // create an app on heroku
        App app = herokuAPI.createApp(new App().on(Heroku.Stack.Cedar));

        if (!app.getCreateStatus().equals("complete")) {
            throw new RuntimeException("Could not create the Heroku app");
        }

        // create a temporary SSH key
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);

        ByteArrayOutputStream publicKeyOutputStream = new ByteArrayOutputStream();
        keyPair.writePublicKey(publicKeyOutputStream, SSH_KEY_COMMENT);
        publicKeyOutputStream.close();

        SshSessionFactory.setInstance(new HerokuSshSessionFactory(keyPair));
        
        String sshPublicKey = new String(publicKeyOutputStream.toByteArray());
        
        // add the key to ${HEROKU_USERNAME}
        herokuAPI.addKey(sshPublicKey);

        URI sourceRepoUri = new URI(gitUrl);

        // git clone a repo specified in sourceRepoUri to local disk
        File tmpDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "src" +
                File.separator + sourceRepoUri.getHost() + File.separator + sourceRepoUri.getPath());

        Git gitRepo = null;

        if (!tmpDir.exists()) {
            CloneCommand cloneCommand = new CloneCommand();
            cloneCommand.setURI(sourceRepoUri.toString());
            cloneCommand.setDirectory(tmpDir);
            gitRepo = cloneCommand.call();
        } else {
            File tmpGitDir = new File(tmpDir.getAbsolutePath() + File.separator + ".git");
            Repository repository = new RepositoryBuilder().setGitDir(tmpGitDir).build();
            gitRepo = new Git(repository);
            gitRepo.pull().call();
        }
        
        // git push the heroku repo
        gitRepo.push().setRemote(app.getGitUrl()).call();

        // share the app with the provided email
        herokuAPI.addCollaborator(app.getName(), emailAddress);

        // transfer the app to the provided email
        herokuAPI.transferApp(app.getName(), emailAddress);

        // remove ${HEROKU_USERNAME} as collaborator
        herokuAPI.removeCollaborator(app.getName(), System.getenv("HEROKU_USERNAME"));

        // remove the key pair from ${HEROKU_USERNAME}
        herokuAPI.removeKey(SSH_KEY_COMMENT);

        return app;
    }

    @Override
    public void onException(Throwable e) {
        this.exception = e;
    }
}