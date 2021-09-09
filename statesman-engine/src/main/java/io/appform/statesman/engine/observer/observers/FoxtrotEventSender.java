package io.appform.statesman.engine.observer.observers;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.appform.eventingester.client.EventPublisher;
import io.appform.eventingester.models.Event;
import io.appform.statesman.engine.Constants;
import io.appform.statesman.engine.events.EngineEventType;
import io.appform.statesman.engine.events.FoxtrotStateTransitionEvent;
import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.engine.observer.events.IngressCallbackEvent;
import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import io.appform.statesman.engine.observer.events.WorkflowInitEvent;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Singleton
public class FoxtrotEventSender extends ObservableEventBusSubscriber {


    private static final EventTranslator EVENT_TRANSLATOR = new EventTranslator();

    private final EventPublisher publisher;

    @Inject
    public FoxtrotEventSender(@Named("eventPublisher") final EventPublisher publisher) {
        super(null);
        this.publisher = publisher;
    }

    @Override
    protected void handleEvent(ObservableEvent event) {
        final List<Event> eventList = event.accept(new ObservableEventVisitor<List<Event>>() {
            @Override
            public List<Event> visit(StateTransitionEvent stateTransitionEvent) {

                return stateTransitionEvent.accept(EVENT_TRANSLATOR);
            }

            @Override
            public List<Event> visit(WorkflowInitEvent workflowInitEvent) {
                return workflowInitEvent.accept(EVENT_TRANSLATOR);
            }

            @Override
            public List<Event> visit(IngressCallbackEvent ingressCallbackEvent) {
                return ingressCallbackEvent.accept(EVENT_TRANSLATOR);
            }
        });

        //publish
        publish(eventList);
    }

    private void publish(final List<Event> eventList) {
        try {
            if (null != eventList && !eventList.isEmpty()) {
                publisher.publish(eventList);
            }
        } catch (final Exception e) {
            log.error("unable to send event", e);
            throw StatesmanError.propagate(e);
        }
    }

    private static final class EventTranslator implements ObservableEventVisitor<List<Event>> {

        @Override
        public List<Event> visit(StateTransitionEvent stateTransitionEvent) {
            return Collections.singletonList(
                    Event.builder()
                            .id(UUID.randomUUID().toString())
                            .topic(Constants.FOXTROT_REPORTING_TOPIC)
                            .app(Constants.FOXTROT_APP_NAME)
                            .eventType(EngineEventType.STATE_CHANGED.name())
                            .groupingKey(stateTransitionEvent.getWorkflow().getId())
                            .partitionKey(stateTransitionEvent.getWorkflow().getId())
                            .time(new Date())
                            .eventSchemaVersion("v1")
                            .eventData(FoxtrotStateTransitionEvent.builder()
                                    .workflowId(stateTransitionEvent.getWorkflow().getId())
                                    .workflowTemplateId(stateTransitionEvent.getWorkflow().getTemplateId())
                                    .workflowCreationTime(stateTransitionEvent.getWorkflow().getCreated().getTime())
                                    .oldState(null != stateTransitionEvent.getOldState() ? stateTransitionEvent.getOldState().getName() : null)
                                    .newState(stateTransitionEvent.getWorkflow().getDataObject().getCurrentState().getName())
                                    .terminal(stateTransitionEvent.getWorkflow().getDataObject().getCurrentState().isTerminal())
                                    .data(stateTransitionEvent.getWorkflow().getDataObject().getData())
                                    .update(stateTransitionEvent.getUpdate().getData())
                                    .appliedAction(stateTransitionEvent.getAppliedAction())
                                    .elapseTime(System.currentTimeMillis() - stateTransitionEvent.getWorkflow().getCreated().getTime())
                                    .build())
                            .build());
        }

        @Override
        public List<Event> visit(WorkflowInitEvent workflowInitEvent) {
            Workflow workflow = workflowInitEvent.getWorkflow();
            return Collections.singletonList(Event.builder()
                    .id(UUID.randomUUID().toString())
                    .topic(Constants.FOXTROT_REPORTING_TOPIC)
                    .app(Constants.FOXTROT_APP_NAME)
                    .eventType(EngineEventType.WORKFLOW_INIT.name())
                    .groupingKey(workflow.getId())
                    .partitionKey(workflow.getId())
                    .time(new Date())
                    .eventSchemaVersion("v1")
                    .eventData(io.appform.statesman.engine.events.WorkflowInitEvent.builder()
                            .workflowId(workflow.getId())
                            .workflowTemplateId(workflow.getTemplateId())
                            .currentState(workflow.getDataObject().getCurrentState().getName())
                            .build())
                    .build());
        }

        @Override
        public List<Event> visit(IngressCallbackEvent ingressCallbackEvent) {
            String groupingKey = Strings.isNullOrEmpty(ingressCallbackEvent.getWorkflowId())
                    ? UUID.randomUUID().toString() : ingressCallbackEvent.getWorkflowId();
            return Collections.singletonList(Event.builder()
                    .id(UUID.randomUUID().toString())
                    .topic(Constants.FOXTROT_INGRESS_CALLBACK_TOPIC)
                    .app(Constants.FOXTROT_INGRESS_CALLBACK_APP_NAME)
                    .eventType(EngineEventType.INGRESS_CALLBACK.name())
                    .groupingKey(groupingKey)
                    .partitionKey(groupingKey)
                    .time(new Date())
                    .eventSchemaVersion("v1")
                    .eventData(ingressCallbackEvent)
                    .build());
        }
    }
}
