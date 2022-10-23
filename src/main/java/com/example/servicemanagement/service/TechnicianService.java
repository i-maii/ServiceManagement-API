package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.RequestType;
import com.example.servicemanagement.entity.Role;
import com.example.servicemanagement.entity.Technician;
import com.example.servicemanagement.entity.User;
import com.example.servicemanagement.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.servicemanagement.constant.Constant.ERR_INSERT_DUPLICATE_TECHNICIAN;
import static com.example.servicemanagement.constant.Constant.ERR_UPDATE_INVALID_TECHNICIAN;

@Service
public class TechnicianService {

    @Autowired
    TechnicianRepository technicianRepository;

    @Autowired
    RequestTypeService requestTypeService;

    @Autowired
    UserService userService;

    @Autowired
    RoleService roleService;

    public int getNumberOfAvailableTechnician() {
        return this.technicianRepository.countTechnicianByAvailable(true);
    }

    public List<Technician> getAvailableTechnician() {
        return this.technicianRepository.findTechniciansByAvailable(true);
    }

    public List<Technician> getAllTechnician() {
        return this.technicianRepository.findAll();
    }

    public Technician getTechnicianById(Integer id) {
        return this.technicianRepository.findTechnicianById(id);
    }

    public List<Technician> getTechnicianByIds(List<Integer> id) {
        return this.technicianRepository.findTechnicianByIdIn(id);
    }

    public Technician getLowestTechnician() {
        return this.technicianRepository.findLowestTechnician();
    }

    public boolean checkLowestAbilitiesTechnicianAvailable() {
        return this.technicianRepository.isLowestAbilitiesTechnicianAvailable();
    }

    public List<Integer> getAllRequestTypeOfLowestTechnician() {
        return this.technicianRepository.findRequestTypeOfLowestTechnician();
    }

    public List<Integer> getPriorityRequestTypeOfLowestTechnician() {
        return this.technicianRepository.findPriorityRequestTypeOfLowestTechnician();
    }

    public List<Technician> getTechnicianSchedule() {
        return this.technicianRepository.findTechnicianSchedule();
    }

    public Technician getTechnicianByUserId(Integer userId) {
        return this.technicianRepository.findTechnicianByUserId(userId);
    }

    public void create(Technician body) {
        boolean isDup = this.technicianRepository.checkCreateDuplicate(body.getUser().getUsername());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_INSERT_DUPLICATE_TECHNICIAN);
        }

        List<Integer> requestTypeIds = body.getRequestTypes().stream().map(RequestType::getId).toList();
        List<RequestType> requestTypeList = this.requestTypeService.getRequestTypeByIds(requestTypeIds);

        Technician technician = new Technician();
        Set<RequestType> requestTypes = new HashSet<>(requestTypeList);
        technician.setRequestTypes(requestTypes);
        Role role = this.roleService.getRoleByName("technician");
        User user = body.getUser();
        user.setRole(role);
        technician.setUser(this.userService.create(user));

        this.technicianRepository.saveAndFlush(technician);
    }

    public void update(Integer id, Technician body) {
        boolean isDup = this.technicianRepository.checkUpdateDuplicate(id, body.getUser().getUsername());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_UPDATE_INVALID_TECHNICIAN);
        }

        List<Integer> requestTypeIds = body.getRequestTypes().stream().map(RequestType::getId).toList();
        List<RequestType> requestTypeList = this.requestTypeService.getRequestTypeByIds(requestTypeIds);

        Technician technician = new Technician();
        technician.setId(body.getId());
        Set<RequestType> requestTypes = new HashSet<>(requestTypeList);
        technician.setRequestTypes(requestTypes);
        technician.setUser(body.getUser());

        this.technicianRepository.saveAndFlush(technician);
    }

    public void delete(Integer id) {
        Technician technician = this.technicianRepository.findTechnicianById(id);
        this.technicianRepository.deleteById(id);
        this.userService.delete(technician.getUser().getId());
    }
}
