package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class RequestService {

    public List<TechnicianPlanDto> reorderPriority(List<Request> requestList) throws ParseException {

        Date[] dateRange = this.getLastWeekRange();
        Date start = dateRange[0];
        Date end = dateRange[1];

        boolean haveOlderRequest = checkOlderRequest(requestList, start, end);

        List<TechnicianPlanDto> technicianPlanDtoList = new ArrayList<>();
        if (haveOlderRequest) {
            requestList.forEach(request -> {
                TechnicianPlanDto technicianPlanDto = new TechnicianPlanDto();
                technicianPlanDto.setRequestId(request.getId());
                technicianPlanDto.setApartmentId(request.getTenant().getApartment().getId());
                technicianPlanDto.setTenantId(request.getTenant().getId());
                technicianPlanDto.setRequestTypeId(request.getRequestType().getId());
                technicianPlanDto.setEstimateTime(request.getEstimateTime());
                technicianPlanDto.setPriority(request.getRequestType().getPriority());

                if (request.getRequestType().getPriority() == 4 && !(request.getRequestDate().after(start) && request.getRequestDate().before(end))) {
                    technicianPlanDto.setPriority(5);
                }

                technicianPlanDtoList.add(technicianPlanDto);
            });
        }

        return technicianPlanDtoList;
    }

    private boolean checkOlderRequest(List<Request> requestList, Date start, Date end) {
        return requestList.stream().anyMatch(request -> (request.getRequestDate().after(start) && request.getRequestDate().before(end)));
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
            return Arrays.asList(1, 2, 3, 4);
        }

        return Arrays.asList(1, 2, 3);
    }
}
