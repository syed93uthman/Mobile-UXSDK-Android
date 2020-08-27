package com.ikramatic.dronemonitor.manage;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.remotecontroller.RemoteController;

public class RemoteControllerClass {
    private RemoteController controller = null;
    private Boolean dualVideoStatus;

    public RemoteControllerClass() {
    }

    public void setController(RemoteController controller) {
        this.controller = controller;
    }

    public boolean checkLiveVideoStatus(){
        if(controller != null) {
            controller.getLiveViewSimultaneousOutputEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    dualVideoStatus = aBoolean;
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
            return true;
        }
        return false;
    }

    public boolean setLiveVideoStatus(Boolean aBoolean){
        dualVideoStatus = aBoolean;
        if(controller != null) {
            controller.setLiveViewSimultaneousOutputEnabled(aBoolean, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
            return true;
        }
        return false;
    }

    public Boolean getDualVideoStatus() {
        return dualVideoStatus;
    }
}
