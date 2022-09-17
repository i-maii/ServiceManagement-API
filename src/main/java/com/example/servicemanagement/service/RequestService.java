package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.entity.RequestType;
import com.example.servicemanagement.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class RequestService {

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    TechnicianService technicianService;

    @Autowired
    RequestTypeService requestTypeService;

    @Autowired
    TenantService tenantService;

    public Request getRequestById(Integer id) {
        return this.requestRepository.findRequestById(id);
    }

    public List<Request> getRequestByStatus(String status) {
        return this.requestRepository.findRequestsByStatus(status);
    }

    public List<Request> getAllRequestForPlanning() {
        List<RequestType> requestTypes = this.requestTypeService.getRequestTypeForTechnician();
        return this.requestRepository.findRequestsByStatusAndRequestTypeIn(STATUS_READY_FOR_PLAN, requestTypes);
    }

    public Integer getTotalRequestHour(List<Request> allRequest, boolean require2Technician) {
        int sum = 0;

        if (require2Technician) {
            sum += allRequest.stream().filter(req -> req.getEstimateTechnician() > 1).map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();
        }
        sum += allRequest.stream().map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();

        return sum;
    }

    public Integer getLowestTotalRequestHour() {
        List<Request> lowestRequest = getLowestRequest();

        return lowestRequest.stream().map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();
    }

    public Integer getLowestTotalHour(List<TechnicianPlanDto> allRequest) {
        List<Integer> requestTypeList = this.technicianService.getAllRequestTypeOfLowestTechnician();
        List<TechnicianPlanDto> lowestRequest = allRequest.stream().filter(req -> requestTypeList.contains(req.getRequestTypeId())).toList();

        return lowestRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
    }

    public Integer getTotalPriorityHour(List<Request> allRequest, boolean require2Technician) {
        int sum = 0;

        if (require2Technician) {
            sum += allRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getRequestType().getPriority()) && req.getEstimateTechnician() > 1).map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();
        }
        sum += allRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getRequestType().getPriority())).map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();

        return sum;
    }

    public Integer getLowestTotalPriorityHour() {
        List<Request> allRequest = getLowestRequest();
        List<Integer> priorityRequestType = this.technicianService.getPriorityRequestTypeOfLowestTechnician();

        return allRequest.stream().filter(req -> priorityRequestType.contains(req.getRequestType().getId())).map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();
    }

    public List<TechnicianPlanDto> getLowestRequest(List<TechnicianPlanDto> allRequest) {
        List<Integer> requestTypeList = this.technicianService.getAllRequestTypeOfLowestTechnician();

        return allRequest.stream().filter(req -> requestTypeList.contains(req.getRequestTypeId()) && req.getRequest().getEstimateTechnician() == 1).toList();
    }

    public List<Request> getLowestRequest() {
        List<Integer> requestTypeList = this.technicianService.getAllRequestTypeOfLowestTechnician();
        List<Request> allRequest = getRequestByStatus(STATUS_READY_FOR_PLAN);

        return allRequest.stream().filter(req -> requestTypeList.contains(req.getRequestType().getId()) && req.getEstimateTechnician() == 1).toList();
    }

    public void updateRequestStatusReadyToService(Request request) {
        request.setStatus(STATUS_READY_TO_SERVICE);
        this.requestRepository.saveAndFlush(request);
    }

    public List<TechnicianPlanDto> reorderPriority(List<Request> requestList) throws ParseException {
        Date[] dateRange = this.getLastWeekRange();
        Date start = dateRange[0];
        Date end = dateRange[1];

        List<TechnicianPlanDto> technicianPlanDtoList = new ArrayList<>();
        requestList.forEach(request -> {
            TechnicianPlanDto technicianPlanDto = new TechnicianPlanDto();
            technicianPlanDto.setRequestId(request.getId());
            technicianPlanDto.setApartmentId(request.getTenant().getApartment().getId());
            technicianPlanDto.setTenantId(request.getTenant().getId());
            technicianPlanDto.setRequestTypeId(request.getRequestType().getId());
            technicianPlanDto.setEstimateTime(request.getEstimateTime());
            technicianPlanDto.setPriority(request.getRequestType().getPriority());
            technicianPlanDto.setRequest(request);
            technicianPlanDto.setApartment(request.getTenant().getApartment());

            if (request.getRequestType().getPriority() == 4 && !(request.getRequestDate().after(start) && request.getRequestDate().before(end))) {
                technicianPlanDto.setPriority(5);
            }

            technicianPlanDtoList.add(technicianPlanDto);
        });

        return technicianPlanDtoList;
    }

    public List<TechnicianPlanDto> requestListToTechnicianPlan(List<Request> requestList) {
        List<TechnicianPlanDto> technicianPlanDtoList = new ArrayList<>();
        requestList.forEach(request -> {
            TechnicianPlanDto technicianPlanDto = new TechnicianPlanDto();
            technicianPlanDto.setRequestId(request.getId());
            technicianPlanDto.setApartmentId(request.getTenant().getApartment().getId());
            technicianPlanDto.setTenantId(request.getTenant().getId());
            technicianPlanDto.setRequestTypeId(request.getRequestType().getId());
            technicianPlanDto.setEstimateTime(request.getEstimateTime());
            technicianPlanDto.setPriority(request.getRequestType().getPriority());
            technicianPlanDto.setRequest(request);
            technicianPlanDto.setApartment(request.getTenant().getApartment());
            technicianPlanDtoList.add(technicianPlanDto);
        });

        return technicianPlanDtoList;
    }

    private boolean checkOlderRequest(List<Request> requestList, Date start, Date end) {
        return requestList.stream().anyMatch(request -> (request.getRequestDate().after(start) && request.getRequestDate().before(end)));
    }

    public boolean checkOlderRequest(List<Request> requestList) throws ParseException {
        Date[] dateRange = this.getLastWeekRange();
        Date start = dateRange[0];
        Date end = dateRange[1];

        return requestList.stream().anyMatch(request -> (request.getRequestDate().after(start) && request.getRequestDate().before(end)));
    }

    public boolean checkRequire2Technician(List<Request> allRequest) {
        return allRequest.stream().anyMatch(req -> req.getEstimateTechnician() > 1);
    }

    private Date[] getLastWeekRange() throws ParseException {
//        Date date = new Date();
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2022-08-24");
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int i = c.get(Calendar.DAY_OF_WEEK) - c.getFirstDayOfWeek();
        c.add(Calendar.DATE, -i - 7);
        Date start = c.getTime();
        c.add(Calendar.DATE, 6);
        c.set(Calendar.MILLISECOND, 999);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.HOUR_OF_DAY, 23);
        Date end = c.getTime();

        return new Date[]{start, end};
    }

    public List<Integer> findPriority(List<Request> requestList) throws ParseException {
        Date[] dates = getLastWeekRange();
        boolean haveOlderRequest = checkOlderRequest(requestList, dates[0], dates[1]);

        if (haveOlderRequest) {
            return Arrays.asList(1, 2, 3);
        }

        return Arrays.asList(1, 2);
    }

    public void createRequest(Request request) {
        request.setStatus(STATUS_READY_FOR_ESTIMATION);

        this.requestRepository.saveAndFlush(request);
    }

    public List<Request> getRequestListByTenantId(Integer id) {
        return this.requestRepository.findRequestsByTenantId(id);
    }

    public void updateRequest(Integer id, Request request) {
        request.setId(id);
        this.requestRepository.saveAndFlush(request);
    }
}
