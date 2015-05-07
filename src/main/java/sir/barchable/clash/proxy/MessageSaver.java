package sir.barchable.clash.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sir.barchable.clash.protocol.*;
import sir.barchable.clash.protocol.Pdu.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static sir.barchable.clash.protocol.Pdu.Type.*;
import static sir.barchable.util.NoopCipher.NOOP_CIPHER;

/**
 * Save PDU message payloads to a directory.
 *
 * @author Sir Barchable
 *         Date: 25/04/15
 */
public class MessageSaver implements PduFilter {
    private static final Logger log = LoggerFactory.getLogger(MessageSaver.class);

    private MessageFactory messageFactory;
    private File saveDir;
    private Set<Type> types;

    /**
     * Construct a MessageSaver for village messages.
     *
     * @param saveDir where to save the messages
     */
    public MessageSaver(MessageFactory messageFactory, File saveDir) throws FileNotFoundException {
        this(messageFactory, saveDir, OwnHomeData, VisitedHomeData, EnemyHomeData, WarHomeData);
    }

    /**
     * Construct a MessageSaver for specified message types.
     *
     * @param saveDir where to save the messages to
     * @param types the IDs of the PDUs to save
     */
    public MessageSaver(MessageFactory messageFactory, File saveDir, Type... types) throws FileNotFoundException {
        if (!saveDir.exists()) {
            throw new FileNotFoundException(saveDir.getName());
        }
        this.messageFactory = messageFactory;
        this.saveDir = saveDir;
        this.types = new HashSet<>(Arrays.asList(types));
    }

    @Override
    public Pdu filter(Pdu pdu) throws IOException {
        Type type = Type.valueOf(pdu.getId());
        try {
            if (types.contains(type)) {
                Message message = messageFactory.fromPdu(pdu);
                String villageName;
                Map<String, Object> user = message.getStruct("user");
                if (user != null) {
                    villageName = sanitize((String) user.get("userName"));
                } else {
                    villageName = "anon";
                }
                String name = String.format("%s[%3$s]%2$tF-%2$tH-%2$tM-%2$tS.pdu", type, new Date(), villageName);
                File file = new File(saveDir, name);
                try (PduOutputStream out = new PduOutputStream(new FileOutputStream(file), NOOP_CIPHER)) {
                    out.write(pdu);
                }
            }
        } catch (PduException | IOException e) {
            log.error("Couldn't save village", e);
        }
        return pdu;
    }

    /**
     * Make a string file system friendly.
     */
    private String sanitize(String s) {
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            if (ch < ' ' || (ch == '.' && i == 0) || ":\\/]".indexOf(ch) != -1) {
                sb.append('_');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
