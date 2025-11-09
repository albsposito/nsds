package com.lab23;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.ArrayList;

public class SensorProcessorActor extends AbstractActor {

	private double currentAverage;

	private ArrayList<Integer> temperatures;

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(TemperatureMsg.class, this::gotData).build();
	}

	private void gotData(TemperatureMsg msg) throws Exception {
		System.out.println("Temperatures of actor: " + self().path().name() + " " + temperatures);
		System.out.println("SENSOR PROCESSOR " + self() + ": Got data from " + msg.getSender());
		int temperature = msg.getTemperature();
		if(temperature < 0){
			throw new Exception("Temperature must be greater than 0");
		}
		else {
			this.temperatures.add(temperature);
			this.currentAverage = this.computeAverage();
			System.out.println("SENSOR PROCESSOR " + self() + ": Current avg is " + currentAverage);
		}
	}

	static Props props() {
		return Props.create(SensorProcessorActor.class);
	}

	public SensorProcessorActor() {
		this.temperatures = new ArrayList<Integer>();
	}

	private float computeAverage(){
		float sum = 0;
		for(int i=0; i<this.temperatures.size(); i++){
			sum += temperatures.get(i);
		}
		return sum / temperatures.size();
	}
}
