package com.doodream.rmovjs.model;

import com.doodream.rmovjs.server.RMIController;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ControllerInfo {
    private int version;
    private Class stubCls;

    public static <R> ControllerInfo build(RMIController controller) {
        return ControllerInfo.builder()
                .stubCls(controller.getItfcCls())
                .version(controller.getController().version())
                .build();
    }
}
