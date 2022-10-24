package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.*;
import com.example.servicemanagement.entity.*;
import com.example.servicemanagement.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    UserService userService;

    @Autowired
    ApartmentService apartmentService;

    public Request getRequestById(Integer id) {
        return this.requestRepository.findRequestById(id);
    }

    public List<Request> getRequestByStatus(String status) {
        return this.requestRepository.findRequestsByStatus(status);
    }

    public List<Request> getRequestByStatusList(List<String> statusList) {
        return this.requestRepository.findRequestsByStatusIn(statusList);
    }

    public List<EstimateDto> getAllEstimateRequest() {
        List<Request> requestList = getRequestByStatusList(Arrays.asList(STATUS_READY_FOR_ESTIMATION, STATUS_READY_FOR_PLAN));
        Map<Apartment, List<Request>> r = requestList.stream().collect(Collectors.groupingBy(Request::getApartment));

        List<EstimateDto> estimateList = new ArrayList<>();
        for (Apartment apartment: r.keySet()) {
            EstimateDto estimate = new EstimateDto();
            estimate.setApartmentName(apartment.getName());

            List<EstimateRequestDto> estimateRequestList = new ArrayList<>();
            for (Request request: r.get(apartment)) {
                EstimateRequestDto estimateRequest = new EstimateRequestDto();
                Tenant tenant = this.tenantService.getTenantByUserId(request.getUser().getId());
                estimateRequest.setRequestId(request.getId());
                estimateRequest.setRoomNo(tenant.getRoomNo());
                estimateRequest.setRequestType(request.getRequestType().getName());
                estimateRequest.setPriority(request.getPriority());
                estimateRequest.setEstimateTechnician(request.getEstimateTechnician());
                estimateRequest.setEstimateTime(request.getEstimateTime());
                estimateRequestList.add(estimateRequest);
            }

            estimate.setRequestList(estimateRequestList);
            estimateList.add(estimate);
        }

        return estimateList;
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
            sum += allRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority()) && req.getEstimateTechnician() > 1).map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();
        }
        sum += allRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).map(Request::getEstimateTime).mapToInt(Integer::intValue).sum();

        return sum;
    }

    public Integer getLowestTotalPriorityHour() {
        List<Request> allRequest = getLowestRequest();
        List<Integer> priorityRequestType = Arrays.asList(1, 2, 3);

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
            technicianPlanDto.setApartmentId(request.getApartment().getId());
            technicianPlanDto.setUserId(request.getUser().getId());
            technicianPlanDto.setRequestTypeId(request.getRequestType().getId());
            technicianPlanDto.setEstimateTime(request.getEstimateTime());
            technicianPlanDto.setPriority(request.getPriority());
            technicianPlanDto.setRequest(request);
            technicianPlanDto.setApartment(request.getApartment());

            if (request.getPriority() == 3 && !(request.getRequestDate().after(start) && request.getRequestDate().before(end))) {
                technicianPlanDto.setPriority(4);
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
            technicianPlanDto.setApartmentId(request.getApartment().getId());
            technicianPlanDto.setUserId(request.getUser().getId());
            technicianPlanDto.setRequestTypeId(request.getRequestType().getId());
            technicianPlanDto.setEstimateTime(request.getEstimateTime());
            technicianPlanDto.setPriority(request.getPriority());
            technicianPlanDto.setRequest(request);
            technicianPlanDto.setApartment(request.getApartment());
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

    public void createRequest(RequestDto dto) {
        Request request = new Request();
        request.setRequestType(this.requestTypeService.getRequestTypeById(dto.getRequestTypeId()));
        request.setUser(this.userService.getById(dto.getUserId()));
        Integer apartmentId = dto.getApartmentId();
        Apartment apartment;
        if (apartmentId == null) {
            apartment = this.tenantService.getTenantByUserId(dto.getUserId()).getApartment();
        } else {
            apartment = this.apartmentService.getApartmentById(apartmentId);
        }
        request.setApartment(apartment);
        request.setName(dto.getName());
        request.setPhoneNo(dto.getPhoneNo());
        request.setDetail(dto.getDetail());
        request.setPriority(request.getRequestType().getPriority());
        request.setRequestDate(new Date());
        request.setEstimateTime(0);
        request.setEstimateTechnician(0);

        if (request.getRequestType().getRole().getName().equals("technician")) {
            request.setStatus(STATUS_READY_FOR_ESTIMATION);
        } else {
            request.setStatus(STATUS_READY_TO_SERVICE);
        }

        this.requestRepository.saveAndFlush(request);
    }

    public List<Request> getRequestListByUserId(Integer userId) {
        return this.requestRepository.findRequestsByUserIdOrderByRequestDateDesc(userId);
    }

    public void updateRequest(Integer id, Request request) {
        request.setId(id);
        this.requestRepository.saveAndFlush(request);
    }

    public EstimateValueDto getEstimateValue() {
        List<RequestType> requestTypes = this.requestTypeService.getRequestTypeByRole("technician");
        List<Integer> priority = requestTypes.stream().map(RequestType::getPriority).distinct().toList();
        List<Integer> time = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        List<Technician> technicians = this.technicianService.getAllTechnician();
        List<Integer> technician = new ArrayList<>();
        for (int i=1; i<=technicians.size(); i++) {
            technician.add(i);
        }

        EstimateValueDto estimateValueDto = new EstimateValueDto();
        estimateValueDto.setPriority(priority);
        estimateValueDto.setTime(time);
        estimateValueDto.setTechnician(technician);

        return estimateValueDto;
    }

    public void updateEstimate(Integer requestId, UpdateEstimateValueDto dto) {
        Request request = this.getRequestById(requestId);
        request.setEstimateTechnician(dto.getTechnician());
        request.setEstimateTime(dto.getTime());
        request.setPriority(dto.getPriority());

        this.requestRepository.saveAndFlush(request);
    }

    public List<RequestListDto> getRequestList(String role) {
        List<RequestType> requestTypes = this.requestTypeService.getRequestTypeByRole(role);
        List<Request> requestList = this.requestRepository.findRequestsByStatusAndRequestTypeIn(STATUS_READY_TO_SERVICE, requestTypes);
        Map<Apartment, List<Request>> r = requestList.stream().collect(Collectors.groupingBy(Request::getApartment));

        List<RequestListDto> requestListDtos = new ArrayList<>();
        for (Apartment apartment: r.keySet()) {
            RequestListDto requestListDto = new RequestListDto();
            requestListDto.setApartmentName(apartment.getName());

            List<RequestItemDto> requestItemDtos = new ArrayList<>();
            for (Request request: r.get(apartment)) {
                RequestItemDto requestItemDto = new RequestItemDto();
                Tenant tenant = this.tenantService.getTenantByUserId(request.getUser().getId());
                requestItemDto.setRequestId(request.getId());
                requestItemDto.setRoomNo(tenant.getRoomNo());
                requestItemDto.setRequestType(request.getRequestType().getName());
                requestItemDtos.add(requestItemDto);
            }

            requestListDto.setRequestList(requestItemDtos);
            requestListDtos.add(requestListDto);
        }

        return requestListDtos;
    }

    public void closeTask(Integer requestId) {
        Request request = this.requestRepository.findRequestById(requestId);
        request.setStatus(STATUS_DONE);

        this.requestRepository.saveAndFlush(request);
    }
}
