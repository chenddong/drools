/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.audit;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.drools.core.FactHandle;
import org.drools.core.WorkingMemory;
import org.drools.core.WorkingMemoryEventManager;
import org.drools.core.audit.event.ActivationLogEvent;
import org.drools.core.audit.event.ILogEventFilter;
import org.drools.core.audit.event.LogEvent;
import org.drools.core.audit.event.ObjectLogEvent;
import org.drools.core.audit.event.RuleBaseLogEvent;
import org.drools.core.audit.event.RuleFlowGroupLogEvent;
import org.drools.core.audit.event.RuleFlowLogEvent;
import org.drools.core.audit.event.RuleFlowNodeLogEvent;
import org.drools.core.audit.event.RuleFlowVariableLogEvent;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.event.ActivationCancelledEvent;
import org.drools.core.event.ActivationCreatedEvent;
import org.drools.core.event.AfterActivationFiredEvent;
import org.drools.core.event.AfterFunctionRemovedEvent;
import org.drools.core.event.AfterPackageAddedEvent;
import org.drools.core.event.AfterPackageRemovedEvent;
import org.drools.core.event.AfterProcessAddedEvent;
import org.drools.core.event.AfterProcessRemovedEvent;
import org.drools.core.event.AfterRuleAddedEvent;
import org.drools.core.event.AfterRuleBaseLockedEvent;
import org.drools.core.event.AfterRuleBaseUnlockedEvent;
import org.drools.core.event.AfterRuleRemovedEvent;
import org.drools.core.event.AgendaEventListener;
import org.drools.core.event.AgendaGroupPoppedEvent;
import org.drools.core.event.AgendaGroupPushedEvent;
import org.drools.core.event.BeforeActivationFiredEvent;
import org.drools.core.event.BeforeFunctionRemovedEvent;
import org.drools.core.event.BeforePackageAddedEvent;
import org.drools.core.event.BeforePackageRemovedEvent;
import org.drools.core.event.BeforeProcessAddedEvent;
import org.drools.core.event.BeforeProcessRemovedEvent;
import org.drools.core.event.BeforeRuleAddedEvent;
import org.drools.core.event.BeforeRuleBaseLockedEvent;
import org.drools.core.event.BeforeRuleBaseUnlockedEvent;
import org.drools.core.event.BeforeRuleRemovedEvent;
import org.drools.core.event.ObjectInsertedEvent;
import org.drools.core.event.ObjectRetractedEvent;
import org.drools.core.event.ObjectUpdatedEvent;
import org.drools.core.event.RuleBaseEventListener;
import org.drools.core.event.RuleFlowGroupActivatedEvent;
import org.drools.core.event.RuleFlowGroupDeactivatedEvent;
import org.drools.core.event.WorkingMemoryEventListener;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.drools.core.impl.StatelessKnowledgeSessionImpl;
import org.drools.core.reteoo.ReteooWorkingMemoryInterface;
import org.drools.core.rule.Declaration;
import org.drools.core.runtime.process.InternalProcessRuntime;
import org.drools.core.spi.Activation;
import org.drools.core.spi.Tuple;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.NodeInstanceContainer;

/**
 * A logger of events generated by a working memory.
 * It listens to the events generated by the working memory and
 * creates associated log event (containing a snapshot of the
 * state of the working event at that time).
 * 
 * Filters can be used to filter out unwanted events.
 * 
 * Subclasses of this class should implement the logEventCreated(LogEvent)
 * method and store this information, like for example log to file
 * or database.
 */
