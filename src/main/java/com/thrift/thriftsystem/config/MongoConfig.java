package com.thrift.thriftsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    //    @Bean
    //    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean
    // factoryBean) {
    //        return new ValidatingMongoEventListener(factoryBean);
    //    }
    //    @Bean
    //    public LocalValidatorFactoryBean localValidatorFactoryBean() {
    //        return new LocalValidatorFactoryBean();
    //    }
    //
    // @Bean
    //    public MongoCustomConversions mongoCustomConversions() {
    //        return new MongoCustomConversions(Collections.emptyList());
    // }
}
