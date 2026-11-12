package com.cloudgaming.sessionmanager.runtime;

import com.cloudgaming.sessionmanager.config.WorkerProperties;
import com.cloudgaming.sessionmanager.exception.WorkerNotReadyException;
import com.cloudgaming.sessionmanager.exception.WorkerStartFailedException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.HostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DockerWorkerRuntimeTest {

    private DockerClient dockerClient;
    private WorkerReadinessProbe probe;
    private CreateContainerCmd createCmd;
    private StopContainerCmd stopCmd;
    private RemoveContainerCmd removeCmd;

    private static final StartSpec SPEC = new StartSpec("xonotic", "abcdef1234567890", "xonotic-worker");

    @BeforeEach
    void setUp() {
        dockerClient = mock(DockerClient.class);
        probe = mock(WorkerReadinessProbe.class);

        createCmd = mock(CreateContainerCmd.class, RETURNS_SELF);
        stopCmd = mock(StopContainerCmd.class, RETURNS_SELF);
        removeCmd = mock(RemoveContainerCmd.class, RETURNS_SELF);

        CreateContainerResponse created = mock(CreateContainerResponse.class);
        when(created.getId()).thenReturn("container-1");
        when(createCmd.exec()).thenReturn(created);
        when(dockerClient.createContainerCmd(anyString())).thenReturn(createCmd);
        when(dockerClient.startContainerCmd(anyString())).thenReturn(mock(StartContainerCmd.class));
        when(dockerClient.stopContainerCmd(anyString())).thenReturn(stopCmd);
        when(dockerClient.removeContainerCmd(anyString())).thenReturn(removeCmd);
        when(dockerClient.logContainerCmd(anyString())).thenReturn(mock(LogContainerCmd.class, RETURNS_SELF));

        givenRunning(true);
    }

    private void givenRunning(boolean running) {
        InspectContainerCmd inspect = mock(InspectContainerCmd.class);
        InspectContainerResponse response = mock(InspectContainerResponse.class, RETURNS_DEEP_STUBS);
        when(response.getState().getRunning()).thenReturn(running);
        when(inspect.exec()).thenReturn(response);
        when(dockerClient.inspectContainerCmd(anyString())).thenReturn(inspect);
    }

    private DockerWorkerRuntime runtime(boolean uinput, Duration readyTimeout) {
        WorkerProperties props = new WorkerProperties(
                "unix:///var/run/docker.sock", "127.0.0.1", readyTimeout, uinput,
                "4g", 4, null, null, null, null);
        return new DockerWorkerRuntime(dockerClient, props, probe);
    }

    @Test
    void start_appliesNameLabelsEnvAndHostNetwork() {
        when(probe.isReady(anyString(), anyInt())).thenReturn(true);

        WorkerHandle handle = runtime(false, Duration.ofSeconds(5)).start(SPEC);

        assertThat(handle.containerId()).isEqualTo("container-1");
        assertThat(handle.name()).isEqualTo("cg-worker-abcdef12");
        assertThat(handle.webrtcPort()).isEqualTo(9090);
        assertThat(handle.advertisedHost()).isEqualTo("127.0.0.1");

        verify(createCmd).withName("cg-worker-abcdef12");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> labels = ArgumentCaptor.forClass(Map.class);
        verify(createCmd).withLabels(labels.capture());
        assertThat(labels.getValue())
                .containsEntry("cg.managed", "true")
                .containsEntry("cg.room_id", SPEC.roomId())
                .containsEntry("cg.game", "xonotic");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> env = ArgumentCaptor.forClass(List.class);
        verify(createCmd).withEnv(env.capture());
        assertThat(env.getValue()).contains("GAME_ID=xonotic");

        ArgumentCaptor<HostConfig> hostConfig = ArgumentCaptor.forClass(HostConfig.class);
        verify(createCmd).withHostConfig(hostConfig.capture());
        assertThat(hostConfig.getValue().getNetworkMode()).isEqualTo("host");
        assertThat(hostConfig.getValue().getMemory()).isEqualTo(4L * 1024 * 1024 * 1024);
        assertThat(hostConfig.getValue().getNanoCPUs()).isEqualTo(4_000_000_000L);
    }

    @Test
    void start_withoutUinput_doesNotAddDeviceOrCapability() {
        when(probe.isReady(anyString(), anyInt())).thenReturn(true);

        runtime(false, Duration.ofSeconds(5)).start(SPEC);

        ArgumentCaptor<HostConfig> captor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createCmd).withHostConfig(captor.capture());
        assertThat(captor.getValue().getDevices()).isNullOrEmpty();
        assertThat(captor.getValue().getCapAdd()).isNullOrEmpty();
    }

    @Test
    void start_withUinput_addsDeviceAndSysAdmin() {
        when(probe.isReady(anyString(), anyInt())).thenReturn(true);

        runtime(true, Duration.ofSeconds(5)).start(SPEC);

        ArgumentCaptor<HostConfig> captor = ArgumentCaptor.forClass(HostConfig.class);
        verify(createCmd).withHostConfig(captor.capture());
        Device[] devices = captor.getValue().getDevices();
        assertThat(devices).hasSize(1);
        assertThat(devices[0].getPathOnHost()).isEqualTo("/dev/uinput");
        assertThat(captor.getValue().getCapAdd()).contains(Capability.SYS_ADMIN);
    }

    @Test
    void start_whenWorkerNeverReady_throwsNotReadyAndCleansUp() {
        when(probe.isReady(anyString(), anyInt())).thenReturn(false);

        DockerWorkerRuntime runtime = runtime(false, Duration.ofMillis(600));

        assertThatThrownBy(() -> runtime.start(SPEC))
                .isInstanceOf(WorkerNotReadyException.class);
        verify(probe, atLeastOnce()).isReady("127.0.0.1", 9090);
        verify(dockerClient).stopContainerCmd("container-1");
        verify(dockerClient).removeContainerCmd("container-1");
    }

    @Test
    void start_whenContainerExited_throwsStartFailedWithoutWaitingTimeout() {
        givenRunning(false);

        DockerWorkerRuntime runtime = runtime(false, Duration.ofSeconds(90));

        assertThatThrownBy(() -> runtime.start(SPEC))
                .isInstanceOf(WorkerStartFailedException.class);
        verify(dockerClient).removeContainerCmd("container-1");
    }

    @Test
    void stop_usesGivenTimeout() {
        runtime(false, Duration.ofSeconds(5)).stop("container-1", Duration.ofSeconds(30));

        verify(stopCmd).withTimeout(30);
        verify(stopCmd).exec();
    }

    @Test
    void remove_forcesRemoval() {
        runtime(false, Duration.ofSeconds(5)).remove("container-1");

        verify(removeCmd).withForce(true);
        verify(removeCmd).exec();
    }

    @Test
    void listManaged_mapsLabelsAndState() {
        ListContainersCmd listCmd = mock(ListContainersCmd.class, RETURNS_SELF);
        Container container = mock(Container.class);
        when(container.getId()).thenReturn("container-1");
        when(container.getNames()).thenReturn(new String[]{"/cg-worker-abcdef12"});
        when(container.getLabels()).thenReturn(Map.of("cg.room_id", "room-1", "cg.game", "xonotic"));
        when(container.getState()).thenReturn("running");
        when(listCmd.exec()).thenReturn(List.of(container));
        when(dockerClient.listContainersCmd()).thenReturn(listCmd);

        List<ManagedContainer> managed = runtime(false, Duration.ofSeconds(5)).listManaged();

        assertThat(managed).hasSize(1);
        assertThat(managed.getFirst().roomId()).isEqualTo("room-1");
        assertThat(managed.getFirst().gameSlug()).isEqualTo("xonotic");
        assertThat(managed.getFirst().running()).isTrue();
    }
}
