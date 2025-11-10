package Syntax;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AbstractActor.*;
import akka.actor.Props;
import com.lab23.DispatcherActor;
import com.lab23.SensorProcessorActor;
import com.lab23.TemperatureSensorActor;

import java.util.concurrent.TimeoutException;

@SuppressWarnings("all")
// noInspection ALL
public class Syntax {

    // Actor initialization
    final ActorSystem sys = ActorSystem.create("System");
    final ActorRef dispatcher =  sys.actorOf(DispatcherActor.props(), "dispatcher");

    // Pass Parameters when declaring an actor
    static Props props(ActorRef dispatcher) {
        return Props.create(TemperatureSensorActor.class, () -> new TemperatureSensorActor(dispatcher));
    }

    // Create supervised actors
    dispatcher.tell(SensorProcessorActor.props(), ActorRef.noSender());

    // Standard CreateReceive
    public Receive createReceive(){
		return receiveBuilder()
				.match(Props.class, this::onProps)
				.match(TemperatureMsg.class, this::dispatchDataLoadBalancer)
				.match(DispatchLogicMsg.class, this::onDispatchLogicMsg)
				.build();
		}



    // Communication

    // Standard "Tell"
    dispatcher.tell(new TemperatureMsg(temp,self()), self());

    // Ask Pattern
    ActorRef counter;
    try {
        scala.concurrent.Future<Object> waitingForCounter = ask(supervisor, Props.create(CounterActor.class), 5000);
        counter = (ActorRef) waitingForCounter.result(timeout, null);
    } catch(TimeoutException | InterruptedException e1) {
        // handle exception
    }


    // Supervision

    // Use Ask Pattern to ask supervisor to create a supervised actor

    // SupervisorStrategy

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(
                    1,
                    Duration.ofMinutes(1),
                    DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.restart())
                        .build());


    @Override
    public SupervisorStrategy supervisorStrategy() { return strategy; }




    // Behaviours

    // Create a wrapper for createReceive which contains the "default" behaviour, then N separate createReceive, for each behavior

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


    // Then, to switch between behaviors we can use either
    getContext().become(createReceiveRoundRobin());

    // or
    getContext().unbecome();

}
