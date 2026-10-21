package com.thrift.thriftsystem.config;

import com.mongodb.MongoNamespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    // ThriftGroup used to be saved to the default "thriftGroup" collection because its
    // @Document annotation set collation instead of collection; move that data over once
    @Bean
    public ApplicationRunner migrateThriftGroupCollection(MongoTemplate mongoTemplate) {
        return args -> {
            if (mongoTemplate.collectionExists("thriftGroup")
                    && !mongoTemplate.collectionExists("thrift_groups")) {
                mongoTemplate
                        .getCollection("thriftGroup")
                        .renameCollection(
                                new MongoNamespace(
                                        mongoTemplate.getDb().getName(), "thrift_groups"));
                log.info("Renamed collection thriftGroup to thrift_groups");
            }
        };
    }

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
