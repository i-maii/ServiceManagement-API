package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.entity.Schedule;
import com.example.servicemanagement.entity.Technician;
import com.example.servicemanagement.repository.RequestRepository;
import com.example.servicemanagement.repository.ScheduleRepository;
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
    private int bestRequestPlanId = 0;

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    RequestService requestService;

    @Autowired
    TechnicianService technicianService;

    public List<TechnicianPlanDto> findRequestWithSpecificHour(int targetHour) throws ParseException {
        List<Request> serviceRequestList = this.requestRepository.findRequestsByStatus("ready to service");
        List<TechnicianPlanDto> technicianPlanDtoList = this.requestService.reorderPriority(serviceRequestList);

        boolean isLowestTechnicianAvailable = this.technicianService.checkLowestAbilitiesTechnicianAvailable();

//        List<Integer> priority = this.requestService.findPriority(serviceRequestList);
        List<Technician> availableTechnician = this.technicianService.getAvailableTechnician();
        Integer[] range = findRangePriority(technicianPlanDtoList, availableTechnician.size());

        if (isLowestTechnicianAvailable) {
            return findLowestTechnicianRequest(technicianPlanDtoList, range);
        }

        List<List<TechnicianPlanDto>> res = new ArrayList<>();
        List<TechnicianPlanDto> ds = new ArrayList<>();
        findPossibleRequest(res, ds, targetHour, technicianPlanDtoList, 0, range);

        return null;
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

    private List<TechnicianPlanDto> findLowestTechnicianRequest(List<TechnicianPlanDto> technicianPlanDtoList, Integer[] rangePriority) {
        List<Integer> requestTypeList = this.technicianService.getAllRequestTypeOfLowestTechnician();

        List<TechnicianPlanDto> lowestTechnicianRequest = technicianPlanDtoList.stream().filter(technicianPlanDto -> requestTypeList.contains(technicianPlanDto.getRequestTypeId())).toList();

        List<List<TechnicianPlanDto>> possiblePlanList = new ArrayList<>();
        List<TechnicianPlanDto> possiblePlan = new ArrayList<>();
        findPossibleRequest(possiblePlanList, possiblePlan, 8, lowestTechnicianRequest, 0, rangePriority);

        List<List<TechnicianPlanDto>> otherPlanList = new ArrayList<>();
        if (possiblePlanList.size() > 1) {
            for (List<TechnicianPlanDto> technicianPlanList: possiblePlanList) {
                List<TechnicianPlanDto> remainingRequest = technicianPlanDtoList.stream().filter(Predicate.not(technicianPlanList::contains)).toList();
                List<TechnicianPlanDto> priorityRequest = remainingRequest.stream().filter(technicianPlanDto -> Arrays.asList(1, 2, 3).contains(technicianPlanDto.getPriority())).toList();
                List<TechnicianPlanDto> normalRequest = remainingRequest.stream().filter(Predicate.not(priorityRequest::contains)).toList();

                List<Integer> apartmentIds = findApartmentId(technicianPlanList, priorityRequest);
                totalApartment = normalRequest.size();
                keepworking = true;

                int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

                List<List<TechnicianPlanDto>> possibleOtherPlanList = new ArrayList<>();
                List<TechnicianPlanDto> possibleOtherPlan = new ArrayList<>();
                findPossibleRemainingRequest(possibleOtherPlanList, possibleOtherPlan, 16 - priorityHour, normalRequest, 0, apartmentIds);

                List<TechnicianPlanDto> newPlan = new ArrayList<>(priorityRequest);
                newPlan.addAll(possibleOtherPlanList.get(0));
                otherPlanList.add(newPlan);
            }
        }

        List<TechnicianPlanDto> bestRequest = checkBestRequest(otherPlanList);

        //save plan for lowestTechnician
        List<TechnicianPlanDto> lowestTechnicianPlan = possiblePlanList.get(bestRequestPlanId);
        for (TechnicianPlanDto planDto: lowestTechnicianPlan) {
            Schedule schedule = new Schedule();
            schedule.setRequest(planDto.getRequest());
            schedule.setApartment(planDto.getApartment());
            schedule.setTechnician(this.technicianService.getLowestTechnician());
            this.scheduleRepository.saveAndFlush(schedule);
        }

        return bestRequest;
    }

    private List<TechnicianPlanDto> checkBestRequest(List<List<TechnicianPlanDto>> otherPlanList) {
        List<TechnicianPlanDto> bestRequest = new ArrayList<>();
        int maxNum = -1;
        for (List<TechnicianPlanDto> plan: otherPlanList) {
            int numberOfPriority4 = plan.stream().map(TechnicianPlanDto::getPriority).filter(priority -> priority == 4).toList().size();
            if (numberOfPriority4 > maxNum) {
                maxNum = numberOfPriority4;
                bestRequest.clear();
                bestRequest.addAll(plan);
                bestRequestPlanId = otherPlanList.indexOf(plan);
            }
        }

        return bestRequest;
    }

    private void findPossibleRemainingRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, List<Integer> apartment) {
        if (!keepworking) {
            return;
        }

        if (target == 0) {
            int remainingNumberOfApartment = ds.stream().map(TechnicianPlanDto::getApartmentId).filter(Predicate.not(apartment::contains)).toList().size();
            if (remainingNumberOfApartment == 0) {
                res.clear();
                res.add(new ArrayList<>(ds));
                keepworking = false;
            } else if (remainingNumberOfApartment < totalApartment) {
                totalApartment = remainingNumberOfApartment;
                res.clear();
                res.add(new ArrayList<>(ds));
            }
            return;
        }
        
        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;
            
            ds.add(arr.get(i));
            findPossibleRemainingRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1, apartment);
            ds.remove(ds.size()-1 );
        }
    }

    private List<Integer> findApartmentId(List<TechnicianPlanDto> planList, List<TechnicianPlanDto> priorityRequest) {
        List<TechnicianPlanDto> allRequest = new ArrayList<>(planList);
        allRequest.addAll(priorityRequest);

        return allRequest.stream().map(TechnicianPlanDto::getApartmentId).distinct().toList();
    }
}
