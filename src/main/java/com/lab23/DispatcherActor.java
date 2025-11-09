package com.lab23;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class DispatcherActor extends AbstractActorWithStash {

	private final static int NO_PROCESSORS = 2;

	private LinkedList<ActorRef> processors;

	// maps temperature sensors to sensor data processors
	private HashMap<ActorRef, ActorRef> load_balancing;
	private HashMap<ActorRef, Integer> processors_mapped;

	private int current_index;

	public DispatcherActor() {
		this.processors = new LinkedList<akka.actor.ActorRef>();
		this.load_balancing = new HashMap<>();
		this.processors_mapped = new HashMap<>();
		this.current_index = 0;
	}


	private static SupervisorStrategy strategy =
			new OneForOneStrategy(
					1,
					Duration.ofMinutes(1),
					DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.restart())
						.build());


	@Override
	public SupervisorStrategy supervisorStrategy() { return strategy; }

	@Override
	public AbstractActor.Receive createReceive() {
		return createReceiveRoundRobin();
	}

	public AbstractActor.Receive createReceiveLoadBalancing() {
		// Creates the child actor within the supervisor actor context
		return receiveBuilder()
				.match(Props.class, this::onProps)
				.match(TemperatureMsg.class, this::dispatchDataLoadBalancer)
				.match(DispatchLogicMsg.class, this::onDispatchLogicMsg)
				.build();
	}
	public AbstractActor.Receive createReceiveRoundRobin() {
		// Creates the child actor within the supervisor actor context
		return receiveBuilder()
				.match(Props.class, this::onProps)
				.match(TemperatureMsg.class, this::dispatchDataRoundRobin)
				.match(DispatchLogicMsg.class, this::onDispatchLogicMsg)
				.build();
	}

	private void dispatchDataLoadBalancer(TemperatureMsg msg) {
		System.out.println("Dispatching Data Load Balancer ... ");
		ActorRef sensorProcessor = this.load_balancing.get(msg.getSender());
		if(sensorProcessor != null){
			sensorProcessor.tell(TemperatureMsg.class, self());
			return;
		}

		int minValue = Integer.MAX_VALUE;
		ActorRef minKey = null;

		for (Map.Entry<ActorRef, Integer> entry : this.processors_mapped.entrySet()) {
			if (entry.getValue() < minValue) {
				minValue = entry.getValue();
				minKey = entry.getKey();
			}
		}

		// update hashmaps after associating temperature sensor with data processor
		this.load_balancing.put(msg.getSender(), minKey);
		this.processors_mapped.put(minKey, processors_mapped.get(minKey) + 1);

		try {
			minKey.tell(msg, self());
		} catch(NullPointerException e){
			System.out.println("Unexpected error");
			System.exit(0);
		}

	}

	private void dispatchDataRoundRobin(TemperatureMsg msg) {
		ActorRef processor = null;
		System.out.println("Dispatching Data Round Robin ...)");
		if(current_index == this.processors.size() - 1){
			current_index = 0;
			processor = this.processors.get(0);
			processor.tell(msg, self());
		}
		else{
			current_index += 1;
			processor = this.processors.get(current_index);
			processor.tell(msg, self());
		}
	}

	private void onProps(Props props){
		ActorRef processor = getContext().actorOf(props);
		System.out.println(processor.path().name());
		this.processors.add(processor);
		this.processors_mapped.put(processor, 0);
		System.out.println("processors_mapped: " + processors_mapped);
	}

	private void onTemperatureMsg(TemperatureMsg msg){

	}

	private void onDispatchLogicMsg(DispatchLogicMsg msg){
		if(msg.getLogic() == 1){
			getContext().become(createReceiveLoadBalancing());
		}
		else{
			getContext().become(createReceiveRoundRobin());
		}
	}

	static Props props() {
		return Props.create(DispatcherActor.class);
	}
}
