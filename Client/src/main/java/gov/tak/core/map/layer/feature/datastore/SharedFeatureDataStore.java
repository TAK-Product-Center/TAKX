package gov.tak.core.map.layer.feature.datastore;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier for {@link com.atakmap.map.layer.feature.FeatureDataStore4} to allow sharing of a single data store between
 * certain classes. See {@link FeatureDataStoreProducer#produceSharedFeatureDataStore()}.
 * 
 * @since 5.2
 */
@Target({PARAMETER, FIELD, METHOD})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface SharedFeatureDataStore
{
}