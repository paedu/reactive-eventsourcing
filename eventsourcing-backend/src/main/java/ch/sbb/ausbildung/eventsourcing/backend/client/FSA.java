
package ch.sbb.ausbildung.eventsourcing.backend.client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.reactivex.annotations.Nullable;
import io.vavr.jackson.datatype.VavrModule;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import scala.Immutable;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * Flux Standard Action:
 * Action type which is widely used in reactive frontend development, namely in combination with react.js and redux.
 * It models the data alongside a commonly used structure in order to simplify the exchange between back- and frontend.
 * See <a href="https://github.com/acdlite/flux-standard-action">Flux Standard Action </a>
 *
 * Herein all action types are defined, i.e. <code>VerkehrsmittelFSA</code> and <tt>UserFSA</tt>.
 * The JSON-annotations are used to inform the mapper about the type to use for de-/serializing the messages.
 */
@Value
@NonFinal
@AllArgsConstructor
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type", visible = true)
public class FSA<P, M> implements Immutable {

    // all possible event types from persisted state (sent to client-side)
    public interface Events {
        String VM_CREATED = "verkehrsmittel_created";
        String VM_MOVED = "verkehrsmittel_moved";
        String VM_ARRIVED = "verkehrsmittel_arrived";
        String VM_DELAYED = "verkehrsmittel_delayed";
    }

    // user commands sent from UI (to backend)
    public interface UserCommands {
        String LOAD_USERNAME = "load_username";
        String LOAD_VERKEHRSMITTEL = "load_verkehrsmittel";
        String DELAY_VERKEHRSMITTEL = "delay_verkehrsmittel";
    }

    // event types from backend, as a result to a user command
    public interface UserEvents {
        String USERNAME_LOADED = "username_loaded";
    }


    /**
     * The `type` of an action identifies to the consumer the nature of the action that has occurred.
     */
    @JsonTypeId
    String type;
    /**
     * The Optional `error` property MAY be set to true if the action represents an error.
     * By convention, the `payload` SHOULD be an error object.
     * If `error` has any other value besides `true`, including `undefined`, the action MUST NOT be interpreted as an error.
     */
    boolean error;
    /**
     * The Optional `payload` property MAY be any type of value.
     * It represents the payload of the action.
     * Any information about the action that is not the type or status of the action should be part of the `payload` field.
     * By convention, if `error` is `true`, the `payload` SHOULD be an error object.
     */
    @Nullable
    @JsonDeserialize
    @NonFinal
    P payload;
    /**
     * The Optional `meta` property MAY be any type of value.
     * It is intended for any extra information that is not part of the payload.
     */
    @Nullable
    @NonFinal
    M meta;


    private FSA(final String type, final P payload, final M meta) {
        this(type, false, payload, meta);
    }

    private FSA(final String type, final P payload) {
        this(type, false, payload, null);
    }

    public static <P> FSA action(final String typ, final P payload) {
        return new FSA<>(typ, payload);
    }

    public static <P, M> FSA action(final String typ, final P payload, final M meta) {
        return new FSA<>(typ, payload, meta);
    }

    public static <M> FSA error(final String typ, final Error error, final M meta) {
        return new FSA<>(typ, true, error, meta);
    }

    public static FSA error(final String typ, final Error error) {
        return new FSA<>(typ, true, error, null);
    }

    public static FSA error(final String typ, final String message, final String name, final String stack) {
        return new FSA<>(typ, true, Error.create(message, name, stack), null);
    }

    static FSA error(final String typ, final String message, final String name) {
        return new FSA<>(typ, true, Error.create(message, name), null);
    }

    static FSA error(final String typ, final String message) {
        return new FSA<>(typ, true, Error.create(message), null);
    }

    /**
     * (View) error used to send to frontend, as part of an FSA (if "error" = true)
     */
    @Value
    static class Error {

        @NonNull
        String message;

        @Nullable
        String name;

