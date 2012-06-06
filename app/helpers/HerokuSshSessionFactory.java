package helpers;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

public class HerokuSshSessionFactory extends SshSessionFactory {

    final private KeyPair keyPair;
    
    public HerokuSshSessionFactory(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @Override
    public RemoteSession getSession(URIish uri, CredentialsProvider credentialsProvider, FS fs, int tms) throws TransportException {
        String user = uri.getUser();
        String host = uri.getHost();

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(getClass().getClassLoader().getResourceAsStream("known_hosts"));

            ByteArrayOutputStream privateKeyOutputStream = new ByteArrayOutputStream();
            keyPair.writePrivateKey(privateKeyOutputStream);
            privateKeyOutputStream.close();
            
            jsch.addIdentity("keypair", privateKeyOutputStream.toByteArray(), keyPair.getPublicKeyBlob(), null);
            
            final Session session = jsch.getSession(user, host);
            
            if (!session.isConnected())
                session.connect(tms);

            return new JschSession(session, uri);
        } catch (JSchException je) {
            final Throwable c = je.getCause();
            if (c instanceof UnknownHostException)
                throw new TransportException(uri, JGitText.get().unknownHost);
            if (c instanceof ConnectException)
                throw new TransportException(uri, c.getMessage());
            throw new TransportException(uri, je.getMessage(), je);
        } catch (IOException e) {
            throw new TransportException(uri, e.getMessage(), e);
        }

    }

}