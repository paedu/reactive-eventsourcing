
package ch.sbb.ausbildung.eventsourcing.backend.vm;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.Serializable;

/**
 * Entity and aggregate root "Verkehrsmittel" (abbr.: "vm"):
 * It represents a single means of transport (german: "Verkehrsmittel") i.e. mostly a train.
 * It will be identified by its <code>vmNummer</code> which should be unique in this example.
 * Besides the vmNummer it holds the list of <tt>fahrtpunkte</tt> representing the line segments (route, series of stops)
 * this verkehrsmittel will pass while moving on.
 * <tt>aktuellePosition</tt> and <tt>delay</tt> are non final and can be (re)set after the entity has been created
 * via event.
 *
 * (For ease of use the lombok plugin is used in order to save some boilerplate code,
 *  although i'm not a fan of such code generation ;-)
 */
@Value
@Builder
@EqualsAndHashCode(of = "vmNummer")
class Verkehrsmittel implements Serializable {
    private final int vmNummer;

    private final String vmArt;

    private final String bezeichnung;

    private final List<String> fahrtpunkte;
    @NonFinal
    private String aktuellePosition;

    @NonFinal
    private Integer delay;

    // null-safe getter
    Option<String> getAktuellePosition() {
        return Option.of(aktuellePosition).map(String::toUpperCase);
    }

    Verkehrsmittel setAktuellePosition(String aktuellePosition) {
        this.aktuellePosition = aktuellePosition;
        return this;
    }

    Verkehrsmittel setDelay(Integer delay) {
        this.delay = delay;
        return this;
    }
}
