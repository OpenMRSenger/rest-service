package OpenMRSenger.rest_service.application.adapters;

public class SecurePostAdapter implements MessageAdapter {
    @Override
    public boolean send() {
        return false;
    }
}
