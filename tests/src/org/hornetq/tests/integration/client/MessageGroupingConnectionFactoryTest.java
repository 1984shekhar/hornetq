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
package org.hornetq.tests.integration.client;

import org.hornetq.tests.util.UnitTestCase;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQ;
import org.hornetq.core.client.*;
import org.hornetq.core.client.impl.ClientSessionFactoryImpl;
import org.hornetq.core.exception.HornetQException;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.TransportConfiguration;
import org.hornetq.core.message.impl.MessageImpl;
import org.hornetq.utils.SimpleString;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         Created Dec 1, 2009
 */
public class MessageGroupingConnectionFactoryTest   extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(MessageGroupingTest.class);

      private HornetQServer server;

      private ClientSession clientSession;

      private SimpleString qName = new SimpleString("MessageGroupingTestQueue");


   public void testBasicGroupingUsingConnection() throws Exception
   {
      doTestBasicGroupingUsingConnectionFactory(false);
   }

   public void testBasicGroupingUsingConnectionDirect() throws Exception
   {
      doTestBasicGroupingUsingConnectionFactory(true);
   }

   public void testBasicGroupingMultipleProducers() throws Exception
   {
      doTestBasicGroupingMultipleProducers(false);
   }

   public void testBasicGroupingMultipleProducersDirect() throws Exception
   {
      doTestBasicGroupingMultipleProducers(true);   
   }

   private void doTestBasicGroupingUsingConnectionFactory(boolean directDelivery) throws Exception
   {
      ClientProducer clientProducer = clientSession.createProducer(qName);
      ClientConsumer consumer = clientSession.createConsumer(qName);
      ClientConsumer consumer2 = clientSession.createConsumer(qName);
      if (directDelivery)
      {
         clientSession.start();
      }
      int numMessages = 100;
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createTextMessage("m" + i, clientSession);
         clientProducer.send(message);
      }
      if (!directDelivery)
      {
         clientSession.start();
      }
      CountDownLatch latch = new CountDownLatch(numMessages);
      DummyMessageHandler dummyMessageHandler = new DummyMessageHandler(latch, true);
      consumer.setMessageHandler(dummyMessageHandler);
      DummyMessageHandler dummyMessageHandler2 = new DummyMessageHandler(latch, true);
      consumer2.setMessageHandler(dummyMessageHandler2);
      assertTrue(latch.await(10, TimeUnit.SECONDS));
      assertEquals(100, dummyMessageHandler.list.size());
      assertEquals(0, dummyMessageHandler2.list.size());
      consumer.close();
      consumer2.close();
   }

   private void doTestBasicGroupingMultipleProducers(boolean directDelivery) throws Exception
   {
      ClientProducer clientProducer = clientSession.createProducer(qName);
      ClientProducer clientProducer2 = clientSession.createProducer(qName);
      ClientProducer clientProducer3 = clientSession.createProducer(qName);
      ClientConsumer consumer = clientSession.createConsumer(qName);
      ClientConsumer consumer2 = clientSession.createConsumer(qName);
      if (directDelivery)
      {
         clientSession.start();
      }
      int numMessages = 100;
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = createTextMessage("m" + i, clientSession);
         clientProducer.send(message);
         clientProducer2.send(message);
         clientProducer3.send(message);
      }
      if (!directDelivery)
      {
         clientSession.start();
      }
      CountDownLatch latch = new CountDownLatch(numMessages * 3);
      DummyMessageHandler dummyMessageHandler = new DummyMessageHandler(latch, true);
      consumer.setMessageHandler(dummyMessageHandler);
      DummyMessageHandler dummyMessageHandler2 = new DummyMessageHandler(latch, true);
      consumer2.setMessageHandler(dummyMessageHandler2);
      assertTrue(latch.await(10, TimeUnit.SECONDS));
      assertEquals(300, dummyMessageHandler.list.size());
      assertEquals(0, dummyMessageHandler2.list.size());
      consumer.close();
      consumer2.close();
   }

   protected void tearDown() throws Exception
   {
      if (clientSession != null)
      {
         try
         {
            clientSession.close();
         }
         catch (HornetQException e1)
         {
            //
         }
      }
      if (server != null && server.isStarted())
      {
         try
         {
            server.stop();
         }
         catch (Exception e1)
         {
            //
         }
      }
      server = null;
      clientSession = null;

      super.tearDown();
   }

   protected void setUp() throws Exception
   {
      super.setUp();

      ConfigurationImpl configuration = new ConfigurationImpl();
      configuration.setSecurityEnabled(false);
      TransportConfiguration transportConfig = new TransportConfiguration(INVM_ACCEPTOR_FACTORY);
      configuration.getAcceptorConfigurations().add(transportConfig);
      server = HornetQ.newHornetQServer(configuration, false);
      // start the server
      server.start();

      // then we create a client as normal
      ClientSessionFactory sessionFactory = new ClientSessionFactoryImpl(new TransportConfiguration(INVM_CONNECTOR_FACTORY));
      sessionFactory.setGroupID("grp1");
      clientSession = sessionFactory.createSession(false, true, true);
      clientSession.createQueue(qName, qName, null, false);
   }

   private static class DummyMessageHandler implements MessageHandler
   {
      ArrayList<ClientMessage> list = new ArrayList<ClientMessage>();

      private CountDownLatch latch;

      private final boolean acknowledge;

      public DummyMessageHandler(CountDownLatch latch, boolean acknowledge)
      {
         this.latch = latch;
         this.acknowledge = acknowledge;
      }

      public void onMessage(ClientMessage message)
      {
         list.add(message);
         if (acknowledge)
         {
            try
            {
               message.acknowledge();
            }
            catch (HornetQException e)
            {
               // ignore
            }
         }
         latch.countDown();
      }

      public void reset(CountDownLatch latch)
      {
         list.clear();
         this.latch = latch;
      }
   }
}
