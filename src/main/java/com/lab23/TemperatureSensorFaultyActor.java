package com.lab23;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class TemperatureSensorFaultyActor extends TemperatureSensorActor {

	private final static int FAULT_TEMP = -50;

    public TemperatureSensorFaultyActor(ActorRef dispatcher) {
        super(dispatcher);
    }

    @Override
	public AbstractActor.Receive createReceive() {
		return receiveBuilder().match(GenerateMsg.class, this::onGenerate)
				.build();
	}

	private void onGenerate(GenerateMsg msg) {
		System.out.println("TEMPERATURE SENSOR "+self()+": Sensing temperature!" + FAULT_TEMP);
		this.dispatcher.tell(new TemperatureMsg(FAULT_TEMP,self()), self());
	}

    static Props props(ActorRef dispatcher) {
        return Props.create(TemperatureSensorFaultyActor.class, () -> new TemperatureSensorFaultyActor(dispatcher));
    }

}
