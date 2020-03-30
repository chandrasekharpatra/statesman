package io.appform.statesman.engine.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.appform.statesman.engine.storage.data.StoredActionTemplate;
import io.appform.statesman.engine.storage.data.StoredStateTransition;
import io.appform.statesman.engine.storage.data.StoredWorkflowInstance;
import io.appform.statesman.engine.storage.data.StoredWorkflowTemplate;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.StateTransition;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.model.action.template.ActionTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class WorkflowUtils {

    private WorkflowUtils() {
    }

    public static StoredWorkflowTemplate toDao(String name, List<String> attributes) {
        return StoredWorkflowTemplate.builder()
                .active(true)
                .name(name)
                .templateId(UUID.randomUUID().toString())
                .attributes(MapperUtils.serialize(attributes))
                .build();
    }

    public static WorkflowTemplate toDto(StoredWorkflowTemplate workflowTemplate) {
        return WorkflowTemplate.builder()
                .id(workflowTemplate.getTemplateId())
                .name(workflowTemplate.getName())
                .attributes(MapperUtils.deserialize(workflowTemplate.getAttributes(), new TypeReference<List<String>>() {
                }))
                .build();
    }

    public static StoredWorkflowInstance toInstanceDao(String templateId, JsonNode initialData) {
        return StoredWorkflowInstance.builder()
                .templateId(templateId)
                .completed(false)
                .workflowId(UUID.randomUUID().toString())
                .data(MapperUtils.serialize(DataObject.builder()
                        .data(initialData)
                        .build()))
                .build();
    }

    public static StoredWorkflowInstance toInstanceDao(Workflow workflow) {
        return StoredWorkflowInstance.builder()
                .templateId(workflow.getTemplateId())
                .workflowId(workflow.getId())
                .data(MapperUtils.serialize(workflow.getDataObject()))
                .currentState(workflow.getDataObject().getCurrentState().getName())
                .completed(workflow.getDataObject().getCurrentState().isTerminal())
                .build();
    }

    public static Workflow toInstanceDto(StoredWorkflowInstance storedWorkflowInstance) {
        return Workflow.builder()
                .id(storedWorkflowInstance.getWorkflowId())
                .templateId(storedWorkflowInstance.getTemplateId())
                .dataObject(MapperUtils.deserialize(storedWorkflowInstance.getData(), DataObject.class))
                .build();
    }

    public static StateTransition toDto(StoredStateTransition storedStateTransitions) {
        return MapperUtils.deserialize(storedStateTransitions.getData(), StateTransition.class);
    }

    public static StoredStateTransition toDao(String workflowTemplateId, String fromState, StateTransition stateTransition) {
        return StoredStateTransition.builder()
                .active(true)
                .fromState(fromState)
                .workflowTemplateId(workflowTemplateId)
                .data(MapperUtils.serialize(stateTransition))
                .build();
    }

    public static ActionTemplate toDto(StoredActionTemplate storedActionTemplate) {
        return MapperUtils.deserialize(storedActionTemplate.getData(), ActionTemplate.class);
    }

    public static StoredActionTemplate toDao(ActionTemplate actionTemplate) {
        String templateId = Strings.isNullOrEmpty(actionTemplate.getTemplateId())
                                ? UUID.randomUUID().toString() : actionTemplate.getTemplateId();
        return StoredActionTemplate.builder()
                .templateId(templateId)
                .active(true)
                .actionType(actionTemplate.getType().name())
                .name(actionTemplate.getName())
                .data(MapperUtils.serialize(actionTemplate))
                .build();
    }

}