public abstract class WorkingMemoryLogger
    implements
    WorkingMemoryEventListener,
    AgendaEventListener,
    ProcessEventListener,
    RuleBaseEventListener {

    private List<ILogEventFilter>    filters = new ArrayList<ILogEventFilter>();

    public WorkingMemoryLogger() {
    }

    /**
     * Creates a new working memory logger for the given working memory.
     * 
     * @param workingMemory
     */
    public WorkingMemoryLogger(final WorkingMemory workingMemory) {
        workingMemory.addEventListener( (WorkingMemoryEventListener) this );
        workingMemory.addEventListener( (AgendaEventListener) this );
        InternalProcessRuntime processRuntime = ((InternalWorkingMemory) workingMemory).getProcessRuntime();
        if (processRuntime != null) {
            processRuntime.addEventListener( (ProcessEventListener) this );
        }
        workingMemory.addEventListener( (RuleBaseEventListener) this );
    }
    
    public WorkingMemoryLogger(final KnowledgeRuntimeEventManager session) {
        if (session instanceof StatefulKnowledgeSessionImpl) {
            WorkingMemoryEventManager eventManager = ((StatefulKnowledgeSessionImpl) session).session;
            eventManager.addEventListener( (WorkingMemoryEventListener) this );
            eventManager.addEventListener( (AgendaEventListener) this );
            eventManager.addEventListener( (RuleBaseEventListener) this );
            InternalProcessRuntime processRuntime = ((StatefulKnowledgeSessionImpl) session).session.getProcessRuntime();
            if (processRuntime != null) {
                processRuntime.addEventListener( (ProcessEventListener) this );
            }
        } else if (session instanceof StatelessKnowledgeSessionImpl) {
            ((StatelessKnowledgeSessionImpl) session).workingMemoryEventSupport.addEventListener( this );
            ((StatelessKnowledgeSessionImpl) session).agendaEventSupport.addEventListener( this );
            ((StatelessKnowledgeSessionImpl) session).processEventSupport.addEventListener( this );
            ((StatelessKnowledgeSessionImpl) session).getRuleBase().addEventListener( this );
        } else if (session instanceof CommandBasedStatefulKnowledgeSession) {
            ReteooWorkingMemoryInterface eventManager =
                ((StatefulKnowledgeSessionImpl)((KnowledgeCommandContext)((CommandBasedStatefulKnowledgeSession) session).getCommandService().getContext()).getKieSession()).session;
            eventManager.addEventListener( (WorkingMemoryEventListener) this );
            eventManager.addEventListener( (AgendaEventListener) this );
            InternalProcessRuntime processRuntime = eventManager.getProcessRuntime();
            eventManager.addEventListener( (RuleBaseEventListener) this );
            if (processRuntime != null) {
                processRuntime.addEventListener( (ProcessEventListener) this );
            }
        } else {
            throw new IllegalArgumentException("Not supported session in logger: " + session.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        filters = (List<ILogEventFilter>) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(filters);
    }

    /**
     * This method is invoked every time a new log event is created.
     * Subclasses should implement this method and store the event,
     * like for example log to a file or database.
     * 
     * @param logEvent
     */
    public abstract void logEventCreated(LogEvent logEvent);

    /**
     * This method is invoked every time a new log event is created.
     * It filters out unwanted events.
     * 
     * @param logEvent
     */
    private void filterLogEvent(final LogEvent logEvent) {
        for ( ILogEventFilter filter: this.filters) {
            // do nothing if one of the filters doesn't accept the event
            if ( !filter.acceptEvent( logEvent ) ) {
                return;
            }
        }
        // if all the filters accepted the event, signal the creation
        // of the event
        logEventCreated( logEvent );
    }

    /**
     * Adds the given filter to the list of filters for this event log.
     * A log event must be accepted by all the filters to be entered in
     * the event log.
     *
     * @param filter The filter that should be added.
     */
    public void addFilter(final ILogEventFilter filter) {
        if ( filter == null ) {
            throw new NullPointerException();
        }
        this.filters.add( filter );
    }

    /**
     * Removes the given filter from the list of filters for this event log.
     * If the given filter was not a filter of this event log, nothing
     * happens.
     *
     * @param filter The filter that should be removed.
     */
    public void removeFilter(final ILogEventFilter filter) {
        this.filters.remove( filter );
    }

    /**
     * Clears all filters of this event log.
     */
    public void clearFilters() {
        this.filters.clear();
    }

    /**
     * @see org.kie.api.event.WorkingMemoryEventListener
     */
    public void objectInserted(final ObjectInsertedEvent event) {
        filterLogEvent( new ObjectLogEvent( LogEvent.INSERTED,
                                            ((InternalFactHandle) event.getFactHandle()).getId(),
                                            event.getObject().toString() ) );
    }

    /**
     * @see org.kie.api.event.WorkingMemoryEventListener
     */
    public void objectUpdated(final ObjectUpdatedEvent event) {
        filterLogEvent( new ObjectLogEvent( LogEvent.UPDATED,
                                            ((InternalFactHandle) event.getFactHandle()).getId(),
                                            event.getObject().toString() ) );
    }

    /**
     * @see org.kie.api.event.WorkingMemoryEventListener
     */
    public void objectRetracted(final ObjectRetractedEvent event) {
        filterLogEvent( new ObjectLogEvent( LogEvent.RETRACTED,
                                            ((InternalFactHandle) event.getFactHandle()).getId(),
                                            event.getOldObject().toString() ) );
    }

    /**
     * @see org.kie.api.event.AgendaEventListener
     */
    public void activationCreated(final ActivationCreatedEvent event,
                                  final WorkingMemory workingMemory) {
        filterLogEvent( new ActivationLogEvent( LogEvent.ACTIVATION_CREATED,
                                                getActivationId( event.getActivation() ),
                                                event.getActivation().getRule().getName(),
                                                extractDeclarations( event.getActivation(), workingMemory ),
                                                event.getActivation().getRule().getRuleFlowGroup() ) );
    }

    /**
     * @see org.kie.api.event.AgendaEventListener
     */
    public void activationCancelled(final ActivationCancelledEvent event,
                                    final WorkingMemory workingMemory) {
        filterLogEvent( new ActivationLogEvent( LogEvent.ACTIVATION_CANCELLED,
                                                getActivationId( event.getActivation() ),
                                                event.getActivation().getRule().getName(),
                                                extractDeclarations( event.getActivation(), workingMemory ),
                                                event.getActivation().getRule().getRuleFlowGroup() ) );
    }

    /**
     * @see org.kie.api.event.AgendaEventListener
     */
    public void beforeActivationFired(final BeforeActivationFiredEvent event,
                                      final WorkingMemory workingMemory) {
        filterLogEvent( new ActivationLogEvent( LogEvent.BEFORE_ACTIVATION_FIRE,
                                                getActivationId( event.getActivation() ),
                                                event.getActivation().getRule().getName(),
                                                extractDeclarations( event.getActivation(), workingMemory ),
                                                event.getActivation().getRule().getRuleFlowGroup() ) );
    }

    /**
     * @see org.kie.api.event.AgendaEventListener
     */
    public void afterActivationFired(final AfterActivationFiredEvent event,
                                     final WorkingMemory workingMemory) {
        filterLogEvent( new ActivationLogEvent( LogEvent.AFTER_ACTIVATION_FIRE,
                                                getActivationId( event.getActivation() ),
                                                event.getActivation().getRule().getName(),
                                                extractDeclarations( event.getActivation(), workingMemory ),
                                                event.getActivation().getRule().getRuleFlowGroup() ) );
    }

    /**
     * Creates a string representation of the declarations of an activation.
     * This is a list of name-value-pairs for each of the declarations in the
     * tuple of the activation.  The name is the identifier (=name) of the
     * declaration, and the value is a toString of the value of the
     * parameter, followed by the id of the fact between parentheses.
     * 
     * @param activation The activation from which the declarations should be extracted
     * @return A String represetation of the declarations of the activation.
     */
    private String extractDeclarations(final Activation activation,  final WorkingMemory workingMemory) {
        final StringBuilder result = new StringBuilder();
        final Tuple tuple = activation.getTuple();
        final Map<?, ?> declarations = activation.getSubRule().getOuterDeclarations();
        for ( Iterator<?> it = declarations.values().iterator(); it.hasNext(); ) {
            final Declaration declaration = (Declaration) it.next();
            final FactHandle handle = tuple.get( declaration );
            if ( handle instanceof InternalFactHandle ) {
                final InternalFactHandle handleImpl = (InternalFactHandle) handle;
                if ( handleImpl.getId() == -1 ) {
                    // This handle is now invalid, probably due to an fact retraction
                    continue;
                }
                final Object value = declaration.getValue( (InternalWorkingMemory) workingMemory, handleImpl.getObject() );

                result.append( declaration.getIdentifier() );
                result.append( "=" );
                if ( value == null ) {
                    // this should never occur
                    result.append( "null" );
                } else {
                    result.append( value );
                    result.append( "(" );
                    result.append( handleImpl.getId() );
                    result.append( ")" );
                }
            }
            if ( it.hasNext() ) {
                result.append( "; " );
            }
        }
        return result.toString();
    }

    /**
     * Returns a String that can be used as unique identifier for an
     * activation.  Since the activationId is the same for all assertions
     * that are created during a single insert, update or retract, the
     * key of the tuple of the activation is added too (which is a set
     * of fact handle ids). 
     * 
     * @param activation The activation for which a unique id should be generated
     * @return A unique id for the activation
     */
    private static String getActivationId(final Activation activation) {
        final StringBuilder result = new StringBuilder( activation.getRule().getName() );
        result.append( " [" );
        final Tuple tuple = activation.getTuple();
        final FactHandle[] handles = tuple.getFactHandles();
        for ( int i = 0; i < handles.length; i++ ) {
            result.append( ((InternalFactHandle) handles[i]).getId() );
            if ( i < handles.length - 1 ) {
                result.append( ", " );
            }
        }
        return result.append( "]" ).toString();
    }
    
    public void agendaGroupPopped(final AgendaGroupPoppedEvent event,
                                  final WorkingMemory workingMemory) {
        // we don't audit this yet     
    }

    public void agendaGroupPushed(final AgendaGroupPushedEvent event,
                                  final WorkingMemory workingMemory) {
        // we don't audit this yet        
    }
    
    public void beforeRuleFlowGroupActivated(
            RuleFlowGroupActivatedEvent event,
            WorkingMemory workingMemory) {
        filterLogEvent(new RuleFlowGroupLogEvent(
                LogEvent.BEFORE_RULEFLOW_GROUP_ACTIVATED, event
                        .getRuleFlowGroup().getName(), event.getRuleFlowGroup()
                        .size()));
    }
    
    public void afterRuleFlowGroupActivated(
            RuleFlowGroupActivatedEvent event,
            WorkingMemory workingMemory) {
        filterLogEvent(new RuleFlowGroupLogEvent(
                LogEvent.AFTER_RULEFLOW_GROUP_ACTIVATED,
                event.getRuleFlowGroup().getName(),
                event.getRuleFlowGroup().size()));
    }

    public void beforeRuleFlowGroupDeactivated(
            RuleFlowGroupDeactivatedEvent event, 
            WorkingMemory workingMemory) {
        filterLogEvent(new RuleFlowGroupLogEvent(
                LogEvent.BEFORE_RULEFLOW_GROUP_DEACTIVATED,
                event.getRuleFlowGroup().getName(),
                event.getRuleFlowGroup().size()));
    }
    
    public void afterRuleFlowGroupDeactivated(
            RuleFlowGroupDeactivatedEvent event,
            WorkingMemory workingMemory) {
        filterLogEvent(new RuleFlowGroupLogEvent(
                LogEvent.AFTER_RULEFLOW_GROUP_DEACTIVATED,
                event.getRuleFlowGroup().getName(),
                event.getRuleFlowGroup().size()));
    }
    
    public void beforeProcessStarted(ProcessStartedEvent event) {
        filterLogEvent( new RuleFlowLogEvent( LogEvent.BEFORE_RULEFLOW_CREATED,
                event.getProcessInstance().getProcessId(),
                event.getProcessInstance().getProcessName(),
                event.getProcessInstance().getId()) );
    }

    public void afterProcessStarted(ProcessStartedEvent event) {
        filterLogEvent(new RuleFlowLogEvent(LogEvent.AFTER_RULEFLOW_CREATED,
                event.getProcessInstance().getProcessId(),
                event.getProcessInstance().getProcessName(),
                event.getProcessInstance().getId()) );
    }

    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        filterLogEvent( new RuleFlowLogEvent( LogEvent.BEFORE_RULEFLOW_COMPLETED,
                event.getProcessInstance().getProcessId(),
                event.getProcessInstance().getProcessName(),
                event.getProcessInstance().getId()) );
    }
    
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        filterLogEvent(new RuleFlowLogEvent(LogEvent.AFTER_RULEFLOW_COMPLETED,
                event.getProcessInstance().getProcessId(),
                event.getProcessInstance().getProcessName(),
                event.getProcessInstance().getId()) );
    }

    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        filterLogEvent(new RuleFlowNodeLogEvent(LogEvent.BEFORE_RULEFLOW_NODE_TRIGGERED,
                createNodeId(event.getNodeInstance()),
                event.getNodeInstance().getNodeName(),
                createNodeInstanceId(event.getNodeInstance()),
                event.getProcessInstance().getProcessId(),
                event.getProcessInstance().getProcessName(),
                event.getProcessInstance().getId()) );
    }

    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        filterLogEvent(new RuleFlowNodeLogEvent(LogEvent.AFTER_RULEFLOW_NODE_TRIGGERED,
                createNodeId(event.getNodeInstance()),
                event.getNodeInstance().getNodeName(),
                createNodeInstanceId(event.getNodeInstance()),
                event.getProcessInstance().getProcessId(), 
                event.getProcessInstance().getProcessName(),
                event.getProcessInstance().getId()) );
    }
    
    private String createNodeId(NodeInstance nodeInstance) {
        Node node = nodeInstance.getNode();
        if (node == null) {
            return "";
        }
        String nodeId = "" + node.getId();
        NodeContainer nodeContainer = node.getNodeContainer();
        while (nodeContainer != null) {
            if (nodeContainer instanceof Node) {
                node = (Node) nodeContainer;
                nodeContainer = node.getNodeContainer();
                // TODO fix this filter out hidden compositeNode inside ForEach node
                if (!(nodeContainer.getClass().getName().endsWith("ForEachNode"))) {
                    nodeId = node.getId() + ":" + nodeId;
                }
            } else {
                break;
            }
        }
        return nodeId;
    }

    private String createNodeInstanceId(NodeInstance nodeInstance) {
        String nodeInstanceId = "" + nodeInstance.getId();
        NodeInstanceContainer nodeContainer = nodeInstance.getNodeInstanceContainer();
        while (nodeContainer != null) {
            if (nodeContainer instanceof NodeInstance) {
                nodeInstance = (NodeInstance) nodeContainer;
                nodeInstanceId = nodeInstance.getId() + ":" + nodeInstanceId;
                nodeContainer = nodeInstance.getNodeInstanceContainer();
            } else {
                break;
            }
        }
        return nodeInstanceId;
    }

    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        filterLogEvent(new RuleFlowNodeLogEvent(LogEvent.BEFORE_RULEFLOW_NODE_EXITED,
            createNodeId(event.getNodeInstance()),
            event.getNodeInstance().getNodeName(),
            createNodeInstanceId(event.getNodeInstance()),
            event.getProcessInstance().getProcessId(),
            event.getProcessInstance().getProcessName(),
            event.getProcessInstance().getId()) );
    }

    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        filterLogEvent(new RuleFlowNodeLogEvent(LogEvent.AFTER_RULEFLOW_NODE_EXITED,
            createNodeId(event.getNodeInstance()),
            event.getNodeInstance().getNodeName(),
            createNodeInstanceId(event.getNodeInstance()),
            event.getProcessInstance().getProcessId(), 
            event.getProcessInstance().getProcessName(),
            event.getProcessInstance().getId()) );
    }

    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        filterLogEvent(new RuleFlowVariableLogEvent(LogEvent.BEFORE_VARIABLE_INSTANCE_CHANGED,
            event.getVariableId(),
            event.getVariableInstanceId(),
            event.getProcessInstance().getProcessId(), 
            event.getProcessInstance().getProcessName(),
            event.getProcessInstance().getId(),
            event.getNewValue() == null ? "null" : event.getNewValue().toString()) );
    }

    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        filterLogEvent(new RuleFlowVariableLogEvent(LogEvent.AFTER_VARIABLE_INSTANCE_CHANGED,
            event.getVariableId(),
            event.getVariableInstanceId(),
            event.getProcessInstance().getProcessId(), 
            event.getProcessInstance().getProcessName(),
            event.getProcessInstance().getId(),
            event.getNewValue() == null ? "null" : event.getNewValue().toString()) );
    }

    public void afterPackageAdded(AfterPackageAddedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.AFTER_PACKAGE_ADDED,
                                              event.getPackage().getName(),
                                              null ) );
    }

    public void afterPackageRemoved(AfterPackageRemovedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.AFTER_PACKAGE_REMOVED,
                                              event.getPackage().getName(),
                                              null ) );
    }

    public void afterRuleAdded(AfterRuleAddedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.AFTER_RULE_ADDED,
                                              event.getPackage().getName(),
                                              event.getRule().getName() ) );
    }

    public void afterRuleRemoved(AfterRuleRemovedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.AFTER_RULE_REMOVED,
                                              event.getPackage().getName(),
                                              event.getRule().getName() ) );
    }

    public void beforePackageAdded(BeforePackageAddedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.BEFORE_PACKAGE_ADDED,
                                              event.getPackage().getName(),
                                              null ) );
    }

    public void beforePackageRemoved(BeforePackageRemovedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.BEFORE_PACKAGE_REMOVED,
                                              event.getPackage().getName(),
                                              null ) );
    }

    public void beforeRuleAdded(BeforeRuleAddedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.BEFORE_RULE_ADDED,
                                              event.getPackage().getName(),
                                              event.getRule().getName() ) );
    }

    public void beforeRuleRemoved(BeforeRuleRemovedEvent event) {
        filterLogEvent( new RuleBaseLogEvent( LogEvent.BEFORE_RULE_REMOVED,
                                              event.getPackage().getName(),
                                              event.getRule().getName() ) );
    }
    
    public void afterFunctionRemoved(AfterFunctionRemovedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void afterRuleBaseLocked(AfterRuleBaseLockedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void afterRuleBaseUnlocked(AfterRuleBaseUnlockedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void beforeFunctionRemoved(BeforeFunctionRemovedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void beforeRuleBaseLocked(BeforeRuleBaseLockedEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void beforeRuleBaseUnlocked(BeforeRuleBaseUnlockedEvent event) {
        // TODO Auto-generated method stub
        
    }

	public void beforeProcessAdded(BeforeProcessAddedEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void afterProcessAdded(AfterProcessAddedEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void beforeProcessRemoved(BeforeProcessRemovedEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void afterProcessRemoved(AfterProcessRemovedEvent event) {
		// TODO Auto-generated method stub
		
	}
}
