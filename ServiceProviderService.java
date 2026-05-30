package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.ServiceProvider;

import java.util.List;
import java.util.Optional;

public interface ServiceProviderService {

    ServiceProvider createServiceProvider(ServiceProvider serviceProvider);

    List<ServiceProvider> getAllServiceProviders(Long tenantId);

    Optional<ServiceProvider> getServiceProviderById(Long serviceProviderId);

    ServiceProvider updateServiceProvider(Long serviceProviderId, ServiceProvider serviceProvider);

    boolean deleteServiceProvider(Long serviceProviderId);
}

