package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.*;
import com.example.servicemanagement.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.*;
import java.util.function.Predicate;

import static com.example.servicemanagement.constant.Constant.*;

@Service
@Transactional
public class ScheduleService {

    private int totalApartment = 6;
    private long totalDate = Long.MAX_VALUE;
    private int bestRequestPlanId = 0;
    private int totalPriority = Integer.MAX_VALUE;
    private boolean keepWorking = true;

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

    @Autowired
    ApartmentDistanceService apartmentDistanceService;

    @Autowired
    ApartmentService apartmentService;

    @Autowired
    StgScheduleService stgScheduleService;

    public void findRequestWithSpecificHour() throws ParseException {
        this.configService.findConfiguration();

        Integer[] rangePriorityHour = this.configService.getRangePriorityHour();
        Integer[] lowestRange = this.configService.getRangeLowestPriorityHour();

        Integer usageTechnician = this.configService.getUsageTechnicianConfig();
        Integer[] targetHour = this.configService.getAllTargetHour();

        if (usageTechnician != 0) {
            List<Request> allRequest = this.requestService.getAllRequestForPlanning();
            boolean haveOlderRequest = this.requestService.checkOlderRequest(allRequest);
            boolean isRequire2Technician = this.requestService.checkRequire2Technician(allRequest);

            List<TechnicianPlanDto> requestListForPlan = new ArrayList<>(this.requestService.requestListToTechnicianPlan(allRequest));
            List<TechnicianPlanDto> priorityRequestList = new ArrayList<>(requestListForPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList());
            if (haveOlderRequest) {
                requestListForPlan = this.requestService.reorderPriority(allRequest);
                priorityRequestList = requestListForPlan.stream().filter(req -> ALL_PRIORITY.contains(req.getPriority())).toList();
            }

            if (isRequire2Technician) {
                if (usageTechnician == 3) {
                    findRequire2TechnicianPlanFor3Technician(requestListForPlan, lowestRange, targetHour);
                } else {
                    List<TechnicianPlanDto> require2RequestList = findRequire2RequestList(targetHour, requestListForPlan);
                    findRequire2TechnicianPlanFor2Technician(require2RequestList, targetHour, 2, null);
                }
            } else {
                if (usageTechnician == 3) { //ใช้ช่าง 3 คน
                    findTechnicianPlanFor3Technician(requestListForPlan, lowestRange, targetHour);
                } else {
                    int totalTargetHour = this.configService.getTotalTargetHour();
                    int totalRequestHour = this.configService.getTotalRequestHour();
                    int totalPriorityHour = this.configService.getTotalPriorityHour();

                    List<TechnicianPlanDto> requestList = findRequestList(totalPriorityHour, totalTargetHour, totalRequestHour, requestListForPlan, priorityRequestList);

                    if (usageTechnician == 1) {
                        List<List<TechnicianPlanDto>> planForTechnician1 = new ArrayList<>();
                        planForTechnician1.add(requestList);
                        saveTechnicianPlan(planForTechnician1);
                    } else {
                        findTechnicianPlanFor2Technician(targetHour, rangePriorityHour, requestList, 2, null);
                    }
                }
            }

            boolean isAlreadyFindRoute = this.scheduleRepository.checkAlreadyFindRoute();
            if (!isAlreadyFindRoute) {
                findRoute();
            }
        }
    }

    private List<List<TechnicianPlanDto>> findTechnicianPlanForLowestTechnician(Integer[] targetHour, List<TechnicianPlanDto> allRequest, Integer[] lowestRangePriority) {
        List<TechnicianPlanDto> lowestTechnicianRequest = this.requestService.getLowestRequest(allRequest).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();
        int totalLowestRequestHour = this.configService.getTotalLowestRequestHour();

        List<List<TechnicianPlanDto>> possibleLowestPlanList = new ArrayList<>();
        if (totalLowestRequestHour > targetHour[2]) {
            totalApartment = 6;
            List<TechnicianPlanDto> possibleLowestPlan = new ArrayList<>();
            findPossibleRequest(possibleLowestPlanList, possibleLowestPlan, targetHour[2], lowestTechnicianRequest, 0, lowestRangePriority);
        } else {
            possibleLowestPlanList.add(lowestTechnicianRequest);
        }

        return possibleLowestPlanList;
    }

