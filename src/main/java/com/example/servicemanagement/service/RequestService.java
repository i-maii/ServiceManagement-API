package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.*;
import com.example.servicemanagement.entity.*;
import com.example.servicemanagement.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class RequestService {

    private static Logger logger = LoggerFactory.getLogger(RequestService.class);

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

    @Autowired
    PushNotificationService pushNotificationService;

    public Request getRequestById(Integer id) {
        return this.requestRepository.findRequestById(id);
    }

    public List<Request> getRequestByStatus(String status) {
        return this.requestRepository.findRequestsByStatus(status);
    }

    public List<Request> getRequestByStatusList(List<String> statusList) {
        return this.requestRepository.findRequestsByStatusInOrderByEstimateTimeAscEstimateTechnicianAsc(statusList);
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
                estimateRequest.setRequestId(request.getId());

                Tenant tenant = this.tenantService.getTenantByUserId(request.getUser().getId());
                if (tenant != null) {
                    estimateRequest.setRoomNo(tenant.getRoomNo());
                }

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

    public List<EstimateDto> getAllEstimateRequestV2() {
        List<Request> requestList = getRequestByStatusList(Arrays.asList(STATUS_READY_FOR_ESTIMATION, STATUS_READY_FOR_PLAN));
        List<Apartment> apartments = requestList.stream().map(Request::getApartment).distinct().toList();

        List<EstimateDto> estimateList = new ArrayList<>();
        for (Apartment apartment: apartments) {
            EstimateDto estimate = new EstimateDto();
            estimate.setApartmentName(apartment.getName());

            List<EstimateRequestDto> estimateRequestList = new ArrayList<>();
            for (Request request: requestList.stream().filter(request -> request.getApartment().equals(apartment)).toList()) {
                EstimateRequestDto estimateRequest = new EstimateRequestDto();
                estimateRequest.setRequestId(request.getId());

                Tenant tenant = this.tenantService.getTenantByUserId(request.getUser().getId());
                if (tenant != null) {
                    estimateRequest.setRoomNo(tenant.getRoomNo());
                }

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
        logger.info("---- เปลี่ยนลำดับงานปัจจุบันที่มีความสำคัญจากลำดับที่ 3 เป็นลำดับที่ 4 ----");
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
                logger.info("เลขที่แจ้งซ่อม: {}, วันที่แจ้งซ่อม: {}, เปลี่ยนลำดับความสำคัญจากลำดับที่ 3 เป็นลำดับที่ 4", technicianPlanDto.getRequestId(), request.getRequestDate());
                technicianPlanDto.setPriority(4);
            } else {
                logger.info("เลขที่แจ้งซ่อม: {}, วันที่แจ้งซ่อม: {}, ลำดับความสำคัญ: {}", technicianPlanDto.getRequestId(), request.getRequestDate(), technicianPlanDto.getPriority());
            }

            technicianPlanDtoList.add(technicianPlanDto);
        });

        logger.info("----------------------------------------------------------\n");

        return technicianPlanDtoList;
    }

    public List<TechnicianPlanDto> requestListToTechnicianPlan(List<Request> requestList) {
        logger.info("---- รายการงานซ่อมทั้งหมดที่จะนำมาหาแผนงานให้กับช่าง ----");
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
            logger.info("เลขที่แจ้งซ่อม: {}, หอ: {}, งานซ่อม: {}, ลำดับความสำคัญ: {}, วันที่แจ้งซ่อม: {}, เวลาที่ใช้: {}, จำนวนช่างที่ใช้: {}"
                    , technicianPlanDto.getRequestId()
                    , technicianPlanDto.getApartment().getName()
                    , technicianPlanDto.getRequest().getRequestType().getName()
                    , technicianPlanDto.getPriority()
                    , technicianPlanDto.getRequest().getRequestDate()
                    , technicianPlanDto.getEstimateTime()
                    , technicianPlanDto.getRequest().getEstimateTechnician());
        });

        logger.info("-------------------------------------------------\n");

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
        Date date = new Date();
//        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2022-08-24");
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
        RequestType requestType = this.requestTypeService.getRequestTypeById(dto.getRequestTypeId());
        if (requestType.isCommonArea()) {
            boolean isDup = this.requestRepository.checkCreateDuplicate(dto.getApartmentId(), dto.getRequestTypeId());

            if (isDup) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_INSERT_DUPLICATE_REQUEST);
            }
        }

        Request request = new Request();
        request.setRequestType(requestType);
        request.setUser(this.userService.getById(dto.getUserId()));
        Integer apartmentId = dto.getApartmentId();
        String roomNo = "";
        Apartment apartment;
        if (apartmentId == null) {
            Tenant tenant = this.tenantService.getTenantByUserId(dto.getUserId());
            apartment = tenant.getApartment();
            roomNo = tenant.getRoomNo();
        } else {
            apartment = this.apartmentService.getApartmentById(apartmentId);
        }
        request.setApartment(apartment);
        request.setDetail(dto.getDetail());
        request.setPriority(request.getRequestType().getPriority());
        request.setRequestDate(new Date());
        request.setEstimateTime(0);
        request.setEstimateTechnician(0);

        if (request.getRequestType().getRole().getName().equals("technician")) {
            request.setStatus(STATUS_READY_FOR_ESTIMATION);
            sendEstimationNotification(apartment.getName(), roomNo, request.getRequestType().getName());
        } else {
            request.setStatus(STATUS_READY_TO_SERVICE);
            sendServiceOtherNotification(apartment.getName(), roomNo, request.getRequestType());
        }

        this.requestRepository.saveAndFlush(request);
    }

    private void sendEstimationNotification(String apartmentName, String roomNo, String requestType) {
        List<User> users = this.userService.getUserByRoleNames(Arrays.asList("admin", "owner"));

        for (User user: users) {
            this.pushNotificationService.sendEstimationPushNotification(user.getNotificationToken(), apartmentName, roomNo, requestType);
        }
    }

    private void sendServiceOtherNotification(String apartmentName, String roomNo, RequestType requestType) {
        List<User> users = this.userService.getUserByRoleId(requestType.getRole().getId());

        for (User user: users) {
            this.pushNotificationService.sendServiceOtherPushNotification(user.getNotificationToken(), apartmentName, roomNo, requestType.getName());
        }
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
        request.setStatus(STATUS_READY_FOR_PLAN);

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
                requestItemDto.setRequestId(request.getId());
                Tenant tenant = this.tenantService.getTenantByUserId(request.getUser().getId());
                if (tenant != null) {
                    requestItemDto.setRoomNo(tenant.getRoomNo());
                }
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

    public void updateServiceDate() {
        List<Request> requests = this.requestRepository.findRequestsByStatus(STATUS_READY_TO_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        for (Request request: requests) {
            request.setServiceDate(calendar.getTime());
            this.requestRepository.saveAndFlush(request);
        }
    }
}
