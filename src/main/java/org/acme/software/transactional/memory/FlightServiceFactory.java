package org.acme.software.transactional.memory;

import org.jboss.stm.Container;

@jakarta.enterprise.context.ApplicationScoped
class FlightServiceFactory {
    private FlightService flightServiceProxy;

    private void initFlightServiceFactory() {
        Container<FlightService> container = new Container<>();
        flightServiceProxy = container.create(new FlightServiceImpl());
    }

    FlightService getInstance() {
        if (flightServiceProxy == null) {
            initFlightServiceFactory();
        }
        return flightServiceProxy;
    }
}