    private void findTechnicianPlanFor2Technician(Integer[] targetHour, Integer[] range, List<TechnicianPlanDto> requestList, int numOfTechnician, List<TechnicianPlanDto> technician3Plan) {
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

            if (tempPossiblePlanListForTechnician1.size() > 1) {
                checkBestRoute(tempPossiblePlanListForTechnician1, possiblePlanListForTechnician2, technician3Plan, false, numOfTechnician);
            } else {
                List<List<TechnicianPlanDto>> planList = new ArrayList<>();
                planList.addAll(tempPossiblePlanListForTechnician1);
                planList.addAll(possiblePlanListForTechnician2);
                saveTechnicianPlan(planList);
            }
        } else {
            List<TechnicianPlanDto> planForTechnician2 = requestList.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
            possiblePlanListForTechnician1.add(planForTechnician2);

            saveTechnicianPlan(possiblePlanListForTechnician1);
        }
    }

    private void findTechnicianPlanFor3Technician(List<TechnicianPlanDto> allRequest, Integer[] lowestRangePriority, Integer[] targetHour) {
        List<List<TechnicianPlanDto>> possibleLowestPlanList = findTechnicianPlanForLowestTechnician(targetHour, allRequest, lowestRangePriority);

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

        findTechnicianPlanFor2Technician(targetHour, rangePriority, bestRequest, 3, lowestTechnicianPlan);
    }

    private void findRequire2TechnicianPlanFor2Technician(List<TechnicianPlanDto> allRequest, Integer[] targetHour, int numOfTechnician, List<TechnicianPlanDto> technician3Plan) {
        List<TechnicianPlanDto> requestList = allRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() == 1).toList();
        List<TechnicianPlanDto> require2RequestList = allRequest.stream().filter(Predicate.not(requestList::contains)).toList();
        List<TechnicianPlanDto> sortedRequestList = requestList.stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        totalApartment = 6;
        int target = targetHour[0] - require2RequestList.stream().mapToInt(TechnicianPlanDto::getEstimateTime).sum();
        List<List<TechnicianPlanDto>> possiblePlanListForTechnician1 = new ArrayList<>();
        List<TechnicianPlanDto> possiblePlanForTechnician1 = new ArrayList<>();
        findPossibleOtherRequire2Request(possiblePlanListForTechnician1, possiblePlanForTechnician1, target, sortedRequestList, 0);

        if (possiblePlanListForTechnician1.size() > 1) {
            List<List<TechnicianPlanDto>> possiblePlanListForTechnician2 = new ArrayList<>();
            List<List<TechnicianPlanDto>> tempPossiblePlanListForTechnician1 = new ArrayList<>();
            totalApartment = 6;

            for (List<TechnicianPlanDto> plan: possiblePlanListForTechnician1) {
                List<TechnicianPlanDto> planForTech1 = new ArrayList<>(require2RequestList);
                planForTech1.addAll(plan);

                List<TechnicianPlanDto> planForTech2 = new ArrayList<>(require2RequestList);
                planForTech2.addAll(requestList.stream().filter(Predicate.not(plan::contains)).toList());

                int numOfApartment = this.findNumberOfApartment(planForTech2);
                if (numOfApartment < totalApartment) {
                    totalApartment = numOfApartment;
                    tempPossiblePlanListForTechnician1.clear();
                    possiblePlanListForTechnician2.clear();
                    tempPossiblePlanListForTechnician1.add(planForTech1);
                    possiblePlanListForTechnician2.add(planForTech2);
                } else if (numOfApartment == totalApartment) {
                    tempPossiblePlanListForTechnician1.add(planForTech1);
                    possiblePlanListForTechnician2.add(planForTech2);
                }
            }

            if (tempPossiblePlanListForTechnician1.size() > 1) {
                checkBestRoute(tempPossiblePlanListForTechnician1, possiblePlanListForTechnician2, technician3Plan, true, numOfTechnician);
            } else {
                List<List<TechnicianPlanDto>> planList = new ArrayList<>();
                planList.add(tempPossiblePlanListForTechnician1.get(0));
                planList.add(possiblePlanListForTechnician2.get(0));

                saveTechnicianPlan(planList);
            }
        } else {
            List<TechnicianPlanDto> planForTechnician1 = new ArrayList<>(require2RequestList);
            planForTechnician1.addAll(possiblePlanListForTechnician1.get(0));

            List<TechnicianPlanDto> planForTechnician2 = requestList.stream().filter(Predicate.not(possiblePlanListForTechnician1.get(0)::contains)).toList();
            planForTechnician2.addAll(require2RequestList);

            List<List<TechnicianPlanDto>> planList = new ArrayList<>();
            planList.add(planForTechnician1);
            planList.add(planForTechnician2);

            saveTechnicianPlan(planList);
        }
    }

    private void findRequire2TechnicianPlanFor3Technician(List<TechnicianPlanDto> requestListForPlan, Integer[] lowestRange, Integer[] targetHour) {
        List<List<TechnicianPlanDto>> possibleLowestPlanList = findTechnicianPlanForLowestTechnician(targetHour, requestListForPlan, lowestRange);

        List<List<TechnicianPlanDto>> otherRequestList = new ArrayList<>();
        if (possibleLowestPlanList.size() > 1) {
            for (List<TechnicianPlanDto> possibleLowestPlan: possibleLowestPlanList) {
                List<TechnicianPlanDto> possibleOtherPlanList = findOtherRequire2RequestList(requestListForPlan, possibleLowestPlan, targetHour);
                otherRequestList.add(possibleOtherPlanList);
            }
        } else {
            List<TechnicianPlanDto> possibleOtherPlanList = findOtherRequire2RequestList(requestListForPlan, possibleLowestPlanList.get(0), targetHour);
            otherRequestList.add(possibleOtherPlanList);
        }

        List<TechnicianPlanDto> bestRequest = checkBestRequest(otherRequestList).stream().sorted(Comparator.comparingInt(TechnicianPlanDto::getEstimateTime)).toList();

        List<TechnicianPlanDto> lowestTechnicianPlan = possibleLowestPlanList.get(bestRequestPlanId);
        saveLowestTechnicianPlan(lowestTechnicianPlan);

        findRequire2TechnicianPlanFor2Technician(bestRequest, targetHour, 3, lowestTechnicianPlan);
    }

    private void saveTechnicianPlanTemp(List<List<TechnicianPlanDto>> technicianPlanList) {
        int index = 1;
        for (List<TechnicianPlanDto> planList: technicianPlanList) {
            for (TechnicianPlanDto plan: planList) {
                Schedule schedule = new Schedule();
                schedule.setRequest(plan.getRequest());
                schedule.setApartment(plan.getApartment());
                schedule.setTechnician(this.technicianService.getTechnicianById(index));
                schedule.setRequestHour(plan.getRequest().getEstimateTime());
                this.scheduleRepository.saveAndFlush(schedule);
            }

            index++;
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
                schedule.setRequestHour(plan.getRequest().getEstimateTime());
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
            schedule.setRequestHour(planDto.getRequest().getEstimateTime());
            this.scheduleRepository.saveAndFlush(schedule);

            this.requestService.updateRequestStatusReadyToService(planDto.getRequest());
        }

        int priorityHour = lowestTechnicianPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        this.configService.updatePriorityHour(priorityHour);
    }

    private List<TechnicianPlanDto> checkBestRequest(List<List<TechnicianPlanDto>> otherPlanList) {
        List<TechnicianPlanDto> bestRequest = new ArrayList<>();
        int maxNum = -1;
        totalDate = Long.MAX_VALUE;

        for (List<TechnicianPlanDto> plan: otherPlanList) {
            int numberOfPriority3 = plan.stream().map(TechnicianPlanDto::getPriority).filter(priority -> priority == 3).toList().size();
            long sumDate = plan.stream().map(p -> p.getRequest().getRequestDate()).mapToLong(Date::getTime).sum();

            if (numberOfPriority3 > maxNum) {
                maxNum = numberOfPriority3;
                bestRequest.clear();
                bestRequest.addAll(plan);
                bestRequestPlanId = otherPlanList.indexOf(plan);
            } else if (numberOfPriority3 == maxNum && sumDate < totalDate) {
                totalDate = sumDate;
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

    private void findPossibleOtherRequire2Request(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index){
        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);

            if (numOfApartment < totalApartment) {
                totalApartment = numOfApartment;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (numOfApartment == totalApartment) {
                res.add(new ArrayList<>(ds));
            }
            return;
        }

        for (int i= index ; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleOtherRequire2Request(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }

    private void findPossibleRemainingRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index, List<Integer> apartment) {
        if (target == 0) {
            int remainingNumberOfApartment = ds.stream().map(TechnicianPlanDto::getApartmentId).filter(Predicate.not(apartment::contains)).toList().size();
            long sumDate = ds.stream().map(d -> d.getRequest().getRequestDate()).mapToLong(Date::getTime).sum();
            int cntPriority3 = ds.stream().map(TechnicianPlanDto::getPriority).filter(priority -> priority == 3).toList().size();

            if (remainingNumberOfApartment < totalApartment) {
                totalApartment = remainingNumberOfApartment;
                totalPriority = cntPriority3;
                totalDate = sumDate;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (remainingNumberOfApartment == totalApartment && totalPriority < cntPriority3) {
                totalPriority = cntPriority3;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (remainingNumberOfApartment == totalApartment && totalPriority == cntPriority3 && sumDate < totalDate) {
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

    private void findPossibleOnePriorityRequest(List<List<TechnicianPlanDto>> res, List<TechnicianPlanDto> ds, int target, List<TechnicianPlanDto> arr, int index) {
        if (target == 0) {
            int numOfApartment = this.findNumberOfApartment(ds);
            int sumPriority = ds.stream().map(TechnicianPlanDto::getPriority).mapToInt(Integer::intValue).sum();

            if (sumPriority < totalPriority) {
                totalPriority = sumPriority;
                res.clear();
                res.add(new ArrayList<>(ds));
            } else if (sumPriority == totalPriority && numOfApartment < totalApartment) {
                totalApartment = numOfApartment;
                res.clear();
                res.add(new ArrayList<>(ds));
            }
            return;
        }

        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getEstimateTime() > target)
                break;

            ds.add(arr.get(i));
            findPossibleOnePriorityRequest(res, ds, target-arr.get(i).getEstimateTime() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }

    private List<TechnicianPlanDto> findRequestList(int totalPriorityHour, int totalTargetHour, int totalRequestHour, List<TechnicianPlanDto> requestListForPlan, List<TechnicianPlanDto> priorityRequestList) {
        if (totalPriorityHour == totalTargetHour) {
            return priorityRequestList;
        }

        if (totalRequestHour <= totalTargetHour) {
            return requestListForPlan;
        }

        List<List<TechnicianPlanDto>> possibleRequestList = new ArrayList<>();
        List<TechnicianPlanDto> possibleRequest = new ArrayList<>();
        if (totalPriorityHour > totalTargetHour) {
            List<TechnicianPlanDto> returnList = new ArrayList<>();
            List<TechnicianPlanDto> priority1RequestList = priorityRequestList.stream().filter(req -> req.getPriority() == 1).toList();
            int priority1Hour = priority1RequestList.stream().mapToInt(TechnicianPlanDto::getEstimateTime).sum();
            int totalHour = totalTargetHour;
            List<TechnicianPlanDto> sortedPriorityRequestList = priorityRequestList.stream().filter(Predicate.not(priority1RequestList::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();

            if (priority1Hour > 0) {
                returnList.addAll(priority1RequestList);
                totalHour = totalHour - priority1Hour;
            }

            totalApartment = 6;
            totalPriority = Integer.MAX_VALUE;
            findPossibleOnePriorityRequest(possibleRequestList, possibleRequest, totalHour, sortedPriorityRequestList, 0);
            returnList.addAll(possibleRequestList.get(0));

            return returnList;
        }

        List<TechnicianPlanDto> normalRequest = requestListForPlan.stream().filter(Predicate.not(priorityRequestList::contains)).toList();
        List<Integer> apartmentIds = priorityRequestList.stream().map(TechnicianPlanDto::getApartmentId).distinct().toList();

        totalApartment = normalRequest.size();
        totalDate = Long.MAX_VALUE;
        totalPriority = Integer.MAX_VALUE;

        int priorityHour = priorityRequestList.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int targetNormalHour = totalTargetHour - priorityHour;

        List<TechnicianPlanDto> sortedNormalRequest = normalRequest.stream().sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
        findPossibleRemainingRequest(possibleRequestList, possibleRequest, targetNormalHour, sortedNormalRequest, 0, apartmentIds);

        List<TechnicianPlanDto> requestList = new ArrayList<>(priorityRequestList);
        requestList.addAll(possibleRequestList.get(0));

        return requestList;
    }

    private List<TechnicianPlanDto> findRequire2RequestList(Integer[] targetHour, List<TechnicianPlanDto> requestListForPlan) {
        List<TechnicianPlanDto> priorityRequest = requestListForPlan.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
        List<TechnicianPlanDto> priorityRequire2Request = priorityRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() > 1).toList();
        List<TechnicianPlanDto> normalRequest = requestListForPlan.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
        List<TechnicianPlanDto> normalRequire2Request = normalRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() > 1).toList();

        int require2PriorityHour = priorityRequire2Request.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

        List<List<TechnicianPlanDto>> tempList = new ArrayList<>();
        List<TechnicianPlanDto> temp = new ArrayList<>();

        totalApartment = Integer.MAX_VALUE;
        totalDate = Long.MAX_VALUE;
        totalPriority = Integer.MAX_VALUE;

        if (require2PriorityHour > targetHour[0]) {
            findPossibleOnePriorityRequest(tempList, temp, targetHour[0], priorityRequire2Request, 0);
            return tempList.get(0);
        } else if (require2PriorityHour < targetHour[0]) {
            int remainingHourForTechnician1 = targetHour[0] - require2PriorityHour;
            int remainingHourForTechnician2 = targetHour[1] - require2PriorityHour;
            List<TechnicianPlanDto> returnList = new ArrayList<>(priorityRequire2Request);

            List<Integer> apartmentIds = findApartmentId(new ArrayList<>(), returnList);

            if (priorityHour == 0) {
                findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician1, normalRequire2Request, 0, apartmentIds);
                returnList.addAll(tempList.get(0));
                return returnList;
            }

            if (priorityHour > remainingHourForTechnician1) {
                findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician1, priorityRequest, 0, apartmentIds);
                returnList.addAll(tempList.get(0));

                totalApartment = Integer.MAX_VALUE;
                totalDate = Long.MAX_VALUE;
                totalPriority = Integer.MAX_VALUE;

                List<TechnicianPlanDto> remainingPriorityRequest = priorityRequest.stream().filter(Predicate.not(tempList.get(0)::contains)).toList();
                int remainingPriorityHour = remainingHourForTechnician1 - priorityHour;
                apartmentIds = findApartmentId(returnList, new ArrayList<>());
                if (remainingPriorityHour > remainingHourForTechnician2) {
                    tempList.clear();
                    temp.clear();
                    findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2, remainingPriorityRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                } else {
                    returnList.addAll(remainingPriorityRequest);

                    apartmentIds = findApartmentId(returnList, new ArrayList<>());
                    if (remainingPriorityHour < remainingHourForTechnician2) {
                        tempList.clear();
                        temp.clear();
                        findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2 - remainingPriorityHour, normalRequest, 0, apartmentIds);
                        returnList.addAll(tempList.get(0));
                    }
                }
            } else {
                returnList.addAll(priorityRequest);

                if (priorityHour < remainingHourForTechnician1) {
                    apartmentIds = findApartmentId(returnList, new ArrayList<>());
                    findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2 + (remainingHourForTechnician1 - priorityHour), normalRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                }
            }

            return returnList;
        } else {
            return priorityRequire2Request;
        }
    }

    private List<TechnicianPlanDto> findOtherRequestList(List<TechnicianPlanDto> allRequest, List<TechnicianPlanDto> lowestPlan, Integer[] targetHour) {
        List<TechnicianPlanDto> remainingRequest = allRequest.stream().filter(Predicate.not(lowestPlan::contains)).toList();
        List<TechnicianPlanDto> priorityRequest = remainingRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();

        List<Integer> apartmentIds;

        int priorityHour = remainingRequest.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();
        int totalTargetHour = targetHour[0] + targetHour[1];

        if (priorityHour == totalTargetHour) {
            return priorityRequest;
        }

        List<List<TechnicianPlanDto>> tempPossibleOtherPlanList = new ArrayList<>();
        List<TechnicianPlanDto> tempPossibleOtherPlan = new ArrayList<>();
        if (priorityHour > totalTargetHour) {
            List<TechnicianPlanDto> returnList = new ArrayList<>();
            List<TechnicianPlanDto> priority1Request = priorityRequest.stream().filter(req -> req.getPriority() == 1).toList();
            List<TechnicianPlanDto> otherPriorityRequest = new ArrayList<>(priorityRequest);
            int priority1Hour = priority1Request.stream().mapToInt(TechnicianPlanDto::getEstimateTime).sum();
            int totalHour = totalTargetHour;

            if (priority1Hour > 0) {
                returnList.addAll(priority1Request);
                totalHour = totalHour - priority1Hour;
                otherPriorityRequest.clear();
                otherPriorityRequest.addAll(priorityRequest.stream().filter(Predicate.not(priority1Request::contains)).toList());
            }

            apartmentIds = findApartmentId(lowestPlan, new ArrayList<>());
            totalApartment = priorityRequest.size();
            totalDate = Long.MAX_VALUE;
            totalPriority = Integer.MAX_VALUE;
            findPossibleRemainingRequest(tempPossibleOtherPlanList, tempPossibleOtherPlan, totalHour, otherPriorityRequest, 0, apartmentIds);
            returnList.addAll(tempPossibleOtherPlanList.get(0));

            return returnList;
        } else {
            List<TechnicianPlanDto> normalRequest = remainingRequest.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
            int normalTargetHour = totalTargetHour - priorityHour;

            apartmentIds = findApartmentId(lowestPlan, priorityRequest);
            totalApartment = normalRequest.size();
            totalPriority = Integer.MAX_VALUE;
            totalDate = Long.MAX_VALUE;
            findPossibleRemainingRequest(tempPossibleOtherPlanList, tempPossibleOtherPlan, normalTargetHour, normalRequest, 0, apartmentIds);

            List<TechnicianPlanDto> possibleRemainingRequest = new ArrayList<>(priorityRequest);
            possibleRemainingRequest.addAll(tempPossibleOtherPlanList.get(0));

            return possibleRemainingRequest;
        }
    }

    private List<TechnicianPlanDto> findOtherRequire2RequestList(List<TechnicianPlanDto> requestListForPlan, List<TechnicianPlanDto> possibleLowestPlan, Integer[] targetHour) {
        List<TechnicianPlanDto> remainingRequest = requestListForPlan.stream().filter(Predicate.not(possibleLowestPlan::contains)).toList();
        List<TechnicianPlanDto> remainingRequire1Request = remainingRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() == 1).toList();
        List<TechnicianPlanDto> remainingRequire2Request = remainingRequest.stream().filter(req -> req.getRequest().getEstimateTechnician() > 1).toList();
        List<TechnicianPlanDto> priorityRequire2Request = remainingRequire2Request.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority())).toList();
        List<TechnicianPlanDto> normalRequire2Request = remainingRequire2Request.stream().filter(Predicate.not(priorityRequire2Request::contains)).toList();

        int require2PriorityHour = priorityRequire2Request.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

        List<List<TechnicianPlanDto>> tempList = new ArrayList<>();
        List<TechnicianPlanDto> temp = new ArrayList<>();
        List<Integer> apartmentIds;

        totalApartment = Integer.MAX_VALUE;
        totalDate = Long.MAX_VALUE;
        totalPriority = Integer.MAX_VALUE;

        if (require2PriorityHour > targetHour[0]) {
            apartmentIds = findApartmentId(possibleLowestPlan, new ArrayList<>());
            findPossibleRemainingRequest(tempList, temp, targetHour[0], priorityRequire2Request, 0, apartmentIds);

            return tempList.get(0);
        } else if (require2PriorityHour < targetHour[0]) {
            int remainingHourForTechnician1 = targetHour[0] - require2PriorityHour;
            int remainingHourForTechnician2 = targetHour[1] - require2PriorityHour;
            List<TechnicianPlanDto> returnList = new ArrayList<>(priorityRequire2Request);

            apartmentIds = findApartmentId(possibleLowestPlan, returnList);

            // TODO: เช็คตรงนี้ดีๆ ต้อง filter แต่ที่เวลาน้อยกว่า remaining
            List<TechnicianPlanDto> priorityRequest = remainingRequire1Request.stream().filter(req -> MOST_PRIORITY.contains(req.getPriority()) && req.getEstimateTime() <= remainingHourForTechnician1).toList();
            List<TechnicianPlanDto> normalRequest = remainingRequire1Request.stream().filter(Predicate.not(priorityRequest::contains)).sorted(Comparator.comparing(TechnicianPlanDto::getEstimateTime)).toList();
            int priorityHour = priorityRequest.stream().map(TechnicianPlanDto::getEstimateTime).mapToInt(Integer::intValue).sum();

            if (priorityHour == 0) {
                findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician1, normalRequire2Request, 0, apartmentIds);
                returnList.addAll(tempList.get(0));
                return returnList;
            }

            if (priorityHour > remainingHourForTechnician1) {
                findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician1, priorityRequest, 0, apartmentIds);
                returnList.addAll(tempList.get(0));

                List<TechnicianPlanDto> remainingPriorityRequest = priorityRequest.stream().filter(Predicate.not(tempList.get(0)::contains)).toList();
                int remainingPriorityHour = priorityHour - remainingHourForTechnician1;

                totalApartment = Integer.MAX_VALUE;
                totalDate = Long.MAX_VALUE;
                totalPriority = Integer.MAX_VALUE;
                if (remainingPriorityHour > remainingHourForTechnician2) {
                    tempList.clear();
                    temp.clear();
                    findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2, remainingPriorityRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                } else {
                    returnList.addAll(remainingPriorityRequest);
                    if (remainingPriorityHour < remainingHourForTechnician2) {
                        tempList.clear();
                        temp.clear();
                        findPossibleRemainingRequest(tempList, temp, remainingHourForTechnician2 - remainingPriorityHour, normalRequest, 0, apartmentIds);
                        returnList.addAll(tempList.get(0));
                    }
                }
            } else {
                returnList.addAll(priorityRequest);
                if (priorityHour < remainingHourForTechnician1) {
                    findPossibleRemainingRequest(tempList, temp, (remainingHourForTechnician1 - priorityHour) + remainingHourForTechnician2, normalRequest, 0, apartmentIds);
                    returnList.addAll(tempList.get(0));
                }
            }

            return returnList;
        } else {
            return priorityRequire2Request;
        }
    }

    private List<Integer> findApartmentId(List<TechnicianPlanDto> planList, List<TechnicianPlanDto> priorityRequest) {
        List<TechnicianPlanDto> allRequest = new ArrayList<>();
        if (!planList.isEmpty() && !priorityRequest.isEmpty()) {
            allRequest.addAll(planList);
            allRequest.addAll(priorityRequest);
        } else if (priorityRequest.isEmpty()) {
            allRequest.addAll(planList);
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

    public void findRoute() throws NoSuchElementException {
        int technician1TargetHour = this.configService.getTechnician1TargetHourConfig();
        int technician2TargetHour = this.configService.getTechnician2TargetHourConfig();
        int technician3TargetHour = this.configService.getTechnician3TargetHourConfig();
        boolean isRequire2 = this.scheduleRepository.checkHaveRequire2();

        if (isRequire2) {
            findRouteRequire2(technician1TargetHour, technician2TargetHour, technician3TargetHour);
        } else {
            findRouteRequire1(technician1TargetHour, technician2TargetHour, technician3TargetHour);
        }
    }

    private void findRouteRequire2(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int numOfTechnician = findNoOfTechnician(technician1TargetHour, technician2TargetHour, technician3TargetHour);
        List<Integer> require2TechnicianId = this.scheduleRepository.findRequire2TechnicianId();

        if (numOfTechnician == 3) {
            this.stgScheduleService.prepareSchedule(require2TechnicianId);
        }

        int driver = this.scheduleRepository.findDriver();
        boolean isMove = false;
        do {
            processFindRoute(technician1TargetHour, technician2TargetHour, technician3TargetHour, driver);

            if (!isMove) {
                boolean isRequire2Finished = this.scheduleRepository.checkRequire2Finished(require2TechnicianId);
                if (isRequire2Finished) {
                    this.stgScheduleService.prepareRequire1Schedule();
                    isMove = true;
                }
            }
        } while (isContinue(technician1TargetHour, technician2TargetHour, technician3TargetHour));
    }

    private void findRouteRequire1(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int driver = this.scheduleRepository.findDriver();

        do {
            processFindRoute(technician1TargetHour, technician2TargetHour, technician3TargetHour, driver);
        } while (isContinue(technician1TargetHour, technician2TargetHour, technician3TargetHour));
    }

    private void processFindRoute(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour, int driver) {
        List<Schedule> allTaskList;
        List<Technician> technicianListForAddRoute;
        List<Schedule> driverTaskList = new ArrayList<>();
        // จำนวนชั่วโมงเท่ากันทุกคน
        if (isTotalHourEqual(technician1TargetHour, technician2TargetHour, technician3TargetHour)) {
            List<Integer> oneApartmentIds = this.scheduleRepository.findOneApartmentIds(driver);
            int driverLatestApartmentId = this.scheduleRepository.findLatestApartmentIdByTechnicianId(driver);
            int numOfTechnician = findNoOfTechnician(technician1TargetHour, technician2TargetHour, technician3TargetHour);

            // มีช่างทำงานที่หอเดียว
            if (!oneApartmentIds.isEmpty()) {
                allTaskList = this.scheduleRepository.findSchedulesNearestOneApartment(driverLatestApartmentId, oneApartmentIds);

                boolean isAllHaveSameApartment = allTaskList.stream().map(Schedule::getTechnician).distinct().toList().size() == numOfTechnician;
                // filter list เอางานของช่างที่เป็นคนขับออก
                // isAllHaveSameApartment = false && driverHaveSameApartment = false
                // case1: ช่าง 3 คน A ทำงานที่หอเดียว, B เป็นคนขับไม่มีงานที่หอนี้, C มีงานที่หอนี้
                // case2: ช่าง 3 คน A ทำงานที่หอเดียว, B และ C ไม่มีงานที่หอนี้
                // case3: ช่าง 2 คน A ทำงานที่หอเดียว, B เป็นคนขับไม่มีงานที่หอนี้


                // ใช้ allTaskList เดิม
                // isAllHaveSameApartment = true ดึงงานคนขับใส่ driverTaskList
                // case2: ช่าง 3 คน A ทำงานที่หอเดียว, B และ C มีงานที่หอนี้
                // case3: ช่าง 2 คน A ทำงานที่หอเดียว, B เป็นคนขับมีงานที่หอนี้

                if (isAllHaveSameApartment) {
                    driverTaskList = allTaskList.stream().filter(sch -> sch.getTechnician().getId().equals(driver)).toList();
                } else {
                    allTaskList.removeIf(tech -> tech.getTechnician().getId().equals(driver));
                }
            } else { // ไม่มีช่างที่ทำงานที่หอเดียว
                List<Integer> sameApartmentIds = this.scheduleRepository.findSameApartmentIds(numOfTechnician);
                // มีช่างทำงานที่หอเดียวกันทุกคน
                // case1-1: ช่าง 3 คน A, B และ C มีงานที่หอเดียวกัน (B เป็นคนขับ)
                // case1-2: ช่าง 3 คน A และ C มีงานที่หอเดียวกัน B ไม่มีงานที่หอนั้น (B เป็นคนขับ)
                // case1-3: ช่าง 2 คน A และ B มีงานที่หอเดียวกัน

                // ไม่มีช่างทำงานที่หอเดียวกัน
                // case2-1: ช่าง 3 คน A, B และ C ไม่มีงานที่หอเดียวกัน (B เป็นคนขับ)
                // case2-2: ช่าง 3 คน A และ B มีงานที่หอเดียวกัน C ไม่มีงานที่หอนั้น (B เป็นคนขับ)
                // case2-3: ช่าง 2 คน A และ B ไม่มีงานที่หอเดียวกัน

                // มีช่างทำงานที่หอเดียวกันทั้งหมด
                // case1-1: ช่าง 3 คน A, B และ C มีงานที่หอเดียวกัน (B เป็นคนขับ)
                // case1-3: ช่าง 2 คน A และ B มีงานที่หอเดียวกัน
                if (!sameApartmentIds.isEmpty()) {
                    allTaskList = this.scheduleRepository.findSchedulesNearestOneApartment(driverLatestApartmentId, sameApartmentIds);
                    driverTaskList = allTaskList.stream().filter(sch -> sch.getTechnician().getId().equals(driver)).toList();
                } else { // ไม่มีช่างทำงานที่หอเดียวกันทุกคน
                    if (numOfTechnician == 3) {
                        sameApartmentIds = this.scheduleRepository.findSameApartmentIds(numOfTechnician - 1);
                        if (!sameApartmentIds.isEmpty()) {
                            boolean isDriverHaveSameApartment = this.scheduleRepository.checkDriverHaveSameApartment(sameApartmentIds, driver);

                            if (isDriverHaveSameApartment) { // case2-2: ช่าง 3 คน A และ B มีงานที่หอเดียวกัน C ไม่มีงานที่หอนั้น (B เป็นคนขับ)
                                allTaskList = findNearestTaskExceptDriver(driverLatestApartmentId, driver);

                            } else { // case1-2: ช่าง 3 คน A และ C มีงานที่หอเดียวกัน B ไม่มีงานที่หอนั้น (B เป็นคนขับ)
                                allTaskList = this.scheduleRepository.findSchedulesNearestOneApartment(driverLatestApartmentId, sameApartmentIds);
                            }
                        } else { // case2-1: ช่าง 3 คน A, B และ C ไม่มีงานที่หอเดียวกัน (B เป็นคนขับ)
                            allTaskList = findNearestTaskExceptDriver(driverLatestApartmentId, driver);
                        }
                    } else { // case2-3: ช่าง 2 คน A และ B ไม่มีงานที่หอเดียวกัน
                        allTaskList = findNearestTaskExceptDriver(driverLatestApartmentId, driver);
                    }
                }
            }
            technicianListForAddRoute = findTechnicianForAddRoute(technician1TargetHour, technician2TargetHour, technician3TargetHour);
        } else { // จำนวนชั่วโมงไม่เท่ากันทุกคน หรือ เท่ากันบางคน
            allTaskList = findNextTask(driver, technician1TargetHour, technician2TargetHour, technician3TargetHour);
            driverTaskList = allTaskList.stream().filter(sch -> sch.getTechnician().getId() == driver).toList();
            List<Technician> technicianTaskList = allTaskList.stream().map(Schedule::getTechnician).distinct().toList();
            technicianListForAddRoute = findTechnicianForAddRoute(technician1TargetHour, technician2TargetHour, technician3TargetHour);
            technicianListForAddRoute.removeIf(tech -> !technicianTaskList.contains(tech) && !tech.getId().equals(driver));
        }

        int nextApartment = allTaskList.get(0).getApartment().getId();
        for (Technician technician: technicianListForAddRoute) {
            // ไปที่หอ ...
            int latestApartment = this.scheduleRepository.findLatestApartmentIdByTechnicianId(technician.getId());
            boolean canWalk = this.apartmentDistanceService.checkCanWalk(latestApartment, nextApartment);

            // ถ้าเดินได้ และในรายการไม่มี driver ไม่ต้องใส่เส้นทางให้ driver
            if (canWalk && driverTaskList.isEmpty() && technician.getId().equals(driver)) {
                continue;
            }

            // ถ้า nextApartment != จุดที่อยู่ปัจจุบัน ใส่เส้นทาง
            if (nextApartment != latestApartment) {
                Schedule schedule = new Schedule();
                schedule.setTechnician(technician);
                schedule.setApartment(allTaskList.get(0).getApartment());
                schedule.setSequence(this.scheduleRepository.findLatestSequenceByTechnicianId(technician.getId()) + 1);
                this.scheduleRepository.saveAndFlush(schedule);
            }

            if (!technician.getId().equals(driver)) {
                List<Schedule> taskList = allTaskList.stream().filter(sch -> sch.getTechnician() == technician).toList();
                if (!taskList.isEmpty()) {
                    updateSequenceTask(taskList);
                }
            }
        }

        if (!driverTaskList.isEmpty()) {
            updateSequenceTaskForDriver(driverTaskList, driver, technician1TargetHour, technician2TargetHour, technician3TargetHour);
        }

        updateRoute(driver, technician1TargetHour, technician2TargetHour, technician3TargetHour);
    }

    private List<Schedule> findNearestTaskExceptDriver(Integer driverLatestApartmentId, Integer driver) {
        int apartmentId = this.scheduleRepository.findNearestApartmentId(driverLatestApartmentId, driver);
        return this.scheduleRepository.findSchedulesByApartmentIdAndSequenceIsNull(apartmentId);
    }

    private List<Schedule> findDriverNearestTask(Integer latestApartmentId, Integer technicianId, int diffHour) {
        int apartmentId = this.scheduleRepository.findNearestApartmentIdByTechnicianId(latestApartmentId, technicianId);
        List<Schedule> taskList = this.scheduleRepository.findSchedulesByApartmentIdAndTechnicianIdAndSequenceIsNull(apartmentId, technicianId);

        return findMatchHourByDiffHour(taskList, diffHour);
    }

    private List<Schedule> findDriverAndOtherNearestTask(Integer latestApartmentId, Integer driver, Integer other, Integer[] hour) {
        boolean isOtherOneApartment = this.scheduleRepository.checkOtherOneApartment(other);
        if (isOtherOneApartment) {
            return this.scheduleRepository.findSchedulesByTechnicianIdAndSequenceIsNull(other);
        }

        List<Integer> sameApartmentId = this.scheduleRepository.findDriverAndOtherSameApartment(Arrays.asList(driver, other));
        if (sameApartmentId != null) {
            List<Schedule> taskList = this.scheduleRepository.findSchedulesNearestOneApartmentByTechnician(latestApartmentId, sameApartmentId, Arrays.asList(driver, other));
            List<Schedule> driverTask = taskList.stream().filter(sch -> sch.getTechnician().getId().equals(driver)).toList();
            int otherHour = hour[1] + taskList.stream().mapToInt(Schedule::getRequestHour).sum();
            int diffHour = Math.min(otherHour, hour[0]) - hour[1];

            taskList.addAll(findMatchHourByDiffHour(driverTask, diffHour));

            return taskList;
        }

        int apartmentId = this.scheduleRepository.findNearestApartmentIdByTechnicianId(latestApartmentId, other);
        List<Integer> technicianId = Arrays.asList(driver, other);
        List<Schedule> taskList = this.scheduleRepository.findSchedulesByApartmentIdAndTechnicianIdInAndSequenceIsNull(apartmentId, technicianId);
        List<Schedule> driverTask = taskList.stream().filter(sch -> sch.getTechnician().getId().equals(driver)).toList();

        if (!driverTask.isEmpty()) {
            taskList.removeIf(driverTask::contains);
            int otherHour = hour[1] + taskList.stream().mapToInt(Schedule::getRequestHour).sum();
            int diffHour = Math.min(otherHour, hour[0]) - hour[1];

            taskList.addAll(findMatchHourByDiffHour(driverTask, diffHour));
        }

        return taskList;
    }

    private List<Schedule> findMatchHourByDiffHour(List<Schedule> taskList, int diffHour) {
        int totalHour = taskList.stream().mapToInt(Schedule::getRequestHour).sum();
        if (totalHour <= diffHour) {
            return taskList;
        } else {
            List<Schedule> sortedTaskList = taskList.stream().sorted(Comparator.comparingInt(Schedule::getRequestHour)).toList();
            List<List<Schedule>> matchHourTaskList = new ArrayList<>();
            List<Schedule> matchHourTask = new ArrayList<>();
            findMatchHourTask(matchHourTaskList, matchHourTask, diffHour, sortedTaskList, 0);

            if (!matchHourTaskList.isEmpty()) {
                return matchHourTaskList.get(0);
            } else {
                List<Schedule> returnList = new ArrayList<>();
                totalHour = 0;
                for (Schedule task: sortedTaskList) {
                    totalHour += task.getRequestHour();
                    if (totalHour >= diffHour) {
                        if (totalHour > diffHour) {
                            int updateHour = totalHour - diffHour;
                            int createHour = task.getRequestHour() - updateHour;
                            task.setRequestHour(updateHour);
                            this.scheduleRepository.saveAndFlush(task);

                            Schedule schedule = new Schedule();
                            schedule.setRequest(task.getRequest());
                            schedule.setApartment(task.getApartment());
                            schedule.setTechnician(task.getTechnician());
                            schedule.setRequestHour(createHour);
                            this.scheduleRepository.saveAndFlush(schedule);
                        }
                        returnList.add(task);

                        break;
                    }
                    returnList.add(task);
                }
                return returnList;
            }
        }
    }

    private void updateRoute(int driver, int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        int technician1LatestApartmentId = this. scheduleRepository.findLatestApartmentIdByTechnicianId(1);
        int technician2LatestApartmentId = this. scheduleRepository.findLatestApartmentIdByTechnicianId(2);
        int technician3LatestApartmentId = this. scheduleRepository.findLatestApartmentIdByTechnicianId(3);

        int firstRoute = 0;
        int secondRoute = 0;
        List<Integer> firstTechnicianIds = new ArrayList<>();
        List<Integer> secondTechnicianIds = new ArrayList<>();
        // ช่าง 3 คน
        if (technician1TotalHour != 0 && technician2TotalHour != 0 && technician3TotalHour != 0) {
            // ช่างคนที่ 1, 2 และ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                switch (driver) {
                    case 1:
                        // เวลาไม่เท่ากัน
                        // case1: 2 > (3 = 1)
                        // ช่างคนที่ 3 อยู่หอเดียวกับช่างคนที่ 1 nothing to do
                        // ช่างคนที่ 3 อยู่คนละหอกับช่างคนที่ 1
                        if (technician2TotalHour > technician3TotalHour && technician3TotalHour == technician1TotalHour && technician3LatestApartmentId != technician1LatestApartmentId) {
                            firstTechnicianIds.add(1);
                            firstRoute = technician3LatestApartmentId;
                        }
                        // case2: 3 > (2 = 1)
                        // ช่างคนที่ 2 อยู่หอเดียวกับช่างคนที่ 1 nothing to do
                        // ช่างคนที่ 2 อยู่คนละหอกับช่างคนที่ 1
                        if (technician3TotalHour > technician2TotalHour && technician2TotalHour == technician1TotalHour && technician2LatestApartmentId != technician1LatestApartmentId) {
                            firstTechnicianIds.add(1);
                            firstRoute = technician2LatestApartmentId;
                        }

                        // เวลาเท่ากันหมด
                        // case1: 1 = 2 = 3
                        if (technician3TotalHour == technician2TotalHour && technician2TotalHour == technician1TotalHour) {
                            // ช่างคนที่ 1,2 และ 3 อยู่หอเดียวกันหมด nothing to do
                            // ช่างคนที่ 1,2 และ 3 อยู่คนละหอกัน
                            if (technician1LatestApartmentId != technician2LatestApartmentId && technician2LatestApartmentId != technician3LatestApartmentId && technician1LatestApartmentId != technician3LatestApartmentId) {
                                firstRoute = this.apartmentDistanceService.getNearest(driver, Arrays.asList(technician2LatestApartmentId, technician3LatestApartmentId));
                                firstTechnicianIds.add(1);

                                if (firstRoute == technician2LatestApartmentId) {
                                    secondRoute = technician3LatestApartmentId;
                                    secondTechnicianIds.addAll(Arrays.asList(1, 2));
                                } else {
                                    secondRoute = technician2LatestApartmentId;
                                    secondTechnicianIds.addAll(Arrays.asList(1, 3));
                                }
                            }
                            // ช่างคนที่ 1 และ 2 อยู่หอเดียวกัน แต่ช่างคนที่ 3 อยู่คนละหอ
                            if (technician1LatestApartmentId == technician2LatestApartmentId && technician2LatestApartmentId != technician3LatestApartmentId) {
                                firstTechnicianIds.addAll(Arrays.asList(1, 2));
                                firstRoute = technician3LatestApartmentId;
                            }
                            // ช่างคนที่ 1 และ 3 อยู่หอเดียวกัน แต่ช่างคนที่ 2 อยู่คนละหอ
                            if (technician1LatestApartmentId == technician3LatestApartmentId && technician3LatestApartmentId != technician2LatestApartmentId) {
                                firstTechnicianIds.addAll(Arrays.asList(1, 3));
                                firstRoute = technician2LatestApartmentId;
                            }
                            // ช่างคนที่ 2 และ 3 อยู่หอเดียวกัน แต่ช่างคนที่ 1 อยู่คนละหอ
                            if (technician2LatestApartmentId == technician3LatestApartmentId && technician3LatestApartmentId != technician1LatestApartmentId) {
                                firstTechnicianIds.add(1);
                                firstRoute = technician2LatestApartmentId;
                            }
                        }
                        break;
                    case 2:
                        // เวลาไม่เท่ากัน
                        // case1: 1 > (3 = 2)
                        // ช่างคนที่ 3 อยู่หอเดียวกับช่างคนที่ 2 nothing to do
                        // ช่างคนที่ 3 อยู่คนละหอกับช่างคนที่ 2
                        if (technician1TotalHour > technician3TotalHour && technician3TotalHour == technician2TotalHour && technician3LatestApartmentId != technician2LatestApartmentId) {
                            firstTechnicianIds.add(2);
                            firstRoute = technician3LatestApartmentId;
                        }
                        // case2: 3 > (1 = 2)
                        // ช่างคนที่ 2 อยู่หอเดียวกับช่างคนที่ 1 nothing to do
                        // ช่างคนที่ 2 อยู่คนละหอกับช่างคนที่ 1
                        if (technician3TotalHour > technician1TotalHour && technician1TotalHour == technician2TotalHour && technician1LatestApartmentId != technician2LatestApartmentId) {
                            firstTechnicianIds.add(2);
                            firstRoute = technician1LatestApartmentId;
                        }

                        // เวลาเท่ากันหมด
                        // case1: 1 = 2 = 3
                        if (technician3TotalHour == technician2TotalHour && technician2TotalHour == technician1TotalHour) {
                            // ช่างคนที่ 1,2 และ 3 อยู่หอเดียวกันหมด nothing to do
                            // ช่างคนที่ 1,2 และ 3 อยู่คนละหอกัน
                            if (technician1LatestApartmentId != technician2LatestApartmentId && technician2LatestApartmentId != technician3LatestApartmentId && technician1LatestApartmentId != technician3LatestApartmentId) {
                                firstRoute = this.apartmentDistanceService.getNearest(driver, Arrays.asList(technician1LatestApartmentId, technician3LatestApartmentId));
                                firstTechnicianIds.add(2);

                                if (firstRoute == technician1LatestApartmentId) {
                                    secondRoute = technician3LatestApartmentId;
                                    secondTechnicianIds.addAll(Arrays.asList(2, 1));
                                } else {
                                    secondRoute = technician1LatestApartmentId;
                                    secondTechnicianIds.addAll(Arrays.asList(2, 3));
                                }
                            }
                            // ช่างคนที่ 2 และ 1 อยู่หอเดียวกัน แต่ช่างคนที่ 3 อยู่คนละหอ
                            if (technician2LatestApartmentId == technician1LatestApartmentId && technician1LatestApartmentId != technician3LatestApartmentId) {
                                firstTechnicianIds.addAll(Arrays.asList(2, 1));
                                firstRoute = technician3LatestApartmentId;
                            }
                            // ช่างคนที่ 2 และ 3 อยู่หอเดียวกัน แต่ช่างคนที่ 1 อยู่คนละหอ
                            if (technician2LatestApartmentId == technician3LatestApartmentId && technician3LatestApartmentId != technician1LatestApartmentId) {
                                firstTechnicianIds.addAll(Arrays.asList(2, 3));
                                firstRoute = technician1LatestApartmentId;
                            }
                            // ช่างคนที่ 3 และ 1 อยู่หอเดียวกัน แต่ช่างคนที่ 2 อยู่คนละหอ
                            if (technician3LatestApartmentId == technician1LatestApartmentId && technician1LatestApartmentId != technician2LatestApartmentId) {
                                firstTechnicianIds.add(2);
                                firstRoute = technician3LatestApartmentId;
                            }
                        }
                        break;
                    case 3:
                        // เวลาไม่เท่ากัน
                        // case1: 1 > (2 = 3)
                        // ช่างคนที่ 2 อยู่หอเดียวกับช่างคนที่ 3 nothing to do
                        // ช่างคนที่ 2 อยู่คนละหอกับช่างคนที่ 3
                        if (technician1TotalHour > technician1TotalHour && technician2TotalHour == technician3TotalHour && technician2LatestApartmentId != technician3LatestApartmentId) {
                            firstTechnicianIds.add(3);
                            firstRoute = technician2LatestApartmentId;
                        }
                        // case2: 2 > (1 = 3)
                        // ช่างคนที่ 1 อยู่หอเดียวกับช่างคนที่ 3 nothing to do
                        // ช่างคนที่ 1 อยู่คนละหอกับช่างคนที่ 3
                        if (technician2TotalHour > technician1TotalHour && technician1TotalHour == technician3TotalHour && technician1LatestApartmentId != technician3LatestApartmentId) {
                            firstTechnicianIds.add(3);
                            firstRoute = technician1LatestApartmentId;
                        }

                        // เวลาเท่ากันหมด
                        // case1: 1 = 2 = 3
                        if (technician3TotalHour == technician2TotalHour && technician2TotalHour == technician1TotalHour) {
                            // ช่างคนที่ 1,2 และ 3 อยู่หอเดียวกันหมด nothing to do
                            // ช่างคนที่ 1,2 และ 3 อยู่คนละหอกัน
                            if (technician1LatestApartmentId != technician2LatestApartmentId && technician2LatestApartmentId != technician3LatestApartmentId && technician1LatestApartmentId != technician3LatestApartmentId) {
                                firstRoute = this.apartmentDistanceService.getNearest(driver, Arrays.asList(technician1LatestApartmentId, technician2LatestApartmentId));
                                firstTechnicianIds.add(3);

                                if (firstRoute == technician1LatestApartmentId) {
                                    secondRoute = technician2LatestApartmentId;
                                    secondTechnicianIds.addAll(Arrays.asList(3, 1));
                                } else {
                                    secondRoute = technician1LatestApartmentId;
                                    secondTechnicianIds.addAll(Arrays.asList(3, 2));
                                }
                            }
                            // ช่างคนที่ 3 และ 1 อยู่หอเดียวกัน แต่ช่างคนที่ 2 อยู่คนละหอ
                            if (technician3LatestApartmentId == technician1LatestApartmentId && technician1LatestApartmentId != technician2LatestApartmentId) {
                                firstTechnicianIds.addAll(Arrays.asList(3, 1));
                                firstRoute = technician2LatestApartmentId;
                            }
                            // ช่างคนที่ 3 และ 2 อยู่หอเดียวกัน แต่ช่างคนที่ 1 อยู่คนละหอ
                            if (technician3LatestApartmentId == technician2LatestApartmentId && technician2LatestApartmentId != technician1LatestApartmentId) {
                                firstTechnicianIds.addAll(Arrays.asList(3, 2));
                                firstRoute = technician1LatestApartmentId;
                            }
                            // ช่างคนที่ 1 และ 2 อยู่หอเดียวกัน แต่ช่างคนที่ 3 อยู่คนละหอ
                            if (technician1LatestApartmentId == technician2LatestApartmentId && technician2LatestApartmentId != technician3LatestApartmentId) {
                                firstTechnicianIds.add(3);
                                firstRoute = technician1LatestApartmentId;
                            }
                        }
                        break;
                }
            }

            // ช่างคนที่ 1 จัดเส้นทางเสร็จแล้ว
            // ช่างคนที่ 2 กับ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour == technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                if (technician2TotalHour == technician3TotalHour && technician2LatestApartmentId != technician3LatestApartmentId) {
                    if (driver == 2) {
                        firstRoute = technician3LatestApartmentId;
                    } else {
                        firstRoute = technician2LatestApartmentId;
                    }
                    firstTechnicianIds.add(driver);
                }
            }

            // ช่างคนที่ 2 จัดเส้นทางเสร็จแล้ว
            // ช่างคนที่ 1 กับ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour == technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                if (technician1TotalHour == technician3TotalHour && technician1LatestApartmentId != technician3LatestApartmentId) {
                    if (driver == 1) {
                        firstRoute = technician3LatestApartmentId;
                    } else {
                        firstRoute = technician1LatestApartmentId;
                    }
                    firstTechnicianIds.add(driver);
                }
            }

            // ช่างคนที่ 3 จัดเส้นทางเสร็จแล้ว
            // ช่างคนที่ 1 กับ 2 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour == technician3TargetHour) {
                if (technician1TotalHour == technician2TotalHour && technician1LatestApartmentId != technician2LatestApartmentId) {
                    if (driver == 1) {
                        firstRoute = technician2LatestApartmentId;
                    } else {
                        firstRoute = technician1LatestApartmentId;
                    }
                    firstTechnicianIds.add(driver);
                }
            }
        }

        // ช่าง 2 คน
        if (technician1TotalHour != 0 && technician2TotalHour != 0 && technician3TotalHour == 0) {
            if (technician1TotalHour == technician2TotalHour && technician1LatestApartmentId != technician2LatestApartmentId) {
                if (driver == 1) {
                    firstRoute = technician2LatestApartmentId;
                } else {
                    firstRoute = technician1LatestApartmentId;
                }
                firstTechnicianIds.add(driver);
            }
        }

        if (firstRoute != 0) {
            createRoute(firstRoute, firstTechnicianIds);
        }

        if (secondRoute != 0) {
            createRoute(secondRoute, secondTechnicianIds);
        }
    }

    private void createRoute(Integer route, List<Integer> technicianIds) {
        Apartment apartment = this.apartmentService.getApartmentById(route);
        List<Technician> technicianList = this.technicianService.getTechnicianByIds(technicianIds);

        for (Technician technician: technicianList) {
            Schedule schedule = new Schedule();
            schedule.setTechnician(technician);
            schedule.setApartment(apartment);
            schedule.setSequence(this.scheduleRepository.findLatestSequenceByTechnicianId(technician.getId()) + 1);
            this.scheduleRepository.saveAndFlush(schedule);
        }
    }

    private void updateSequenceTaskForDriver(List<Schedule> taskList, int driver, int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        int diffHour = 0;
        // ช่าง 3 คน
        if (technician1TargetHour != 0 && technician2TargetHour != 0 && technician3TargetHour != 0) {
            // ช่างคนที่ 3 เป็นคนขับ
            if (driver == 3) {
                // จำนวนชั่วโมงช่างคนที่ 1 > 2 > 3
                if (technician1TotalHour > technician2TotalHour) {
                    diffHour = technician2TotalHour - technician3TotalHour;
                } else { // จำนวนชั่วโมงช่างคนที่ 2 > 1 > 3 หรือ (2 = 1) > 3
                    diffHour = technician1TotalHour - technician3TotalHour;
                }
            } else if (driver == 2) { // ช่างคนที่ 2 เป็นคนขับ
                // จำนวนชั่วโมงช่างคนที่ 1 > 3 > 2
                if (technician1TotalHour > technician3TotalHour) {
                    diffHour = technician3TotalHour - technician2TotalHour;
                } else { // จำนวนชั่วโมงช่างคนที่ 3 > 1 > 2 หรือ (3 = 1) > 2
                    diffHour = technician1TotalHour - technician2TotalHour;
                }
            } else {  // ช่างคนที่ 1 เป็นคนขับ
                // จำนวนชั่วโมงช่างคนที่ 2 > 3 > 1
                if (technician2TotalHour > technician3TotalHour) {
                    diffHour = technician3TotalHour - technician1TotalHour;
                } else { // จำนวนชั่วโมงช่างคนที่ 3 > 2 > 1 หรือ (3 = 2) > 1
                    diffHour = technician2TotalHour - technician1TotalHour;
                }
            }
        }

        // ช่าง 2 คน
        if (technician1TargetHour != 0 && technician2TargetHour != 0 && technician3TargetHour == 0) {
            if (driver == 2) { // ช่างคนที่ 2 เป็นคนขับ
                diffHour = technician1TotalHour - technician2TotalHour;
            } else {  // ช่างคนที่ 1 เป็นคนขับ
                diffHour = technician2TotalHour - technician1TotalHour;
            }
        }

        keepWorking = true;
        int totalHour = taskList.stream().mapToInt(Schedule::getRequestHour).sum();
        if (totalHour <= diffHour) {
            updateSequenceTask(taskList);
        } else {
            List<Schedule> sortedTaskList = taskList.stream().sorted(Comparator.comparingInt(Schedule::getRequestHour)).toList();
            List<List<Schedule>> matchHourTaskList = new ArrayList<>();
            List<Schedule> matchHourTask = new ArrayList<>();
            findMatchHourTask(matchHourTaskList, matchHourTask, diffHour, sortedTaskList, 0);

            if (!matchHourTaskList.isEmpty()) {
                updateSequenceTask(matchHourTaskList.get(0));
            } else {
                totalHour = 0;
                for (Schedule task: sortedTaskList) {
                    totalHour += task.getRequestHour();
                    if (totalHour >= diffHour) {
                        if (totalHour > diffHour) {
                            int updateHour = totalHour - diffHour;
                            int createHour = task.getRequestHour() - updateHour;
                            task.setRequestHour(updateHour);
                            this.scheduleRepository.saveAndFlush(task);

                            Schedule schedule = new Schedule();
                            schedule.setRequest(task.getRequest());
                            schedule.setApartment(task.getApartment());
                            schedule.setTechnician(task.getTechnician());
                            schedule.setRequestHour(createHour);
                            this.scheduleRepository.saveAndFlush(schedule);
                        }
                        task.setSequence(this.scheduleRepository.findLatestSequenceByTechnicianId(task.getTechnician().getId()) + 1);
                        this.scheduleRepository.saveAndFlush(task);

                        break;
                    }
                    task.setSequence(this.scheduleRepository.findLatestSequenceByTechnicianId(task.getTechnician().getId()) + 1);
                    this.scheduleRepository.saveAndFlush(task);
                }
            }
        }
    }

    private void findMatchHourTask(List<List<Schedule>> res, List<Schedule> ds, int target, List<Schedule> arr, int index) {
        if (!keepWorking) {
            return;
        }

        if (target == 0) {
            keepWorking = false;
            res.clear();
            res.add(new ArrayList<>(ds));
            return;
        }

        for (int i=index; i<arr.size(); i++) {
            if (arr.get(i).getRequestHour() > target)
                break;

            ds.add(arr.get(i));
            findMatchHourTask(res, ds, target-arr.get(i).getRequestHour() , arr, i+1);
            ds.remove(ds.size()-1 );
        }
    }

    private int findNoOfTechnician(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        // ช่าง 3 คน
        int noOfTechnician = 0;
        if (technician1TargetHour != 0 && technician2TargetHour != 0 && technician3TargetHour != 0) {
            if (technician1TotalHour != technician1TargetHour) {
                noOfTechnician += 1;
            }

            if (technician2TotalHour != technician2TargetHour) {
                noOfTechnician += 1;
            }

            if (technician3TotalHour != technician3TargetHour) {
                noOfTechnician += 1;
            }
            return noOfTechnician;
        }
        // ช่าง 2 คน
        if (technician1TargetHour != 0 && technician2TargetHour != 0) {
            if (technician1TotalHour != technician1TargetHour) {
                noOfTechnician += 1;
            }

            if (technician2TotalHour != technician2TargetHour) {
                noOfTechnician += 1;
            }
            return noOfTechnician;
        }
        // ช่าง 1 คน
        if (technician1TargetHour != 0 && technician1TotalHour != technician1TargetHour) {
            return 1;
        }

        return 0;
    }

    private List<Schedule> findNextTask(int driver, int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        int diffHour = 0;
        int sameTechnicianId = 0;
        int driverLatestApartmentId = this. scheduleRepository.findLatestApartmentIdByTechnicianId(driver);
        boolean isDriverLowest = false;
        List<Schedule> allTask;
        Integer[] hour = new Integer[2];

        // ช่าง 3 คน
        if (technician1TotalHour != 0 && technician2TotalHour != 0 && technician3TotalHour != 0) {
            // ช่างคนที่ 1, 2 และ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                switch (driver) {
                    case 1:
                        // คนที่ 1 เป็นคนขับ
                        // 2 > 3 > 1
                        if (technician2TotalHour > technician3TotalHour && technician3TotalHour > technician1TotalHour ) {
                            diffHour = technician3TotalHour - technician1TotalHour;
                            isDriverLowest = true;
                        }
                        // 2 > (3 = 1)
                        if (technician2TotalHour > technician3TotalHour && technician3TotalHour == technician1TotalHour) {
                            diffHour = technician2TotalHour - technician1TotalHour;
                            sameTechnicianId = 3;
                            hour[0] = technician2TotalHour;
                            hour[1] = technician1TotalHour;
                        }
                        // 3 > 2 > 1
                        if (technician3TotalHour > technician2TotalHour && technician2TotalHour > technician1TotalHour) {
                            diffHour = technician2TotalHour - technician1TotalHour;
                            isDriverLowest = true;
                        }
                        // 3 > (2 = 1)
                        if (technician3TotalHour > technician2TotalHour && technician2TotalHour == technician1TotalHour) {
                            diffHour = technician3TotalHour - technician1TotalHour;
                            sameTechnicianId = 2;
                            hour[0] = technician3TotalHour;
                            hour[1] = technician1TotalHour;
                        }
                        // (2 = 3) > 1
                        if (technician2TotalHour == technician3TotalHour && technician3TotalHour > technician1TotalHour) {
                            diffHour = technician3TotalHour - technician1TotalHour;
                            isDriverLowest = true;
                        }
                        break;
                    case 2:
                        // คนที่ 2 เป็นคนขับ
                        // 1 > 3 > 2
                        if (technician1TotalHour > technician3TotalHour && technician3TotalHour > technician2TotalHour) {
                            diffHour = technician3TotalHour - technician2TotalHour;
                            isDriverLowest = true;
                        }
                        // 1 > (3 = 2)
                        if (technician1TotalHour > technician3TotalHour && technician3TotalHour == technician2TotalHour) {
                            diffHour = technician1TotalHour - technician2TotalHour;
                            sameTechnicianId = 3;
                            hour[0] = technician1TotalHour;
                            hour[1] = technician2TotalHour;
                        }
                        // 3 > 1 > 2
                        if (technician3TotalHour > technician1TotalHour && technician1TotalHour > technician2TotalHour) {
                            diffHour = technician1TotalHour - technician2TotalHour;
                            isDriverLowest = true;
                        }
                        // 3 > (1 = 2)
                        if (technician3TotalHour > technician1TotalHour && technician1TotalHour == technician2TotalHour) {
                            diffHour = technician3TotalHour - technician2TotalHour;
                            sameTechnicianId = 1;
                            hour[0] = technician3TotalHour;
                            hour[1] = technician2TotalHour;
                        }
                        // (1 = 3) > 2
                        if (technician1TotalHour == technician3TotalHour && technician3TotalHour > technician2TotalHour) {
                            diffHour = technician3TotalHour - technician2TotalHour;
                            isDriverLowest = true;
                        }
                        break;
                    case 3:
                        // คนที่ 3 เป็นคนขับ
                        // 1 > 2 > 3
                        if (technician1TotalHour > technician2TotalHour && technician2TotalHour > technician3TotalHour) {
                            diffHour = technician2TotalHour - technician3TotalHour;
                            isDriverLowest = true;
                        }
                        // 1 > (2 = 3)
                        if (technician1TotalHour > technician2TotalHour && technician2TotalHour == technician3TotalHour) {
                            diffHour = technician1TotalHour - technician3TotalHour;
                            sameTechnicianId = 2;
                            hour[0] = technician1TotalHour;
                            hour[1] = technician3TotalHour;
                        }
                        // 2 > 1 > 3
                        if (technician2TotalHour > technician1TotalHour && technician1TotalHour > technician3TotalHour) {
                            diffHour = technician1TotalHour - technician3TotalHour;
                            isDriverLowest = true;
                        }
                        // 2 > (1 = 3)
                        if (technician2TotalHour > technician1TotalHour && technician1TotalHour == technician3TotalHour) {
                            diffHour = technician2TotalHour - technician3TotalHour;
                            sameTechnicianId = 1;
                            hour[0] = technician2TotalHour;
                            hour[1] = technician3TotalHour;
                        }
                        // (1 = 2) > 3
                        if (technician1TotalHour == technician2TotalHour && technician2TotalHour > technician3TotalHour) {
                            diffHour = technician2TotalHour - technician3TotalHour;
                            isDriverLowest = true;
                        }
                        break;
                }
            }

            // ช่างคนที่ 1 จัดเส้นทางเสร็จแล้ว
            // ช่างคนที่ 2 กับ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour == technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                // หาจำนวนชั่วโมงให้เท่ากับที่มากกว่า
                if (technician2TotalHour > technician3TotalHour) {
                    diffHour = technician2TotalHour - technician3TotalHour;
                    isDriverLowest = true;
                } else {
                    diffHour = technician3TotalHour - technician2TotalHour;
                    isDriverLowest = true;
                }
            }

            // ช่างคนที่ 2 จัดเส้นทางเสร็จแล้ว
            // ช่างคนที่ 1 กับ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour == technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                if (technician1TotalHour > technician3TotalHour) {
                    diffHour = technician1TotalHour - technician3TotalHour;
                    isDriverLowest = true;
                } else {
                    diffHour = technician3TotalHour - technician1TotalHour;
                    isDriverLowest = true;
                }
            }

            // ช่างคนที่ 3 จัดเส้นทางเสร็จแล้ว
            // ช่างคนที่ 1 กับ 2 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour == technician3TargetHour) {
                if (technician1TotalHour > technician2TotalHour) {
                    diffHour = technician1TotalHour - technician2TotalHour;
                    isDriverLowest = true;
                } else {
                    diffHour = technician2TotalHour - technician1TotalHour;
                    isDriverLowest = true;
                }
            }
        }

        // ช่าง 2 คน
        if (technician1TotalHour != 0 && technician2TotalHour != 0 && technician3TotalHour == 0) {
            if (technician1TotalHour > technician2TotalHour) {
                diffHour = technician1TotalHour - technician2TotalHour;
                isDriverLowest = true;
            } else {
                diffHour = technician2TotalHour - technician1TotalHour;
                isDriverLowest = true;
            }
        }

        if (isDriverLowest) {
            allTask = findDriverNearestTask(driverLatestApartmentId, driver, diffHour);
        } else {
            allTask = findDriverAndOtherNearestTask(driverLatestApartmentId, driver, sameTechnicianId, hour);
        }

        return allTask;
    }

    private boolean isContinue(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        if (technician1TargetHour != 0 && technician2TargetHour != 0 && technician3TargetHour != 0) {
            return technician1TotalHour != technician1TargetHour || technician2TotalHour != technician2TargetHour || technician3TotalHour != technician3TargetHour;
        }

        if (technician1TargetHour != 0 && technician2TargetHour != 0) {
            return technician1TotalHour != technician1TargetHour || technician2TotalHour != technician2TargetHour;
        }

        return true;
    }

    private void updateSequenceTask(List<Schedule> allTask) {
        for (Schedule task: allTask) {
            task.setSequence(this.scheduleRepository.findLatestSequenceByTechnicianId(task.getTechnician().getId()) + 1);
            this.scheduleRepository.saveAndFlush(task);
        }
    }

    private List<Technician> findTechnicianForAddRoute(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        List<Technician> technicianIdList = new ArrayList<>();
        if (technician1TargetHour != 0 && technician2TargetHour != 0 && technician3TargetHour != 0) {
            if (technician1TotalHour != technician1TargetHour) {
                technicianIdList.add(this.technicianService.getTechnicianById(1));
            }
            if (technician2TotalHour != technician2TargetHour) {
                technicianIdList.add(this.technicianService.getTechnicianById(2));
            }
            if (technician3TotalHour != technician3TargetHour) {
                technicianIdList.add(this.technicianService.getTechnicianById(3));
            }

            return technicianIdList;
        }

        if (technician1TargetHour != 0 && technician2TargetHour != 0) {
            if (technician1TotalHour != technician1TargetHour) {
                technicianIdList.add(this.technicianService.getTechnicianById(1));
            }
            if (technician2TotalHour != technician2TargetHour) {
                technicianIdList.add(this.technicianService.getTechnicianById(2));
            }

            return technicianIdList;
        }

        technicianIdList.add(this.technicianService.getTechnicianById(1));
        return technicianIdList;
    }

    private boolean isTotalHourEqual(int technician1TargetHour, int technician2TargetHour, int technician3TargetHour) {
        int technician1TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(1);
        int technician2TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(2);
        int technician3TotalHour = this.scheduleRepository.findTotalHourByTechnicianId(3);

        // ช่าง 3 คน
        if (technician1TargetHour != 0 && technician2TargetHour != 0 && technician3TargetHour != 0) {
            // ช่างคนที่ 1, 2 และ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                // return true ช่างคนที่ 1,2 และ 3 มีจำนวนชั่วโมงเท่ากัน
                // return false ช่างมีจำนวนชั่วโมงไม่เท่ากัน
                return technician1TotalHour == technician2TotalHour && technician2TotalHour == technician3TotalHour;
            }
            // ช่างคนที่ 1 จัดเส้นทางเสร็จหมดแล้ว
            // ช่างคนที่ 2 กับ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour == technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                // return true ช่างคนที่ 2 กับ 3 มีจำนวนชั่วโมงเท่ากัน
                // return false ช่างคนที่ 2 กับ 3 มีจำนวนชั่วโมงไม่เท่ากัน
                return technician2TotalHour == technician3TotalHour;
            }
            // ช่างคนที่ 2 จัดเส้นทางเสร็จหมดแล้ว
            // ช่างคนที่ 1 กับ 3 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour == technician2TargetHour && technician3TotalHour != technician3TargetHour) {
                // return true ช่างคนที่ 1 กับ 3 มีจำนวนชั่วโมงเท่ากัน
                // return false ช่างคนที่ 1 กับ 3 มีจำนวนชั่วโมงไม่เท่ากัน
                return technician1TotalHour == technician3TotalHour;
            }
            // ช่างคนที่ 3 จัดเส้นทางเสร็จหมดแล้ว
            // ช่างคนที่ 1 กับ 2 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour && technician3TotalHour == technician3TargetHour) {
                // return true ช่างคนที่ 1 กับ 2 มีจำนวนชั่วโมงเท่ากัน
                // return false ช่างคนที่ 1 กับ 2 มีจำนวนชั่วโมงไม่เท่ากัน
                return technician1TotalHour == technician2TotalHour;
            }
            // เหลือช่าง 1 คนยังจัดเส้นทางไม่เสร็จ
            return true;
        }

        // ช่าง 2 คน
        if (technician1TargetHour != 0 && technician2TargetHour != 0) {
            // ช่างคนที่ 1 และ 2 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour && technician2TotalHour != technician2TargetHour) {
                // return true ช่างคนที่ 1 และ 2 มีจำนวนชั่วโมงเท่ากัน
                // return false ช่างมีจำนวนชั่วโมงไม่เท่ากัน
                return technician1TotalHour == technician2TotalHour;
            }
            // ช่างคนที่ 1 หรือ 2 ยังจัดเส้นทางไม่เสร็จ
            if (technician1TotalHour != technician1TargetHour || technician2TotalHour != technician2TargetHour) {
                return true;
            }
            // ช่างคนที่ 1 และ 2 จัดเส้นทางเสร็จหมดแล้ว
            return true;
        }

        // ช่าง 1 คน
        return true;
    }

    private void checkBestRoute(List<List<TechnicianPlanDto>> technician1Plan, List<List<TechnicianPlanDto>> technician2Plan, List<TechnicianPlanDto> technician3Plan, boolean isRequire2, int numOfTechnician) {
        int technician1TargetHour = this.configService.getTechnician1TargetHourConfig();
        int technician2TargetHour = this.configService.getTechnician2TargetHourConfig();
        int technician3TargetHour = this.configService.getTechnician3TargetHourConfig();
        int maxRoute = Integer.MAX_VALUE;

        for (int i = 0; i < technician1Plan.size(); i++) {
            this.stgScheduleService.deleteSchedule();

            List<List<TechnicianPlanDto>> tempPlan = new ArrayList<>();
            tempPlan.add(technician1Plan.get(i));
            tempPlan.add(technician2Plan.get(i));

            if (technician3Plan != null) {
                tempPlan.add(technician3Plan);
            }

            saveTechnicianPlanTemp(tempPlan);

            if (isRequire2) {
                findRouteRequire2(technician1TargetHour, technician2TargetHour, technician3TargetHour);
            } else {
                findRouteRequire1(technician1TargetHour, technician2TargetHour, technician3TargetHour);
            }

            int noOfRoute = this.scheduleRepository.findNumberOfRoute();

            if (noOfRoute < maxRoute) {
                this.stgScheduleService.truncateStgSchedule();
                this.stgScheduleService.saveToTemp();
                maxRoute = noOfRoute;
            }

            this.stgScheduleService.deleteSchedule();
        }

        this.stgScheduleService.saveBestRequest();
    }
}
