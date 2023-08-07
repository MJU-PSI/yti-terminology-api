package fi.vm.yti.terminology.api.index;

import fi.vm.yti.terminology.api.model.termed.Identifier;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

public class TermedNotification {

    public static class Body {
        @NotNull private String user;
        @NotNull private Date date;
        @NotNull private List<Identifier> nodes;
        private boolean sync;

        public String getUser() {
            return user;
        }
        public void setUser(String user) {
            this.user = user;
        }
        public Date getDate() {
            return date;
        }
        public void setDate(Date date) {
            this.date = date;
        }
        public List<Identifier> getNodes() {
            return nodes;
        }
        public void setNodes(List<Identifier> nodes) {
            this.nodes = nodes;
        }
        public boolean isSync() {
            return sync;
        }
        public void setSync(boolean sync) {
            this.sync = sync;
        }
    }

    public enum EventType {
        NodeSavedEvent,
        NodeDeletedEvent,
        ApplicationReadyEvent,
        ApplicationShutdownEvent
    }

    @NotNull private EventType type;
    @NotNull private Body body;

    public EventType getType() {
        return type;
    }
    public void setType(EventType type) {
        this.type = type;
    }
    public Body getBody() {
        return body;
    }
    public void setBody(Body body) {
        this.body = body;
    }
}
