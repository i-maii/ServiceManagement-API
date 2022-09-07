package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.entity.Schedule;
import com.example.servicemanagement.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class ScheduleService {

    private int totalApartment = 6;
    private long totalDate = Long.MAX_VALUE;
    private boolean keepworking = true;
    private int bestRequestPlanId = 0;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    RequestService requestService;

    @Autowired
    TechnicianService technicianService;

    @Autowired
    ConfigService configService;

    @Autowired
    RouteService routeService;

    public void findRequestWithSpecificHour() throws ParseException {
        this.configService.findConfiguration();

        Integer[] range = this.configService.getRangePriorityHour();
        Integer[] lowestRange = this.configService.getRangeLowestPriorityHour();

        Integer usageTechnician = this.configService.getUsageTechnicianConfig();
        Integer[] targetHour = this.configService.getAllTargetHour();

        if (usageTechnician != 0) {
            List<Request> allRequest = this.requestService.getAllRequestForPlanning();
            boolean haveOlderRequest = this.requestService.checkOlderRequest(allRequest);

            List<TechnicianPlanDto> requestListForPlan;
            List<TechnicianPlanDto> priorityRequestList;
            if (haveOlderRequest) {
                requestListForPlan = this.requestService.reorderPriority(allRequest);
                priorityRequestList = requestListForPlan.stream().filter(req -> ALL_PRIORITY.contains(req.getPriority())).toList();
            } else {
                requestListForPlan = this.requestService.requestListToTechnicianPlan(allRequest);
                priorityRequestList = requestListForPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
            }

            if (usageTechnician == 3) { //ใช้ช่าง 3 คน
                findTechnicianPlanFor3Technician(requestListForPlan, lowestRange, targetHour);
            } else {
                int totalTargetHour = this.configService.getTotalTargetHour();
                int totalRequestHour = this.configService.getTotalRequestHour();
                int totalPriorityHour = this.configService.getTotalPriorityHour();
                List<TechnicianPlanDto> requestList = new ArrayList<>(requestListForPlan);

                List<List<TechnicianPlanDto>> possibleRequestList = new ArrayList<>();
                List<TechnicianPlanDto> possibleRequest = new ArrayList<>();
                if (totalPriorityHour >= totalTargetHour) {
                    keepworking = true;
                    findPossibleOneRequest(possibleRequestList, possibleRequest, totalTargetHour, priorityRequestList, 0);
                    requestList.clear();
                    requestList.addAll(possibleRequestList.get(0));
                } else if (totalRequestHour > totalTargetHour) {
                    List<TechnicianPlanDto> normalRequest = requestListForPlan.stream().filter(Predicate.not(priorityRequestList::contains)).toList();
                    List<Integer> apartmentIds = priorityRequestList.stream().map(TechnicianPlanDto::getApartmentId).distinct().toList();

                    totalApartment = normalRequest.size();
                    totalDate = Long.MAX_VALUE;
                    keepworking = true;

                    int priorityHour = priorityRequestList.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
                    int targetNormalHour = totalTargetHour - priorityHour;

                    List<TechnicianPlanDto> sortedNormalRequest = normalRequest.stream().sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
                    findPossibleRemainingRequest(possibleRequestList, possibleRequest, targetNormalHour, sortedNormalRequest, 0, apartmentIds);
                    requestList.clear();
                    requestList.addAll(priorityRequestList);
                    requestList.addAll(possibleRequestList.get(0));
                }

                if (usageTechnician == 1) {
                    List<List<TechnicianPlanDto>> planForTechnician1 = new ArrayList<>();
                    planForTechnician1.add(requestList);
                    saveTechnicianPlan(planForTechnician1);
                } else {
                    List<TechnicianPlanDto> sortedRequestList = requestList.stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

                    totalApartment = 6;
                    List<List<TechnicianPlanDto>> possiblePlanListForTechnician1 = new ArrayList<>();
                    List<TechnicianPlanDto> possiblePlanForTechnician1 = new ArrayList<>();
                    findPossibleRequest(possiblePlanListForTechnician1, possiblePlanForTechnician1, targetHour[0], sortedRequestList, 0, range);

                    if (possiblePlanListForTechnician1.size() > 1) {
                        List<List<TechnicianPlanDto>> possiblePlanListForTechnician2 = new ArrayList<>();
                        List<List<TechnicianPlanDto>> tempPossiblePlanListForTechnician1 = new ArrayList<>();
                        totalApartment = 6;

                        for (List<TechnicianPlanDto> plan : possiblePlanListForTechnician1) {
                            List<TechnicianPlanDto> possiblePlanForTechnician2 = new ArrayList<>(requestList.stream().filter(Predicate.not(plan::contains)).toList());
                            int numOfApartment = this.findNumberOfApartment(possiblePlanForTechnician2);
                            if (numOfApartment < totalApartment) {
                                totalApartment = numOfApartment;
                                tempPossiblePlanListForTechnician1.clear();
                                possiblePlanListForTechnician2.clear();
                                tempPossiblePlanListForTechnician1.add(plan);
                                possiblePlanListForTechnician2.add(possiblePlanForTechnician2);
                            } else if (numOfApartment == totalApartment) {
                                tempPossiblePlanListForTechnician1.add(plan);
                                possiblePlanListForTechnician2.add(possiblePlanForTechnician2);
                            }
                        }
                        this.routeService.checkBestRoute(tempPossiblePlanListForTechnician1, possiblePlanListForTechnician2);

                        // TODO: save best plan
                    } else {
                        List<TechnicianPlanDto> planForTechnician2 = requestList.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
                        possiblePlanListForTechnician1.add(planForTechnician2);

                        saveTechnicianPlan(possiblePlanListForTechnician1);
                    }
                }
            }
        }
    }

    private void findTechnicianPlanFor3Technician(List<TechnicianPlanDto> allRequest, Integer[] lowestRangePriority, Integer[] targetHour) {
        List<TechnicianPlanDto> lowestTechnicianRequest = this.requestService.getLowestRequest(allRequest).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        totalApartment = 6;
        List<List<TechnicianPlanDto>> possibleLowestPlanList = new ArrayList<>();
        List<TechnicianPlanDto> possibleLowestPlan = new ArrayList<>();
        findPossibleRequest(possibleLowestPlanList, possibleLowestPlan, targetHour[2], lowestTechnicianRequest, 0, lowestRangePriority);

        List<List<TechnicianPlanDto>> otherRequestList = new ArrayList<>();
        if (possibleLowestPlanList.size() > 1) {
            for (List<TechnicianPlanDto> lowestPlan: possibleLowestPlanList) {
                List<TechnicianPlanDto> possibleOtherPlanList = findOtherRequestList(allRequest, lowestPlan, targetHour);
                otherRequestList.add(possibleOtherPlanList);
            }
        } else {
            List<TechnicianPlanDto> requestList = findOtherRequestList(allRequest, possibleLowestPlanList.get(0), targetHour);
            otherRequestList.add(requestList);
        }

        List<TechnicianPlanDto> bestRequest = checkBestRequest(otherRequestList).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        //save plan for lowestTechnician
        List<TechnicianPlanDto> lowestTechnicianPlan = possibleLowestPlanList.get(bestRequestPlanId);
        saveLowestTechnicianPlan(lowestTechnicianPlan);

        Integer[] rangePriority = this.configService.getRangePriorityHour();

        totalApartment = 6;
        List<List<TechnicianPlanDto>> possiblePlanListForTechnician1 = new ArrayList<>();
        List<TechnicianPlanDto> possiblePlan = new ArrayList<>();
        findPossibleRequest(possiblePlanListForTechnician1, possiblePlan, targetHour[0], bestRequest, 0, rangePriority);

        if (possiblePlan.size() > 1) {
            List<List<TechnicianPlanDto>> possiblePlanListForTechnician2 = new ArrayList<>();
            for (List<TechnicianPlanDto> plan: possiblePlanListForTechnician1) {
                List<TechnicianPlanDto> possiblePlanForTechnician2 = new ArrayList<>(bestRequest.stream().filter(Predicate.not(plan::contains)).toList());
                possiblePlanListForTechnician2.add(possiblePlanForTechnician2);
            }
            this.routeService.checkBestRoute(possiblePlanListForTechnician1, possiblePlanListForTechnician2);
        } else {
            List<TechnicianPlanDto> planForTechnician2 = bestRequest.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
            possiblePlanListForTechnician1.add(planForTechnician2);
            saveTechnicianPlan(possiblePlanListForTechnician1);
        }
    }

    private void saveTechnicianPlan(List<List<TechnicianPlanDto>> technicianPlanList) {
        int index = 1;
        for (List<TechnicianPlanDto> planList: technicianPlanList) {
            for (TechnicianPlanDto plan: planList) {
                Schedule schedule = new Schedule();
                schedule.setRequest(plan.getRequest());
                schedule.setApartment(plan.getApartment());
                schedule.setTechnician(this.technicianService.getTechnicianById(index));
                this.scheduleRepository.saveAndFlush(schedule);

                this.requestService.updateRequestStatusReadyToService(plan.getRequest());
            }
            index++;
        }
    }

    private void saveLowestTechnicianPlan(List<TechnicianPlanDto> lowestTechnicianPlan) {
        for (TechnicianPlanDto planDto: lowestTechnicianPlan) {
            Schedule schedule = new Schedule();
            schedule.setRequest(planDto.getRequest());
            schedule.setApartment(planDto.getApartment());
            schedule.setTechnician(this.technicianService.getLowestTechnician());
            this.scheduleRepository.saveAndFlush(schedule);

            this.requestService.updateRequestStatusReadyToService(planDto.getRequest());
        }
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

    private void findPossibleRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, Integer[] rangePriority){
        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);
            int numOfPriority = this.findNumberOfPriority(ds, MOST_PRIORITY);

            if (numOfPriority >= rangePriority[0] && numOfPriority <= rangePriority[1]) {
                if (numOfApartment < totalApartment) {
                    totalApartment = numOfApartment;
                    res.clear();
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

    private void findPossibleRemainingRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, List<Integer> apartment) {
        if (!keepworking) {
            return;
        }

        if (target == 0) {
            int remainingNumberOfApartment = ds.stream().map(TechnicianPlanDto::getApartmentId).filter(Predicate.not(apartment::contains)).toList().size();
            long sumDate = ds.stream().map(d -> d.getRequest().getRequestDate()).mapToLong(Date::getTime).sum();

            if (remainingNumberOfApartment == 0) {
                res.clear();
                res.add(new ArrayList<>(ds));
                keepworking = false;
            } else if (remainingNumberOfApartment < totalApartment) {
                totalApartment = remainingNumberOfApartment;
                totalDate = sumDate;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (remainingNumberOfApartment == totalApartment && sumDate < totalDate) {
                totalDate = sumDate;
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
            ds.remove(ds.size() - 1);
        }
    }

    private void findPossibleOneRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index) {
        if (!keepworking) {
            return;
        }

        if (target == 0) {
            res.add(new ArrayList<>(ds));
            keepworking = false;
            return;
        }

        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleOneRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }

    private List<TechnicianPlanDto> findOtherRequestList(List<TechnicianPlanDto> allRequest, List<TechnicianPlanDto> lowestPlan, Integer[] targetHour) {
        List<TechnicianPlanDto> remainingRequest = allRequest.stream().filter(Predicate.not(lowestPlan::contains)).toList();
        List<TechnicianPlanDto> priorityRequest = remainingRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
        List<TechnicianPlanDto> normalRequest = remainingRequest.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();

        List<Integer> apartmentIds = findApartmentId(lowestPlan, priorityRequest);
        totalApartment = normalRequest.size();
        totalDate = Long.MAX_VALUE;
        keepworking = true;

        int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        this.configService.updatePriorityHour(priorityHour);
        int normalTargetHour = (targetHour[0]+targetHour[1]) - priorityHour;

        List<List<TechnicianPlanDto>> tempPossibleOtherPlanList = new ArrayList<>();
        List<TechnicianPlanDto> tempPossibleOtherPlan = new ArrayList<>();
        findPossibleRemainingRequest(tempPossibleOtherPlanList, tempPossibleOtherPlan, normalTargetHour, normalRequest, 0, apartmentIds);

        List<TechnicianPlanDto> possibleOtherPlanList = new ArrayList<>(priorityRequest);
        possibleOtherPlanList.addAll(tempPossibleOtherPlanList.get(0));

        return possibleOtherPlanList;
    }

    private List<Integer> findApartmentId(List<TechnicianPlanDto> planList, List<TechnicianPlanDto> priorityRequest) {
        List<TechnicianPlanDto> allRequest = new ArrayList<>();
        if (!planList.isEmpty()) {
            allRequest.addAll(planList);
            allRequest.addAll(priorityRequest);
        } else {
            allRequest.addAll(priorityRequest);
        }

        return allRequest.stream().map(TechnicianPlanDto::getApartmentId).distinct().toList();
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
}
