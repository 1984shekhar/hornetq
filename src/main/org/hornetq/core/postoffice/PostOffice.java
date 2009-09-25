/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.postoffice;

import org.hornetq.core.paging.PagingManager;
import org.hornetq.core.server.HornetQComponent;
import org.hornetq.core.server.Queue;
import org.hornetq.core.server.ServerMessage;
import org.hornetq.core.transaction.Transaction;
import org.hornetq.utils.SimpleString;

/**
 * 
 * A PostOffice instance maintains a mapping of a String address to a Queue. Multiple Queue instances can be bound
 * with the same String address.
 * 
 * Given a message and an address a PostOffice instance will route that message to all the Queue instances that are
 * registered with that address.
 * 
 * Addresses can be any String instance.
 * 
 * A Queue instance can only be bound against a single address in the post office.
 * 
 * The PostOffice also maintains a set of "allowable addresses". These are the addresses that it is legal to
 * route to.
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public interface PostOffice extends HornetQComponent
{
   void addBinding(Binding binding) throws Exception;

   Binding removeBinding(SimpleString uniqueName) throws Exception;

   Bindings getBindingsForAddress(SimpleString address) throws Exception;

   Binding getBinding(SimpleString uniqueName);
   
   Bindings getMatchingBindings(SimpleString address);

   void route(ServerMessage message) throws Exception;
   
   void route(ServerMessage message, Transaction tx) throws Exception;
   
   boolean redistribute(ServerMessage message, final Queue originatingQueue, Transaction tx) throws Exception;

   PagingManager getPagingManager();

   DuplicateIDCache getDuplicateIDCache(SimpleString address);
   
   void sendQueueInfoToQueue(SimpleString queueName, SimpleString address) throws Exception;
   
   Object getNotificationLock();     
}
