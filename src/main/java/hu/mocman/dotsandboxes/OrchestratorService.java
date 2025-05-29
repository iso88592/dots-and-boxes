package hu.mocman.dotsandboxes;

public interface OrchestratorService {
    String spawn(String sshKey);

    String getContainerIp(String id);

    void clearAllContainers();

    void clearContainer(String friendlyName);


}
