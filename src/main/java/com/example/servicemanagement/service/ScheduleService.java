package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.entity.Technician;
import com.example.servicemanagement.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Service
public class ScheduleService {

    private int totalApartment = 6;
    private boolean keepworking = true;

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    RequestService requestService;

    @Autowired
    TechnicianService technicianService;

    public List<List<TechnicianPlanDto>> findRequestWithSpecificHour(int targetHour) throws ParseException {
        List<Request> serviceRequestList = this.requestRepository.findRequestsByStatus("ready to service");
        List<TechnicianPlanDto> technicianPlanDtoList = this.requestService.reorderPriority(serviceRequestList);

        boolean isLowestTechnicianAvailable = this.technicianService.checkLowestAbilitiesTechnicianAvailable();

//        List<Integer> priority = this.requestService.findPriority(serviceRequestList);
        List<Technician> availableTechnician = this.technicianService.getAvailableTechnician();
        Integer[] range = findRangePriority(technicianPlanDtoList, availableTechnician.size());

        if (isLowestTechnicianAvailable) {
            List<List<TechnicianPlanDto>> lowestRequest = findRequestForLowestTechnician(technicianPlanDtoList, range);
            return lowestRequest;
        }

        List<List<TechnicianPlanDto>> res = new ArrayList<>();
        List<TechnicianPlanDto> ds = new ArrayList<>();
        findPossibleRequest(res, ds, targetHour, technicianPlanDtoList, 0, range);

        return res;
    }

    private void findPossibleRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, Integer[] rangePriority){
        List<Integer> priority = Arrays.asList(1, 2, 3);

        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);
            int numOfPriority = this.findNumberOfPriority(ds, priority);

            if (numOfPriority >= rangePriority[0] && numOfPriority <= rangePriority[1]) {
                if (numOfApartment < totalApartment) {
                    totalApartment = numOfApartment;

                    if (res != null) {
                        res.clear();
                    }

                    res.add(new ArrayList<>(ds));
                } else if (numOfApartment == totalApartment) {
                    res.add(new ArrayList<>(ds));
                }
            }
            return;
        }

        for (int i= index ; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1, rangePriority);
            ds.remove(ds.size()-1 );
        }
    }

    private int findNumberOfApartment(List<TechnicianPlanDto> technicianPlanDtoList) {
        int cnt = 0;
        int temp = 0;

        List<TechnicianPlanDto> planDtos = technicianPlanDtoList.stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getApartmentId)).toList();

        for (TechnicianPlanDto planDto : planDtos) {
            if (planDto.getApartmentId() != temp) {
                temp = planDto.getApartmentId();
                cnt++;
            }
        }

        return cnt;
    }

    private int findNumberOfPriority(List<TechnicianPlanDto> technicianPlanDtoList, List<Integer> priority) {
        int cnt = 0;

        for (TechnicianPlanDto planDto : technicianPlanDtoList) {
            if (priority.contains(planDto.getPriority())) {
                cnt = cnt + planDto.getEstimateTime();
            }
        }

        return cnt;
    }

    private Integer[] findRangePriority(List<TechnicianPlanDto> technicianPlanDtoList, int numberOfTechnician) {
        List<Integer> priority = Arrays.asList(1, 2, 3);

        int cnt = technicianPlanDtoList.stream().filter(technicianPlanDto -> priority.contains(technicianPlanDto.getPriority())).map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int start = cnt/numberOfTechnician;

        return new Integer[]{start, 8};
    }

    private List<List<TechnicianPlanDto>> findRequestForLowestTechnician(List<TechnicianPlanDto> technicianPlanDtoList, Integer[] rangePriority) {
        List<Integer> requestTypeList = this.technicianService.getAllRequestTypeOfLowestTechnician();

        List<TechnicianPlanDto> lowestTechnicianRequest = technicianPlanDtoList.stream().filter(technicianPlanDto -> requestTypeList.contains(technicianPlanDto.getRequestTypeId())).toList();

        List<List<TechnicianPlanDto>> res = new ArrayList<>();
        List<TechnicianPlanDto> ds = new ArrayList<>();
        findPossibleRequest(res, ds, 8, lowestTechnicianRequest, 0, rangePriority);

        if (res.size() > 1) {
            for (List<TechnicianPlanDto> technicianPlanList: res) {
                List<Integer> requestIds = technicianPlanList.stream().map(TechnicianPlanDto::getRequestId).toList();
                List<TechnicianPlanDto> remainingRequest = technicianPlanDtoList.stream().filter(Predicate.not(technicianPlanDto -> requestIds.contains(technicianPlanDto.getRequestId()))).toList();

                List<TechnicianPlanDto> r = new ArrayList<>();
                findPossibleRemainingRequest(r, 16, remainingRequest, 0);
            }
        }

        return res;
    }

    private void findPossibleRemainingRequest(List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index) {
        if (!keepworking) {
            return;
        }
        
        if (target == 0) {
            keepworking = false;
            return;
        }
        
        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;
            
            ds.add(arr.get(i));
            findPossibleRemainingRequest(ds, target-arr.get(i).getEstimateTime() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }
}
