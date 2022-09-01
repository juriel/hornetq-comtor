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
package org.hornetq.core.server.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.hornetq.core.filter.Filter;
import org.hornetq.core.server.HornetQServerLogger;
import org.hornetq.core.server.MessageReference;
import org.hornetq.core.server.Queue;
import org.hornetq.core.server.ScheduledDeliveryHandler;
import org.hornetq.utils.ConcurrentHashSet;

/**
 * Handles scheduling deliveries to a queue at the correct time.
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="clebert.suconic@jboss.com">Clebert Suconic</a>
 * @author Tom Monk - provided patching on using TreeSet
 */
public class ScheduledDeliveryHandlerImpl implements ScheduledDeliveryHandler
{
   private static final boolean trace = HornetQServerLogger.LOGGER.isTraceEnabled();

   private final ScheduledExecutorService scheduledExecutor;

   private final Object lockDelivery = new Object();


   private final Map<Long, Runnable> runnables = new ConcurrentHashMap<>();

   // This contains RefSchedules which are delegates to the real references
   // just adding some information to keep it in order accordingly to the initial operations
   private final TreeSet<RefScheduled> scheduledReferences = new TreeSet<>(new MessageReferenceComparator());

   public ScheduledDeliveryHandlerImpl(final ScheduledExecutorService scheduledExecutor)
   {
      this.scheduledExecutor = scheduledExecutor;
   }

   public boolean checkAndSchedule(final MessageReference ref, final boolean tail)
   {
      long deliveryTime = ref.getScheduledDeliveryTime();

      if (deliveryTime > 0 && scheduledExecutor != null)
      {
         if (ScheduledDeliveryHandlerImpl.trace)
         {
            HornetQServerLogger.LOGGER.trace("Scheduling delivery for " + ref + " to occur at " + deliveryTime);
         }

         addInPlace(deliveryTime, ref, tail);

         scheduleDelivery(deliveryTime);

         return true;
      }
      return false;
   }


   public void addInPlace(final long deliveryTime, final MessageReference ref, final boolean tail)
   {
      synchronized (scheduledReferences)
      {
         scheduledReferences.add(new RefScheduled(ref, tail));
      }
   }

   public int getScheduledCount()
   {
      synchronized (scheduledReferences)
      {
         return scheduledReferences.size();
      }
   }

   public List<MessageReference> getScheduledReferences()
   {
      List<MessageReference> refs = new LinkedList<MessageReference>();

      synchronized (scheduledReferences)
      {
         for (RefScheduled ref: scheduledReferences)
         {
            refs.add(ref.getRef());
         }
      }
      return refs;
   }

   public List<MessageReference> cancel(final Filter filter)
   {
      List<MessageReference> refs = new ArrayList<MessageReference>();

      synchronized (scheduledReferences)
      {
         Iterator<RefScheduled> iter = scheduledReferences.iterator();

         while (iter.hasNext())
         {
            MessageReference ref = iter.next().getRef();
            if (filter == null || filter.match(ref.getMessage()))
            {
               iter.remove();
               refs.add(ref);
            }
         }
      }
      return refs;
   }

   public MessageReference removeReferenceWithID(final long id)
   {
      synchronized (scheduledReferences)
      {
         Iterator<RefScheduled> iter = scheduledReferences.iterator();
         while (iter.hasNext())
         {
            MessageReference ref = iter.next().getRef();
            if (ref.getMessage().getMessageID() == id)
            {
               iter.remove();
               return ref;
            }
         }
      }

      return null;
   }

   private void scheduleDelivery(final long deliveryTime)
   {

      if (!runnables.containsKey(deliveryTime))
      {
         long now = System.currentTimeMillis();
         ScheduledDeliveryRunnable runnable = new ScheduledDeliveryRunnable(deliveryTime);

         long delay = deliveryTime - now;

         if (delay < 0)
         {
            delay = 0;
         }

         runnables.put(deliveryTime, runnable);
         scheduledExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
      }
   }

   private class ScheduledDeliveryRunnable implements Runnable
   {
      long deliveryTime;
      public ScheduledDeliveryRunnable(final long deliveryTime)
      {
         this.deliveryTime = deliveryTime;
      }

      public void run()
      {
         HashMap<Queue, LinkedList<MessageReference>> refs = new HashMap<Queue, LinkedList<MessageReference>>();

         synchronized (lockDelivery)
         {
            runnables.remove(deliveryTime);

            synchronized (scheduledReferences)
            {

               Iterator<RefScheduled> iter = scheduledReferences.iterator();
               while (iter.hasNext())
               {
                  MessageReference reference = iter.next().getRef();
                  if (reference.getScheduledDeliveryTime() > System.currentTimeMillis())
                  {
                     // We will delivery as long as there are messages to be delivered
                     break;
                  }

                  iter.remove();

                  reference.setScheduledDeliveryTime(0);

                  LinkedList<MessageReference> references = refs.get(reference.getQueue());

                  if (references == null)
                  {
                     references = new LinkedList<MessageReference>();
                     refs.put(reference.getQueue(), references);
                  }

                  references.addFirst(reference);
               }
            }

            for (Map.Entry<Queue, LinkedList<MessageReference>> entry : refs.entrySet())
            {
               entry.getKey().addHead(entry.getValue());
            }

            // Just to speed up GC
            refs.clear();
         }
      }
   }


   // We need a treeset ordered, but we need to order tail operations as well.
   // So, this will serve as a delegate to the object
   class RefScheduled
   {
      private final MessageReference ref;
      private final boolean tail;

      RefScheduled(MessageReference ref, boolean tail)
      {
         this.ref = ref;
         this.tail = tail;
      }

      public MessageReference getRef()
      {
         return ref;
      }

      public boolean isTail()
      {
         return tail;
      }

   }

   static class MessageReferenceComparator implements Comparator<RefScheduled>
   {
      public int compare(RefScheduled ref1, RefScheduled ref2)
      {
         long diff = ref1.getRef().getScheduledDeliveryTime() - ref2.getRef().getScheduledDeliveryTime();

         if (diff < 0L)
         {
            return -1;
         }
         if (diff > 0L)
         {
            return 1;
         }


         // Even if ref1 and ref2 have the same delivery time, we only want to return 0 if they are identical
         if (ref1 == ref2)
         {
            return 0;
         }
         else
         {

            if (ref1.isTail() && !ref2.isTail())
            {
               return 1;
            }
            else
            if (!ref1.isTail() && ref2.isTail())
            {
               return -1;
            }
            if (!ref1.isTail() && !ref2.isTail())
            {
               return -1;
            }
            else
            {
               return 1;
            }
         }
      }
   }

}
