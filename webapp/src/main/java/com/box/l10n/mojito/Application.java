package com.box.l10n.mojito;

import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.xml.XmlParsingConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    scanBasePackageClasses = Application.class,
    exclude = {
      QuartzAutoConfiguration.class, // We integrated with Quartz before spring supported it
    })
@EnableSpringConfigured
@EnableJpaAuditing(dateTimeProviderRef = "zonedDateTimeProvider")
@EnableJpaRepositories
@EnableScheduling
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableRetry
@EntityScan(basePackageClasses = BaseEntity.class)
public class Application {

  // TODO(spring2), find replacement - this was commented in previous attempt
  //    @Value("${org.springframework.http.converter.json.indent_output}")
  boolean shouldIndentJacksonOutput;

  public static void main(String[] args) throws IOException {
    // Added a custom model converter to align generated Open API specification schemas with the
    // actual structure of the requests and responses
    ModelConverters.getInstance().addConverter(new CustomModelResolver(Json.mapper()));

    XmlParsingConfiguration.disableXPathLimits();

    SpringApplication springApplication = new SpringApplication(Application.class);
    springApplication.addListeners(new ApplicationPidFileWriter("application.pid"));
    springApplication.run(args);
  }

  /**
   * Fix Spring scanning issue.
   *
   * <p>without this the ObjectMapper instance is not created/available in the container.
   *
   * @return
   */
  @Bean
  @Primary
  public ObjectMapper getObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerHibernateModule();
    return objectMapper;
  }

  @Bean(name = "fail_on_unknown_properties_false")
  public ObjectMapper getObjectMapperFailOnUnknownPropertiesFalse() {
    ObjectMapper objectMapper = ObjectMapper.withNoFailOnUnknownProperties();
    return objectMapper;
  }

  @Bean(name = "smile_format_object_mapper")
  public ObjectMapper getSmileFormatObjectMapper() {
    ObjectMapper objectMapper = ObjectMapper.withSmileEnabled();
    return objectMapper;
  }

  /**
   * Configuration Jackson ObjectMapper
   *
   * @return
   */
  @Bean
  public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
    MappingJackson2HttpMessageConverter mjhmc = new MappingJackson2HttpMessageConverter();

    Jackson2ObjectMapperFactoryBean jomfb = new Jackson2ObjectMapperFactoryBean();
    jomfb.setAutoDetectFields(false);
    jomfb.setIndentOutput(shouldIndentJacksonOutput);

    // To keep backward compatibility with the Joda output, disable write/reading nano seconds with
    // Java time and ZonedDateTime
    // also see {@link com.box.l10n.mojito.json.ObjectMapper}
    jomfb.setFeaturesToDisable(
        SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
        DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

    jomfb.setModulesToInstall(Hibernate6Module.class);
    jomfb.afterPropertiesSet();
    mjhmc.setObjectMapper(jomfb.getObject());

    return mjhmc;
  }

  @Bean
  public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(5);
    threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
    return threadPoolTaskScheduler;
  }

  @Bean
  public RetryTemplate retryTemplate() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(6);

    ExponentialRandomBackOffPolicy exponentialRandomBackOffPolicy =
        new ExponentialRandomBackOffPolicy();
    exponentialRandomBackOffPolicy.setInitialInterval(10);
    exponentialRandomBackOffPolicy.setMultiplier(3);
    exponentialRandomBackOffPolicy.setMaxInterval(5000);

    RetryTemplate template = new RetryTemplate();
    template.setRetryPolicy(retryPolicy);
    template.setBackOffPolicy(exponentialRandomBackOffPolicy);
    template.setThrowLastExceptionOnExhausted(true);

    return template;
  }

  /**
   * Default is {@link org.springframework.data.auditing.CurrentDateTimeProvider} but does not work
   * with ZonedDateTime
   */
  @Bean(name = "zonedDateTimeProvider")
  public DateTimeProvider dateTimeProvider() {
    return () -> Optional.of(ZonedDateTime.now());
  }
}
