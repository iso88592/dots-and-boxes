package hu.mocman.dotsandboxes;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Optional;
import java.net.NetworkInterface;
import java.net.InetAddress;

@Slf4j
@Service
public class OrchestratorServiceImpl implements OrchestratorService {

    private final String bridgedNetworkName = "dots_and_boxes_bridge";
    private final String dockerImageName = "game-dab";
    private final DockerClient dockerClient;
    private String applicationHostAddress = "";

    public OrchestratorServiceImpl() {
        
        log.info("Starting OrchestratorServiceImpl");
        NettyDockerCmdExecFactory factory = new NettyDockerCmdExecFactory();
        DockerClientConfig custom = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        dockerClient = DockerClientBuilder
                .getInstance(custom)
                .withDockerCmdExecFactory(factory)
                .build();
        var result = dockerClient.pingCmd().exec();
        log.info("Ping result: {}", result);

        log.info("Setting up network interface");
        Optional<Network> existingNetwork = extractBridgedNetwork();

        if (existingNetwork.isEmpty()) {
            log.info("Creating new network interface");
            dockerClient.createNetworkCmd()
                    .withName(bridgedNetworkName)
                    .withDriver("bridge")
                    .exec();
            existingNetwork = extractBridgedNetwork();
        } else {
            log.info("Using existing network interface");
        }

        applicationHostAddress = existingNetwork.orElseThrow(() -> new ConflictException("Unable to create bridged network"))
                .getIpam()
                .getConfig()
                .get(0)
                .getGateway();
        log.info("Network gateway: {} -> {}", bridgedNetworkName, applicationHostAddress);

        try {
            NetworkInterface networkInterface = findInterfaceByIp(applicationHostAddress);
            if (networkInterface != null) {
                log.info("Found network interface: {} for IP: {}. Please make sure that a route exists for the network and run routing.sh as root!", networkInterface.getDisplayName(), applicationHostAddress);
            }
        } catch (Exception e) {
            log.error("Error finding network interface: {}", e.getMessage());
        }

    }


    private NetworkInterface findInterfaceByIp(String ipAddress) throws Exception {
        InetAddress targetAddress = InetAddress.getByName(ipAddress);
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

            while (inetAddresses.hasMoreElements()) {
                InetAddress address = inetAddresses.nextElement();
                if (address.equals(targetAddress)) {
                    return networkInterface;
                }
            }
        }
        return null;
    }

    private Optional<Network> extractBridgedNetwork() {
        return dockerClient.listNetworksCmd()
                .withNameFilter(bridgedNetworkName)
                .exec()
                .stream()
                .findFirst();
    }

    @Override
    public void clearAllContainers() {
        Network network = dockerClient
                .listNetworksCmd()
                .withNameFilter(bridgedNetworkName)
                .exec()
                .stream().findFirst().get();
        List<Container> runningContainers = dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(x -> {
                            if (x.getNetworkSettings() == null) return false;
                            if (x.getNetworkSettings().getNetworks() == null) return false;
                            return (x.getNetworkSettings().getNetworks().containsKey(network.getName()));
                        }
                ).toList();
        for (Container container : runningContainers) {
            String friendlyName = dockerClient.inspectContainerCmd(container.getId()).exec().getName().substring(1);
            clearContainer(friendlyName);
        }
    }

    @Override
    public void clearContainer(String friendlyName) {
        Container container = dockerClient.listContainersCmd().withNameFilter(Collections.singleton(friendlyName)).exec().getFirst();
        if (container.getState().equals("running")) {
            dockerClient.stopContainerCmd(container.getId()).exec();
        }
        dockerClient.removeContainerCmd(container.getId()).exec();
        log.info("Removed container {}", container.getId());

    }

    private void execInContainer(String containerId, String[] command, boolean detach) throws Exception {
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(command)
                .exec();

        var dc = dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new ExecStartResultCallback(System.out, System.err));
        if (!detach) {
            dc.awaitCompletion();
        }
    }

    private void execInContainer(String containerId, String command, boolean detach) throws Exception {
        execInContainer(containerId, new String[]{"sh", "-c", command}, detach);
    }

    private boolean isValidSshKey(String key) {
        if (key == null) return false;
        key = key.trim();
        String[] parts = key.split(" ");
        if (parts.length != 3) return false;
        List<String> validKeyTypes = List.of(
                "ssh-rsa", "ssh-ed25519", "ssh-dss",
                "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384", "ecdsa-sha2-nistp521"
        );
        if (!validKeyTypes.contains(parts[0])) return false;
        if (!parts[2].contains("@")) return false;
        try {
            Base64.getDecoder().decode(parts[1]);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String spawn(String sshKey) {
        if (!isValidSshKey(sshKey)) return "validation error";
        try {

            CreateContainerResponse container = dockerClient
                    .createContainerCmd(dockerImageName)
                    .withNetworkMode(bridgedNetworkName)
                    .withEnv("SERVERHOST=http://" + applicationHostAddress + ":8080/dab")
                    .withCmd("sh", "-c", "while true; do echo 'here'; sleep 2; done")
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            execInContainer(container.getId(), "mkdir -p /root/.ssh && echo '%s' >> /root/.ssh/authorized_keys && chmod 700 /root/.ssh && chmod 600 /root/.ssh/authorized_keys".formatted(sshKey.replace("'", "'\"'\"'")), false);

            execInContainer(container.getId(), "echo 'export SERVERHOST=http://%s:8080/dab' >> ~/.ssh/environment".formatted(applicationHostAddress), false);

            execInContainer(container.getId(), "git init -b main --bare /root/dots-and-boxes.git", false);

            execInContainer(container.getId(), "mv /docker/InitialRepo/post-update /root/dots-and-boxes.git/hooks/ && chmod +x /root/dots-and-boxes.git/hooks/post-update", false);

            execInContainer(container.getId(), "service ssh start", false);

            execInContainer(container.getId(), "echo '%s' > /myname.is".formatted(dockerClient.inspectContainerCmd(container.getId()).exec().getName().substring(1)), false);

            execInContainer(container.getId(), "git clone file:///root/dots-and-boxes.git /dab-repo && cd /dab-repo && git config --global user.name \"Sir Git-A-Lot\" && git config --global user.email \"git@your.mom\" && cp -r /docker/InitialRepo/DotsAndBoxes . && git add . && git commit -m 'Initial commit' && git push origin main && cd && rm -rf /dab-repo", true);

            return container.getId();
        } catch (Exception e) {
            log.error(e.getMessage());
            return "failure";
        }
    }

    @Override
    public String getContainerIp(String id) {
        return dockerClient.inspectContainerCmd(id).exec().getNetworkSettings().getNetworks().values().stream().findFirst().get().getIpAddress();
    }
}
