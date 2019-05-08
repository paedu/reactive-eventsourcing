
package ch.sbb.ausbildung.eventsourcing.backend.in;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.file.javadsl.FileTailSource;
import akka.stream.javadsl.Sink;
import ch.sbb.ausbildung.eventsourcing.backend.vm.VerkehrsmittelActor;
import com.opencsv.CSVParser;
import io.vavr.collection.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Inbound Actor listening for new commands arriving in the <a href="">verkehrsmittel.csv</a> file.
 * Therefore it starts a <a href="https://doc.akka.io/docs/alpakka/current/file.html#tailing-a-file-into-a-stream">FileTailSource</a>
 * (Akka Alpakka, reactive streaming pipelines and connectors) which polls the given file for changes and emits
 * these changes as chunks of bytes (i.e. lines) as soon as they are written to the (end of) file.
 *
 * This actor simulates an inbound command source like e.g. a message queue or similar.
 * Because we don't wanna set up some complicated messaging middleware (MoM) in order to receive a stream of
 * commands, we just use this simplified filetailsource acting as a streaming source for inbound commands.
 *
 * <b>Important</b>: put the <code>resources/inbound/verkehrsmittel.csv</code> file outside of your workspace
 * so your IDE won't continuously scan and index the file for changes!
 * If you wanna add a new command while the app is running you can append your new command as a single line
 * at the end of the file (UTF-8).
 * Pay attention to the line feeds you're using: in a windows system use windows style line feeds (CR LF), in
 * a unix/linux system use unix style line feeds (LF); otherwise it won't work at all!
 *
 * @see <a href="https://doc.akka.io/docs/alpakka/current/file.html#file">FileTailSource</a>
 */
public class InboundActor extends AbstractLoggingActor {

    // constants
    private static final String CREATE_VERKEHRSMITTEL_COMMAND_ID = "CreateVerkehrsmittel";
    private static final String MOVE_VERKEHRSMITTEL_COMMAND_ID = "MoveVerkehrsmittel";

    // path to the "command file" (defined in "application.conf")
    private final String fileLocation = context().system().settings().config().getString("eventsourcing.command-file.path");
    private final Path filePath = FileSystems.getDefault().getPath(fileLocation);
    // materializer used to "materialize" and run the stream definition(s) aka. stages (source, sink)
    private final ActorMaterializer materializer = ActorMaterializer.create(context().system());
    private final ActorRef vmActor;
    private final CSVParser parser = new CSVParser();

    // props to create this actor
    public static Props props(ActorRef vmActor) {
        return Props.create(InboundActor.class, () -> new InboundActor(vmActor));
    }

    private InboundActor(ActorRef vmActor) {
        this.vmActor = vmActor;
    }

    @Override
    public void preStart() throws Exception {
        // start FileTailSource which continuously polls the file for new commands and emits them as reactive stream
        FileTailSource.createLines(filePath, 1024, Duration.ofSeconds(1))
                .log("Line", log())
                .map(this::toCommand)
                .log("Cmd:", log())
                .runWith(Sink.actorRef(vmActor, "complete"), materializer);

        super.preStart();
    }

    // no message handling here (just empty bahaviour)
    @Override
    public Receive createReceive() {
        return emptyBehavior();
    }

    // parses and converts the given string (command "line" from file) into the appropriate verkehrsmittel command (if possible)
    private VerkehrsmittelActor.Command toCommand(String line) throws IOException {
        final String[] values = parser.parseLine(line);

        if (ArrayUtils.isNotEmpty(values)) {
            switch (values[0]) {
                case CREATE_VERKEHRSMITTEL_COMMAND_ID:
                    if (ArrayUtils.getLength(values) == 5) {
                        return new VerkehrsmittelActor.Command.CreateVerkehrsmittel(Integer.parseInt(values[1]), values[2], values[3], List.of(StringUtils.split(values[4], ";")));
                    }
                    break;
                case MOVE_VERKEHRSMITTEL_COMMAND_ID:
                    if (ArrayUtils.getLength(values) == 3) {
                        return new VerkehrsmittelActor.Command.MoveVerkehrsmittel(Integer.parseInt(values[1]), values[2]);
                    }
                    break;
                default:
            }
        }
        // if there's no matching case return "noop" command (instead of null)
        return VerkehrsmittelActor.Command.NoOp.instance();
    }
}
