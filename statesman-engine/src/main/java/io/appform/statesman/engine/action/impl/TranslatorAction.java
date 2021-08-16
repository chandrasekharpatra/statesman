package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import io.appform.eventingester.client.EventPublisher;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.TranslatorActionTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "TRANSLATOR")
public class TranslatorAction extends BaseAction<TranslatorActionTemplate> {

    private HandleBarsService handleBarsService;

    @Inject
    public TranslatorAction(HandleBarsService handleBarsService,
                            ObjectMapper mapper,
                            @Named("eventPublisher") final EventPublisher publisher) {
        super(publisher, mapper);
        this.handleBarsService = handleBarsService;
    }

    @Override
    protected JsonNode execute(TranslatorActionTemplate actionTemplate, Workflow workflow) {

        log.debug("Translate Action triggered with Template: {} and Workflow: {}",
            actionTemplate, workflow);

        String translator = actionTemplate.getTranslator();
        JsonNode workflowNode = mapper.valueToTree(workflow);
        JsonNode response = toJsonNode(handleBarsService.transform(translator, workflowNode));

        log.debug("Translation output: {}", response);

        return response;
    }

    @Override
    public ActionType getType() {
        return ActionType.TRANSLATOR;
    }

    private JsonNode toJsonNode(String responseBodyStr) {
        try {
            return mapper.readTree(responseBodyStr);
        } catch (Exception e) {
            log.error("Error while converting to json:" + responseBodyStr, e);
            return null;
        }
    }
}
