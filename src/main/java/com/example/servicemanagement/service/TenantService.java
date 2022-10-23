package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Role;
import com.example.servicemanagement.entity.Tenant;
import com.example.servicemanagement.entity.User;
import com.example.servicemanagement.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.ERR_INSERT_DUPLICATE_TENANT;
import static com.example.servicemanagement.constant.Constant.ERR_UPDATE_INVALID_TENANT;

@Service
public class TenantService {
    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    public Tenant getTenantById(Integer id) {
        return this.tenantRepository.findTenantById(id);
    }

    public Tenant getTenantByUserId(Integer userId) {
        return this.tenantRepository.findTenantByUserId(userId);
    }

    public List<Tenant> getAllByApartmentId(Integer apartmentId) {
        return this.tenantRepository.findTenantsByApartmentId(apartmentId);
    }

    public void update(Integer id, Tenant body) {
        boolean isDup = this.tenantRepository.checkUpdateDuplicate(body.getId(), body.getRoomNo(), body.getApartment().getId(), body.getUser().getUsername());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_UPDATE_INVALID_TENANT);
        }

        Tenant tenant = this.tenantRepository.findTenantById(id);
        tenant.setRoomNo(body.getRoomNo());
        User user = tenant.getUser();
        user.setName(body.getUser().getName());
        user.setPhoneNo(body.getUser().getPhoneNo());
        user.setPassword(body.getUser().getPassword());

        this.tenantRepository.saveAndFlush(tenant);
    }

    public void create(Tenant body) {
        boolean isDup = this.tenantRepository.checkCreateDuplicate(body.getRoomNo(), body.getApartment().getId(), body.getUser().getUsername());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_INSERT_DUPLICATE_TENANT);
        }

        Role role = this.roleService.getRoleByName("tenant");
        User user = body.getUser();
        user.setRole(role);
        body.setUser(this.userService.create(user));
        this.tenantRepository.saveAndFlush(body);
    }

    public void delete(Integer id) {
        Tenant tenant = this.tenantRepository.findTenantById(id);
        this.tenantRepository.deleteById(id);
        this.userService.delete(tenant.getUser().getId());
    }
}
