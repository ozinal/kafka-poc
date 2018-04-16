package com.main.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import com.main.kafka.consumer.Receiver;
import com.main.kafka.producer.Sender;

import example.avro.User;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AppTest {

  @Autowired
  private Sender sender;

  @Autowired
  private Receiver receiver;

  @Autowired
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  @ClassRule
  public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, "avro.t");

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    System.setProperty("kafka.bootstrap-servers", embeddedKafka.getBrokersAsString());
  }

  @Before
  public void setUp() throws Exception {
    // wait until the partitions are assigned
    for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
        .getListenerContainers()) {
      ContainerTestUtils.waitForAssignment(messageListenerContainer,
          embeddedKafka.getPartitionsPerTopic());
    }
  }

  @Test
  public void testReceiver() throws Exception {
    User user = User.newBuilder().setName("John Doe").setFavoriteColor("green")
        .setFavoriteNumber(null).build();
    
    sender.send(user);

    receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);
    assertThat(receiver.getLatch().getCount()).isEqualTo(0);
  }
}
