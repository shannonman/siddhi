/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.siddhi.core.query.processor.stream.window;

import io.siddhi.core.aggregation.AggregationRuntime;
import io.siddhi.core.config.SiddhiQueryContext;
import io.siddhi.core.event.ComplexEventChunk;
import io.siddhi.core.event.state.StateEvent;
import io.siddhi.core.event.stream.StreamEvent;
import io.siddhi.core.event.stream.StreamEventCloner;
import io.siddhi.core.exception.SiddhiAppRuntimeException;
import io.siddhi.core.executor.ExpressionExecutor;
import io.siddhi.core.executor.VariableExpressionExecutor;
import io.siddhi.core.query.input.stream.join.JoinProcessor;
import io.siddhi.core.query.processor.Processor;
import io.siddhi.core.table.Table;
import io.siddhi.core.util.collection.operator.CompiledCondition;
import io.siddhi.core.util.collection.operator.MatchingMetaInfoHolder;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.query.api.aggregation.Within;
import io.siddhi.query.api.expression.Expression;

import java.util.List;
import java.util.Map;

/**
 * This is the {@link WindowProcessor} intended to be used with aggregate join queries.
 * This processor keeps a reference of the Aggregate and finds the items from that.
 * The process method just passes the events to the next
 * {@link JoinProcessor} inorder to handle
 * the events there.
 */
public class AggregateWindowProcessor extends BatchingWindowProcessor implements FindableProcessor {
    private final Within within;
    private final Expression per;
    private AggregationRuntime aggregationRuntime;
    private ConfigReader configReader;
    private boolean outputExpectsExpiredEvents;
    private SiddhiQueryContext siddhiQueryContext;

    public AggregateWindowProcessor(AggregationRuntime aggregationRuntime, Within within, Expression per) {
        this.aggregationRuntime = aggregationRuntime;
        this.within = within;
        this.per = per;
    }

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        boolean outputExpectsExpiredEvents, SiddhiQueryContext siddhiQueryContext) {
        // nothing to be done
        this.configReader = configReader;
        this.outputExpectsExpiredEvents = outputExpectsExpiredEvents;
        this.siddhiQueryContext = siddhiQueryContext;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner) {
        // Pass the event  to the post JoinProcessor
        nextProcessor.process(streamEventChunk);
    }

    @Override
    public StreamEvent find(StateEvent matchingEvent, CompiledCondition compiledCondition) {
        return aggregationRuntime.find(matchingEvent, compiledCondition);
    }

    @Override
    public CompiledCondition compileCondition(Expression condition, MatchingMetaInfoHolder matchingMetaInfoHolder,
                                              List<VariableExpressionExecutor> variableExpressionExecutors,
                                              Map<String, Table> tableMap, SiddhiQueryContext siddhiQueryContext) {
        return aggregationRuntime.compileExpression(condition, within, per, matchingMetaInfoHolder,
                variableExpressionExecutors, tableMap, siddhiQueryContext);
    }

    @Override
    public void start() {
        //Do nothing
    }

    @Override
    public void stop() {
        //Do nothing
    }

    @Override
    public Processor cloneProcessor(String key) {
        try {
            AggregateWindowProcessor streamProcessor = new AggregateWindowProcessor(aggregationRuntime, within, per);
            streamProcessor.inputDefinition = inputDefinition;
            ExpressionExecutor[] innerExpressionExecutors = new ExpressionExecutor[attributeExpressionLength];
            ExpressionExecutor[] attributeExpressionExecutors1 = this.attributeExpressionExecutors;
            for (int i = 0; i < attributeExpressionLength; i++) {
                innerExpressionExecutors[i] = attributeExpressionExecutors1[i].cloneExecutor(key);
            }
            streamProcessor.attributeExpressionExecutors = innerExpressionExecutors;
            streamProcessor.attributeExpressionLength = attributeExpressionLength;
            streamProcessor.additionalAttributes = additionalAttributes;
            streamProcessor.complexEventPopulater = complexEventPopulater;
            streamProcessor.init(metaStreamEvent, inputDefinition, attributeExpressionExecutors, configReader,
                    outputExpectsExpiredEvents, siddhiQueryContext);
            streamProcessor.start();
            return streamProcessor;

        } catch (Exception e) {
            throw new SiddhiAppRuntimeException("Exception in cloning " + this.getClass().getCanonicalName(), e);
        }
    }

    @Override
    public Map<String, Object> currentState() {
        //No state
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        //Nothing to be done
    }
}
