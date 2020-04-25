package org.acme.global;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.util.Locale;

import static org.eclipse.yasson.YassonConfig.ZERO_TIME_PARSE_DEFAULTING;

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

