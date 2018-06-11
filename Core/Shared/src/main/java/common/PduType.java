package common;

/**
 * Enumeration zur Definition der Chat-PDU-Typen
 *
 * @author Matthias Strobl, P. Mandl
 */
public enum PduType {
    UNDEFINED(0, "Undefined"),
    LOGIN_REQUEST(1, "Login-Request"),
    LOGIN_RESPONSE(2, "Login-Response"),
    LOGOUT_REQUEST(3, "Logout-Request"),
    LOGOUT_RESPONSE(4, "Logout-Response"),
    CHAT_MESSAGE_REQUEST(5, "Chat-Message-Request"),
    CHAT_MESSAGE_RESPONSE(6, "Chat-Message-Response"),
    CHAT_MESSAGE_EVENT(7, "Chat-Message-Event"),
    LOGIN_EVENT(8, "Login-Event"),
    LOGOUT_EVENT(9, "Logout-Event");

    private final int id;
    private final String description;

    PduType(int id, String description) {
	this.id = id;
	this.description = description;
    }

    public static PduType getId(int id) {
	for (PduType e : values()) {
	    if (e.getId() == id) {
		return e;
	    }
	}
	return null;
    }

    public int getId() {
	return id;
    }

    public String getDescription() {
	return description;
    }

    @Override
    public String toString() {
	return description;
    }
}