        @Nullable
        String stack;

        static Error create(final String message) {
            return new Error(message, null, null);
        }

        static Error create(final String message, final String name) {
            return new Error(message, name, null);
        }

        static Error create(final String message, final String name, final String stack) {
            return new Error(message, name, stack);
        }
    }


    @JsonTypeInfo(
            use = NAME,
            include = PROPERTY,
            property = "type", visible = true)
    @JsonSubTypes({
            // Frontend actions
            @JsonSubTypes.Type(value = VerkehrsmittelFSA.class, name = Events.VM_CREATED),
            @JsonSubTypes.Type(value = VerkehrsmittelFSA.class, name = Events.VM_MOVED),
            @JsonSubTypes.Type(value = VerkehrsmittelFSA.class, name = Events.VM_ARRIVED),
            @JsonSubTypes.Type(value = VerkehrsmittelFSA.class, name = Events.VM_DELAYED),
            @JsonSubTypes.Type(value = VerkehrsmittelFSA.class, name = UserCommands.DELAY_VERKEHRSMITTEL),
            // Frontend actions
            @JsonSubTypes.Type(value = UserFSA.class, name = UserCommands.LOAD_VERKEHRSMITTEL),
            @JsonSubTypes.Type(value = UserFSA.class, name = UserCommands.LOAD_USERNAME),
            @JsonSubTypes.Type(value = UserFSA.class, name = UserEvents.USERNAME_LOADED)
    })
    public static class VerkehrsmittelFSA<T> extends FSA<T, Integer> {

        VerkehrsmittelFSA(String type, boolean error, T payload, Integer meta) {
            super(type, error, payload, meta);
        }

        VerkehrsmittelFSA(String type, T payload) {
            this(type, false, payload, null);
        }

        public VerkehrsmittelFSA() {
            this(null, null);
        }

        public static <T> VerkehrsmittelFSA action(final String typ, final T payload) {
            return new VerkehrsmittelFSA(typ, false, payload, null);
        }

        static <T> VerkehrsmittelFSA action(final String typ, final T payload, final Integer meta) {
            return new VerkehrsmittelFSA(typ, false, payload, meta);
        }

        public static VerkehrsmittelFSA error(final String typ, final Error error) {
            return new VerkehrsmittelFSA(typ, true, error, null);
        }

        static VerkehrsmittelFSA error(final String typ, final String message, final String name) {
            return new VerkehrsmittelFSA(typ, true, message, null);
        }
    }

    @JsonTypeInfo(
            use = NAME,
            include = PROPERTY,
            property = "type", visible = true)
    @JsonSubTypes({
    })
    // user commands or events
    public static class UserFSA extends VerkehrsmittelFSA<String> {

        UserFSA(String type, boolean error, String payload, Integer meta) {
            super(type, error, payload, meta);
        }

        UserFSA(String type, String payload) {
            this(type, false, payload, null);
        }

        public UserFSA() {
            this(null, null);
        }

        static UserFSA action(final String typ, final String payload) {
            return new UserFSA(typ, false, payload, null);
        }
    }


    // JSON utilities
    private static ObjectMapper defaultMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new JavaTimeModule())
            // serialize dates in ISO-8601 format ("e.g. 2017-05-19T16:54:16.29+02:00")
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRAP_EXCEPTIONS)
            .registerModule(new VavrModule());

    static <T> String writeValueAsString(final T o) {
        try {
            return defaultMapper.writeValueAsString(o);
        } catch (final Exception e) {
            throw new SerialisationException(e);
        }
    }

    static <T> T readValueFor(final String content, final Class<T> valueType) {
        try {
            return defaultMapper.readerFor(valueType).readValue(content);
        } catch (final Exception e) {
            throw new SerialisationException(e);
        }
    }

    private static class SerialisationException extends RuntimeException {
        SerialisationException(Throwable cause) {
            super(cause);
        }
    }
}

