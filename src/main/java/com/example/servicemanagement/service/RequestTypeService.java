package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.RequestType;
import com.example.servicemanagement.repository.RequestTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.ERR_INSERT_DUPLICATE_REQUEST_TYPE;
import static com.example.servicemanagement.constant.Constant.ERR_UPDATE_INVALID_REQUEST_TYPE;

@Service
public class RequestTypeService {
    @Autowired
    RequestTypeRepository requestTypeRepository;

    public List<RequestType> getAll() {
        return this.requestTypeRepository.findAll();
    }

    public RequestType getRequestTypeById(Integer id) {
        return this.requestTypeRepository.findRequestTypeById(id);
    }

    public List<RequestType> getRequestTypeForTechnician() {
        return this.requestTypeRepository.findRequestTypeByRoleName("technician");
    }

    public List<RequestType> getRequestTypeByRole(String role) {
        return this.requestTypeRepository.findRequestTypeByRoleName(role);
    }

    public List<Integer> getAllPriority() {
        List<RequestType> requestTypes = this.getRequestTypeByRole("technician");
        return requestTypes.stream().map(RequestType::getPriority).distinct().toList();
    }

    public void create(RequestType body) {
        boolean isDup = this.requestTypeRepository.checkCreateDuplicate(body.getName());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_INSERT_DUPLICATE_REQUEST_TYPE);
        }

        this.requestTypeRepository.saveAndFlush(body);
    }

    public void update(Integer id, RequestType body) {
        boolean isDup = this.requestTypeRepository.checkUpdateDuplicate(body.getId(), body.getName());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_UPDATE_INVALID_REQUEST_TYPE);
        }

        RequestType requestType = this.requestTypeRepository.findRequestTypeById(id);
        requestType.setName(body.getName());
        requestType.setPriority(body.getPriority());
        requestType.setRole(body.getRole());

        this.requestTypeRepository.saveAndFlush(requestType);
    }

    public void delete(Integer id) {
        this.requestTypeRepository.deleteById(id);
    }
}
