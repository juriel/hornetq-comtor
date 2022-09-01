package org.hornetq.rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@XmlRootElement(name = "rest-messaging")
public class MessageServiceConfiguration
{
   private int producerSessionPoolSize = 10;
   private long producerTimeToLive = -1;
   private int timeoutTaskInterval = 1;
   private int consumerSessionTimeoutSeconds = 300;
   private int consumerWindowSize = -1;
   private boolean defaultDurableSend = false;
   private boolean dupsOk = true;
   private String topicPushStoreDirectory = "topic-push-store";
   private String queuePushStoreDirectory = "queue-push-store";
   private String inVmId = "0";
   private boolean useLinkHeaders = false;

   @XmlElement(name = "server-in-vm-id")
   public String getInVmId()
   {
      return inVmId;
   }

   public void setInVmId(String inVmId)
   {
      this.inVmId = inVmId;
   }

   @XmlElement(name = "use-link-headers")
   public boolean isUseLinkHeaders()
   {
      return useLinkHeaders;
   }

   public void setUseLinkHeaders(boolean useLinkHeaders)
   {
      this.useLinkHeaders = useLinkHeaders;
   }

   @XmlElement(name = "default-durable-send")
   public boolean isDefaultDurableSend()
   {
      return defaultDurableSend;
   }

   public void setDefaultDurableSend(boolean defaultDurableSend)
   {
      this.defaultDurableSend = defaultDurableSend;
   }

   @XmlElement(name = "dups-ok")
   public boolean isDupsOk()
   {
      return dupsOk;
   }

   public void setDupsOk(boolean dupsOk)
   {
      this.dupsOk = dupsOk;
   }

   @XmlElement(name = "topic-push-store-dir")
   public String getTopicPushStoreDirectory()
   {
      return topicPushStoreDirectory;
   }

   public void setTopicPushStoreDirectory(String topicPushStoreDirectory)
   {
      this.topicPushStoreDirectory = topicPushStoreDirectory;
   }

   @XmlElement(name = "queue-push-store-dir")
   public String getQueuePushStoreDirectory()
   {
      return queuePushStoreDirectory;
   }

   public void setQueuePushStoreDirectory(String queuePushStoreDirectory)
   {
      this.queuePushStoreDirectory = queuePushStoreDirectory;
   }

   @XmlElement(name="producer-time-to-live")
   public long getProducerTimeToLive()
   {
      return producerTimeToLive;
   }

   public void setProducerTimeToLive(long producerTimeToLive)
   {
      this.producerTimeToLive = producerTimeToLive;
   }

   @XmlElement(name = "producer-session-pool-size")
   public int getProducerSessionPoolSize()
   {
      return producerSessionPoolSize;
   }

   public void setProducerSessionPoolSize(int producerSessionPoolSize)
   {
      this.producerSessionPoolSize = producerSessionPoolSize;
   }

   @XmlElement(name = "session-timeout-task-interval")
   public int getTimeoutTaskInterval()
   {
      return timeoutTaskInterval;
   }

   public void setTimeoutTaskInterval(int timeoutTaskInterval)
   {
      this.timeoutTaskInterval = timeoutTaskInterval;
   }

   @XmlElement(name = "consumer-session-timeout-seconds")
   public int getConsumerSessionTimeoutSeconds()
   {
      return consumerSessionTimeoutSeconds;
   }

   public void setConsumerSessionTimeoutSeconds(int consumerSessionTimeoutSeconds)
   {
      this.consumerSessionTimeoutSeconds = consumerSessionTimeoutSeconds;
   }

   @XmlElement(name = "consumer-window-size")
   public int getConsumerWindowSize()
   {
      return consumerWindowSize;
   }

   public void setConsumerWindowSize(int consumerWindowSize)
   {
      this.consumerWindowSize = consumerWindowSize;
   }
}
