package OpenMRSenger.rest_service.application.adapters;

public class AsyncFlowAdapter implements MessageAdapter {
    @Override
    public boolean send() {
        return false;
    }
}
