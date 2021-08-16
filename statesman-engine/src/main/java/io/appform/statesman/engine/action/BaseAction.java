package io.appform.statesman.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.appform.eventingester.client.EventPublisher;
import io.appform.eventingester.models.Event;
import io.appform.statesman.engine.Constants;
import io.appform.statesman.engine.events.ActionExecutedEvent;
import io.appform.statesman.engine.events.EngineEventType;
import io.appform.statesman.model.Action;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.template.ActionTemplate;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class BaseAction<T extends ActionTemplate> implements Action<T> {

    private static final String FAILED = "FAILED";
    private static final String SUCCESS = "SUCCESS";
    private final EventPublisher publisher;
    protected final ObjectMapper mapper;
    private final Retryer<JsonNode> retryer;

    protected BaseAction(EventPublisher publisher, ObjectMapper mapper) {
        this.publisher = publisher;
        this.mapper = mapper;
        retryer = RetryerBuilder.<JsonNode>newBuilder()
                .retryIfException()
                .withWaitStrategy(WaitStrategies.exponentialWait(100, 5, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .build();
    }

    protected abstract JsonNode execute(T actionTemplate, Workflow workflow);

    @Override
    public JsonNode apply(T actionTemplate, Workflow workflow) {
        String status = SUCCESS;
        JsonNode response = null;
        try {
            response = retryer.call(() -> execute(actionTemplate, workflow));
        } catch (Exception e) {
            status = FAILED;
            log.error("Error while executing action", e);
        }
        publish(actionExecutedEvent(actionTemplate, workflow, status));
        return null == response
                ? NullNode.getInstance()
                : response;
    }


    private void publish(final List<Event> eventList) {
        try {
            if (null != eventList && !eventList.isEmpty()) {
                this.publisher.publish(eventList);
            }
        } catch (final Exception e) {
            log.error("Unable to send event", e);
            throw StatesmanError.propagate(e);
        }
    }

    private List<Event> actionExecutedEvent(T actionTemplate, Workflow workflow, String status) {
        try {
            return Collections.singletonList(Event.builder()
                    .topic(Constants.FOXTROT_REPORTING_TOPIC)
                    .app(Constants.FOXTROT_APP_NAME)
                    .eventType(EngineEventType.ACTION_EXECUTED.name())
                    .groupingKey(workflow.getId())
                    .partitionKey(workflow.getId())
                    .time(new Date())
                    .eventSchemaVersion("v1")
                    .eventData(ActionExecutedEvent.builder()
                            .workflow(status.equalsIgnoreCase(SUCCESS) ? null : mapper.writeValueAsString(workflow))
                            .workflowId(workflow.getId())
                            .workflowTemplateId(workflow.getTemplateId())
                            .actionTemplateId(actionTemplate.getTemplateId())
                            .actionType(actionTemplate.getType().name())
                            .status(status)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error("Error while generating actionExecutedEvent", e);
            return Collections.emptyList();
        }
    }

}