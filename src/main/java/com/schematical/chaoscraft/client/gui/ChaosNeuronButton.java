package com.schematical.chaoscraft.client.gui;

import com.schematical.chaoscraft.ai.NeuronBase;
import net.minecraft.client.gui.widget.button.Button;

public class ChaosNeuronButton extends Button {

    final int EXPANDED_WIDTH = 200;
    public static final int MIN_WIDTH = 20;
    public enum State{
        Open,
        Closed
    }
    private final NeuronBase neuronBase;
    private final int i;

    public State state = State.Closed;
    private ChaosDebugOverlayGui chaosDebugOverlayGui;
    public ChaosNeuronButton(NeuronBase neuronBase, ChaosDebugOverlayGui chaosDebugOverlayGui, int i, int y) {
        super(1,
                y,
                MIN_WIDTH,
                20,
                "X",
                (button)->{

                });
        String text = null;
        this.chaosDebugOverlayGui = chaosDebugOverlayGui;
        this.neuronBase  = neuronBase;
        this.i = i;
        this.min();

    }
    public void min() {
        setWidth(MIN_WIDTH);
        switch(neuronBase._base_type()){
            case(com.schematical.chaoscraft.Enum.INPUT):
                x = 10;
                setMessage( "I-" + i);
                break;
            case(com.schematical.chaoscraft.Enum.OUTPUT):
                x = chaosDebugOverlayGui.width - 10 - MIN_WIDTH;
                setMessage("O-" + i);
                break;
            case(com.schematical.chaoscraft.Enum.MIDDLE):
                x = chaosDebugOverlayGui.width / 2;
                setMessage("M-" + i);
                break;

        }
    }

    @Override
    public void onRelease(double p_onRelease_1_, double p_onRelease_3_) {
        //Toggle
        if(state.equals(State.Closed)){
            //Expand it

            this.chaosDebugOverlayGui.minAll();
            this.setWidth(EXPANDED_WIDTH);
            this.setMessage(this.neuronBase.toString());
            switch(neuronBase._base_type()){
                case(com.schematical.chaoscraft.Enum.OUTPUT):
                    x = chaosDebugOverlayGui.width - 10 - EXPANDED_WIDTH;
                    break;
            }

        }else{
            //Open detail view
        }
    }
}