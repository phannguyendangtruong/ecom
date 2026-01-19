package org.ecom.service;

import org.ecom.model.Role;
import org.ecom.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleService extends BaseServiceImpl<Role, Long>{
    private final RoleRepository roleRepository;
    protected RoleService(RoleRepository roleRepository) {
        super(roleRepository);
        this.roleRepository = roleRepository;
    }
}
