package mqtt;

public class Common {
    private String idMessage;
    private String messageType;
    private String user;

    public String getMessageType() {
        return messageType;
    }

    public String getIdMessage() {
        return idMessage;
    }

    public String getUser(){return user;}
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setIdMessage(String idMessage) {
        this.idMessage = idMessage;
    }

    public void setUser(String user){this.user=user;}
}
