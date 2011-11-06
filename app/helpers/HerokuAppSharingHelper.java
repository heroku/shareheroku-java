package helpers;

import com.heroku.api.HerokuStack;
import com.heroku.api.command.*;
import com.heroku.api.connection.HerokuBasicAuthConnectionProvider;
import com.heroku.api.connection.HerokuConnection;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import models.AppMetadata;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import play.jobs.Job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class HerokuAppSharingHelper extends Job<AppMetadata> {

    private static final String SSH_KEY_COMMENT = "share@heroku";
    
    String emailAddress;
    String gitUrl;
    
    public HerokuAppSharingHelper(String emailAddress, String gitUrl) {
        this.emailAddress = emailAddress;
        this.gitUrl = gitUrl;
    }

    @Override
    public AppMetadata doJobWithResult() throws Exception {
        AppMetadata appMetadata = new AppMetadata();

        try {
            HerokuConnection herokuConnection = new HerokuBasicAuthConnectionProvider(System.getenv("HEROKU_USERNAME"), System.getenv("HEROKU_PASSWORD")).getConnection();
            HerokuCommandConfig config = new HerokuCommandConfig().onStack(HerokuStack.Cedar);

            // create an app on heroku (using heroku credentials specified in ${HEROKU_USERNAME} / ${HEROKU_PASSWORD}
            HerokuCommand createCommand = new HerokuAppCreateCommand(config);
            HerokuCommandResponse createCommandResponse = createCommand.execute(herokuConnection);

            if (!createCommandResponse.isSuccess()) {
                throw new RuntimeException("Could not create the Heroku app");
            }

            // git@heroku.com:growing-ice-6779.git
            appMetadata.name = createCommandResponse.get("name").toString();
            appMetadata.gitUrl = "git@heroku.com:" + createCommandResponse.get("name") + ".git";
            appMetadata.httpUrl = "http://" + createCommandResponse.get("name") + ".herokuapp.com";

            // set the app name in the config
            config.app(appMetadata.name);

            // write the public key to a file
            String fakeUserHome = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString();

            File fakeUserHomeSshDir = new File(fakeUserHome + File.separator + ".ssh");
            fakeUserHomeSshDir.mkdirs();

            JSch jsch = new JSch();
            KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            keyPair.writePrivateKey(fakeUserHomeSshDir.getAbsolutePath() + File.separator + "id_rsa");
            keyPair.writePublicKey(fakeUserHomeSshDir.getAbsolutePath() + File.separator + "id_rsa.pub", SSH_KEY_COMMENT);

            ByteArrayOutputStream publicKeyOutputStream = new ByteArrayOutputStream();
            keyPair.writePublicKey(publicKeyOutputStream, SSH_KEY_COMMENT);
            publicKeyOutputStream.close();
            String sshPublicKey = new String(publicKeyOutputStream.toByteArray());

            // copy the known_hosts file to the .ssh dir
            File knownHostsFile = new File(getClass().getClassLoader().getResource("known_hosts").getFile());
            FileUtils.copyFileToDirectory(knownHostsFile, fakeUserHomeSshDir);

            // add the key pair to ${HEROKU_USERNAME}
            config.set(HerokuRequestKey.sshkey, sshPublicKey);
            HerokuCommand keysAddCommand = new HerokuKeysAddCommand(config);
            HerokuCommandResponse keysAddCommandResponse = keysAddCommand.execute(herokuConnection);

            if (!keysAddCommandResponse.isSuccess()) {
                throw new RuntimeException("Could not add an ssh key to the user");
            }

            // add the SOURCE_REPO env var
            // add GIT_URL env var
            //config.set(HerokuRequestKey.configvars, "{\"SOURCE_REPO\":\"" + System.getenv("SOURCE_REPO") + "\", \"GIT_URL\":\"" + appGitUrl + "\"}");

            //HerokuCommand configAddCommand = new HerokuConfigAddCommand(config);
            //HerokuCommandResponse configAddCommandResponse = configAddCommand.execute(herokuConnection);

            //if (!configAddCommandResponse.isSuccess()) {
            //    throw new RuntimeException("Could not configure the env vars");
            //}

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

            gitRepo.getRepository().getFS().setUserHome(new File(fakeUserHome));
            gitRepo.push().setRemote(appMetadata.gitUrl).call();

            // share the app with the provided email
            config.set(HerokuRequestKey.collaborator, emailAddress);
            HerokuCommand sharingAddCommand = new HerokuSharingAddCommand(config);
            HerokuCommandResponse sharingAddCommandResponse = sharingAddCommand.execute(herokuConnection);

            if (!sharingAddCommandResponse.isSuccess()) {
                throw new RuntimeException("Could not add " + emailAddress + " as a collaborator");
            }

            // transfer the app to the provided email
            config.set(HerokuRequestKey.transferOwner, emailAddress);
            HerokuCommand sharingTransferCommand = new HerokuSharingTransferCommand(config);
            HerokuCommandResponse sharingTransferCommandResponse = sharingTransferCommand.execute(herokuConnection);

            if (!sharingTransferCommandResponse.isSuccess()) {
                throw new RuntimeException("Could not transfer the app to " + emailAddress);
            }

            // remove ${HEROKU_USERNAME} as collaborator
            config.set(HerokuRequestKey.collaborator, System.getenv("HEROKU_USERNAME"));
            HerokuCommand sharingRemoveCommand = new HerokuSharingRemoveCommand(config);
            HerokuCommandResponse sharingRemoveCommandResponse = sharingRemoveCommand.execute(herokuConnection);

            if (!sharingRemoveCommandResponse.isSuccess()) {
                throw new RuntimeException("Could remove " + System.getenv("HEROKU_USERNAME") + " from the app");
            }

            // remove the key pair from ${HEROKU_USERNAME}
            config.set(HerokuRequestKey.name, SSH_KEY_COMMENT);
            HerokuCommand keysRemoveCommand = new HerokuKeysRemoveCommand(config);
            HerokuCommandResponse keysRemoveCommandResponse = keysRemoveCommand.execute(herokuConnection);

            if (!keysRemoveCommandResponse.isSuccess()) {
                throw new RuntimeException("Could not remove ssh key");
            }

            // cleanup the fakeUserHome
            new File(fakeUserHome).delete();

            return appMetadata;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        } catch (InvalidRemoteException e) {
            throw new RuntimeException(e);
        } catch (WrongRepositoryStateException e) {
            throw new RuntimeException(e);
        } catch (CanceledException e) {
            throw new RuntimeException(e);
        } catch (RefNotFoundException e) {
            throw new RuntimeException(e);
        } catch (DetachedHeadException e) {
            throw new RuntimeException(e);
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
    }
}