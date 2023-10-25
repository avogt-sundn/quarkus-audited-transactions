package org.acme.global;

import static org.eclipse.yasson.YassonConfig.ZERO_TIME_PARSE_DEFAULTING;

import java.util.Locale;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyNamingStrategy;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * https://stackoverflow.com/questions/55234056/how-do-you-adjust-json-config-in-quarkus
 */
@Provider
public class JsonbConfigSetup implements ContextResolver<Jsonb> {

    @Override
    public Jsonb getContext(Class type) {
        JsonbConfig config = new JsonbConfig();
        config.withFormatting(true);
        config.withDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMAN);
        config.withPropertyNamingStrategy(PropertyNamingStrategy.CASE_INSENSITIVE);
        config.setProperty(ZERO_TIME_PARSE_DEFAULTING, true);
        //config.withSerializers(new XMLGregorianCalendarTypeSerializer(d));

        //config.withPropertyVisibilityStrategy(new IgnoreMethods());
        return JsonbBuilder.create(config);
    }
}

