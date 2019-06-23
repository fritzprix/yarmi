package net.doodream.yarmi.data;

import net.doodream.yarmi.server.RMIController;

import java.util.Objects;

public class ControllerInfo {
    private int version;
    private Class stubCls;


    static class Builder {
        private final ControllerInfo controllerInfo = new ControllerInfo();
        public Builder version(int version) {
            controllerInfo.version = version;
            return this;
        }

        public Builder stubCls(Class stub) {
            controllerInfo.stubCls = stub;
            return this;
        }

        public ControllerInfo build() {
            return controllerInfo;
        }
    }

    public static <R> ControllerInfo build(RMIController controller) {
        return ControllerInfo.builder()
                .version(controller.getController().version())
                .stubCls(controller.getStub())
                .build();
    }

    public Class getStubCls() {
        return stubCls;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, stubCls.getName());
    }

    private static Builder builder() {
        return new Builder();
    }
}
